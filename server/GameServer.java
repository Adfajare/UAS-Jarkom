import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class GameServer {
    private static int PORT = 12345;
    private static final int GAME_DURATION = 120;
    private static final int TURN_TIMEOUT = 10; // 5 seconds per turn
    private static final String HOST = "0.0.0.0";
    private ServerSocket serverSocket;
    private List<ClientHandler> clients;
    private GameState gameState;
    private Timer gameTimer;
    private Timer turnTimer;
    private boolean gameRunning;
    private long gameStartTime;
    private Scanner adminScanner;
    private boolean adminInputActive;
    private String currentPlayer;
    private List<String> playerTurnOrder;
    private int currentPlayerIndex;

    public GameServer() {
        clients = new CopyOnWriteArrayList<>();
        gameState = new GameState();
        gameRunning = false;
        adminScanner = new Scanner(System.in);
        adminInputActive = false;
        playerTurnOrder = new ArrayList<>();
        currentPlayerIndex = 0;
    }

    public void start() throws IOException {
        // Try multiple ports if the default is in use
        for (int attempt = 0; attempt < 10; attempt++) {
            try {
                serverSocket = new ServerSocket(PORT, 50, InetAddress.getByName(HOST));
                System.out.println("Server started on port " + PORT);
                break;
            } catch (IOException e) {
                if (attempt == 9) {
                    throw new IOException("Could not bind to any port starting from " + PORT, e);
                }
                System.out.println("Port " + PORT + " is in use, trying " + (PORT + 1));
                PORT++;
            }
        }

        while (true) {
            Socket clientSocket = serverSocket.accept();
            ClientHandler clientHandler = new ClientHandler(clientSocket, this);
            clients.add(clientHandler);
            new Thread(clientHandler).start();
            System.out.println("Client connected. Total clients: " + clients.size());

            if (clients.size() >= 2 && !gameRunning && !adminInputActive) {
                promptAdminToStartGame();
            }
        }
    }

    private void promptAdminToStartGame() {
        adminInputActive = true;
        System.out.println("\n" + clients.size() + " players are connected.");
        System.out.print("Do you want to start the game? (y/n): ");

        // Handle admin input in a separate thread to not block server operations
        new Thread(() -> {
            try {
                String input = adminScanner.nextLine().trim().toLowerCase();
                if (input.equals("y") || input.equals("yes")) {
                    System.out.println("Admin starting the game...");
                    startNewGame();
                } else {
                    System.out.println("Game start cancelled. Waiting for more players or admin decision...");
                }
            } catch (Exception e) {
                System.out.println("Error reading admin input: " + e.getMessage());
            } finally {
                adminInputActive = false;
            }
        }).start();
    }

    public synchronized void startNewGame() {
        if (gameRunning)
            return;

        gameRunning = true;
        adminInputActive = false;
        gameState.reset();
        gameStartTime = System.currentTimeMillis();

        // Initialize turn order with active players
        setupTurnOrder();

        broadcastMessage("GAME_START:" + GAME_DURATION);
        broadcastMessage("TURN_ORDER:" + String.join(",", playerTurnOrder));

        // Start the first turn
        startNextTurn();

        gameTimer = new Timer();
        gameTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                endGame();
            }
        }, GAME_DURATION * 1000);
    }

    private void setupTurnOrder() {
        playerTurnOrder.clear();
        for (ClientHandler client : clients) {
            if (client.getPlayerName() != null) {
                playerTurnOrder.add(client.getPlayerName());
            }
        }
        // Randomize the order
        Collections.shuffle(playerTurnOrder);
        currentPlayerIndex = 0;
        System.out.println("Turn order: " + playerTurnOrder);
    }

    private void startNextTurn() {
        if (!gameRunning || playerTurnOrder.isEmpty())
            return;

        // Find next active (non-stopped) player
        int attempts = 0;
        while (attempts < playerTurnOrder.size()) {
            currentPlayer = playerTurnOrder.get(currentPlayerIndex);
            if (!gameState.isPlayerStopped(currentPlayer)) {
                break;
            }
            currentPlayerIndex = (currentPlayerIndex + 1) % playerTurnOrder.size();
            attempts++;
        }

        // If all players are stopped, end game
        if (attempts >= playerTurnOrder.size()) {
            endGame();
            return;
        }

        broadcastMessage("TURN_START:" + currentPlayer);

        // Start turn timeout
        if (turnTimer != null) {
            turnTimer.cancel();
        }
        turnTimer = new Timer();
        turnTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                handleTurnTimeout();
            }
        }, TURN_TIMEOUT * 1000);
    }

    private synchronized void handleTurnTimeout() {
        if (!gameRunning || currentPlayer == null)
            return;

        System.out.println("Turn timeout for player: " + currentPlayer);

        // Move to next turn without making a guess
         makeRandomGuessForPlayer(currentPlayer);
    }

    private void makeRandomGuessForPlayer(String playerName) {
        // Get all numbers that this player hasn't guessed yet
        Set<Integer> availableNumbers = new HashSet<>();
        for (int i = 1; i <= GameState.getMaxNumber(); i++) {
            if (!gameState.isAlreadyGuessed(playerName, i)) {
                availableNumbers.add(i);
            }
        }

        if (!availableNumbers.isEmpty()) {
            // Pick a random number from available ones
            Random random = new Random();
            List<Integer> availableList = new ArrayList<>(availableNumbers);
            int randomNumber = availableList.get(random.nextInt(availableList.size()));

            System.out.println("Auto-selecting number " + randomNumber + " for player " + playerName);

            // Process the guess as if the player made it
            handleGuess(playerName, randomNumber);
        } else {
            // If no numbers available, just move to next turn
            System.out.println("No available numbers for player " + playerName + ", moving to next turn");
            moveToNextTurn();
        }
    }

    private void moveToNextTurn() {
        if (!gameRunning || playerTurnOrder.isEmpty())
            return;

        currentPlayerIndex = (currentPlayerIndex + 1) % playerTurnOrder.size();
        startNextTurn();
    }

    public synchronized void endGame() {
        gameRunning = false;
        if (gameTimer != null) {
            gameTimer.cancel();
        }
        if (turnTimer != null) {
            turnTimer.cancel();
        }

        String winner = gameState.getWinner();
        broadcastMessage("GAME_END:" + winner);
        broadcastLeaderboard();

        // Reset turn state
        currentPlayer = null;
        playerTurnOrder.clear();
        currentPlayerIndex = 0;

        // Start new game after 3 seconds if we still have enough players
        Timer restartTimer = new Timer();
        restartTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (clients.size() >= 2 && !adminInputActive) {
                    promptAdminToStartGame();
                }
            }
        }, 3000);
    }

    private int getRemainingTime() {
        if (!gameRunning)
            return 0;
        long elapsed = (System.currentTimeMillis() - gameStartTime) / 1000;
        return Math.max(0, GAME_DURATION - (int) elapsed);
    }

    public synchronized void handleGuess(String playerName, int number) {
        if (!gameRunning)
            return;

        // Check if it's this player's turn
        if (!playerName.equals(currentPlayer)) {
            // Send error message to the player who tried to guess out of turn
            for (ClientHandler client : clients) {
                if (client.getPlayerName() != null && client.getPlayerName().equals(playerName)) {
                    client.sendMessage("ERROR:Not your turn! Current player: " + currentPlayer);
                    break;
                }
            }
            return;
        }

        // Cancel turn timer first to prevent timeout
        if (turnTimer != null) {
            turnTimer.cancel();
        }

        boolean isBomb = gameState.isBomb(number);
        boolean alreadyGuessed = gameState.isAlreadyGuessed(playerName, number);

        if (!alreadyGuessed) {
            gameState.addGuess(playerName, number);
            if (isBomb) {
                gameState.decreaseScore(playerName, 5);
            } else {
                gameState.increaseScore(playerName, 1);
            }
        }

        String response = "GUESS_RESULT:" + number + ":" + isBomb + ":" + alreadyGuessed + ":" + playerName;
        broadcastMessage(response);
        broadcastLeaderboard();

        // Move to next turn
        moveToNextTurn();
    }

    public synchronized void handlePlayerStop(String playerName) {
        gameState.setPlayerStopped(playerName);
        broadcastMessage("PLAYER_STOPPED:" + playerName);
        broadcastLeaderboard();

        // If it was this player's turn, move to next turn
        if (playerName.equals(currentPlayer)) {
            if (turnTimer != null) {
                turnTimer.cancel();
            }
            moveToNextTurn();
        }

        if (gameState.allPlayersStopped()) {
            endGame();
        }
    }

    public void broadcastMessage(String message) {
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }

    public void broadcastLeaderboard() {
        String leaderboard = gameState.getLeaderboardString();
        broadcastMessage("LEADERBOARD:" + leaderboard);
    }

    public void removeClient(ClientHandler client) {
        clients.remove(client);
        System.out.println("Client disconnected. Total clients: " + clients.size());
    }

    public boolean isGameRunning() {
        return gameRunning;
    }

    public void addPlayer(String playerName) {
        gameState.addPlayer(playerName);
        broadcastLeaderboard();

        if (gameRunning) {
            // Find this player's client and send them the game start message
            for (ClientHandler client : clients) {
                if (client.getPlayerName() != null && client.getPlayerName().equals(playerName)) {
                    // Calculate remaining time
                    int remainingTime = getRemainingTime();
                    client.sendMessage("GAME_START:" + remainingTime);
                    client.sendMessage("TURN_ORDER:" + String.join(",", playerTurnOrder));
                    if (currentPlayer != null) {
                        client.sendMessage("TURN_START:" + currentPlayer);
                    }
                    break;
                }
            }
        } else if (clients.size() >= 2 && !adminInputActive) {
            // Prompt admin when a new player joins and we have enough players
            promptAdminToStartGame();
        }
    }

    public static void main(String[] args) {
        // Parse command line arguments
        if (args.length > 0) {
            try {
                PORT = Integer.parseInt(args[0]);
                System.out.println("Using custom port: " + PORT);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number: " + args[0] + ". Using default port: " + PORT);
            }
        }

        try {
            new GameServer().start();
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
            System.err.println("Try running with a different port: java GameServer <port>");
        }
    }
}
