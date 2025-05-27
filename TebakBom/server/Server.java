package server;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class Server {
    private static final int PORT = 8010;
    private static final int MAX_LIVES = 3;
    private static final int GUESS_TIMEOUT = 20; // 20 seconds
    
    private static List<PlayerHandler> players = new ArrayList<>();
    private static Map<PlayerHandler, Integer> playerLives = new HashMap<>();
    private static Set<Integer> usedNumbers = new HashSet<>();
    
    private static int bombNumber;
    private static boolean gameActive = true;
    private static boolean gameInProgress = false;
    private static int currentPlayerIndex = 0;

    static {
        // Create logs directory if not exists
        File logDir = new File("logs");
        if (!logDir.exists()) {
            logDir.mkdirs();
        }

        File logFile = new File("logs/game_log.txt");
        try {
            if (!logFile.exists()) {
                logFile.createNewFile();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

<<<<<<< HEAD
   public static void main(String[] args) throws IOException {
    ServerSocket serverSocket = new ServerSocket(PORT);
    System.out.println("Server started. Waiting for players...");

    bombNumber = new Random().nextInt(10) + 1;
    System.out.println("Bom berada di angka: " + bombNumber);

    while (true) {
        Socket socket = serverSocket.accept();
        PlayerHandler player = new PlayerHandler(socket, players.size() + 1);
        players.add(player);
        playerLives.put(player, MAX_LIVES); // Set 3 nyawa untuk player baru
        new Thread(player).start();
        
        // Beritahu player tentang posisi mereka dan info join
        System.out.println("👤 Player #" + player.getPlayerId() + " bergabung! Total pemain: " + players.size());
        player.sendMessage("👤 Anda adalah Player #" + player.getPlayerId());
        player.sendMessage("🎮 Selamat datang! Anda memiliki " + MAX_LIVES + " nyawa.");
        player.sendMessage("⏱️ Anda memiliki " + GUESS_TIMEOUT + " detik untuk menebak setiap giliran.");
        
        // Broadcast ke semua player lain bahwa ada player baru
        broadcast("👤 Player #" + player.getPlayerId() + " bergabung! Total pemain: " + players.size(), player);
        
        // Auto start game jika sudah ada 2 pemain
        if (players.size() >= 2 && !gameInProgress) {
            startGame();
=======
    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("🎮 Bomb Guessing Game Server started on port " + PORT);
        System.out.println("⏳ Waiting for players...");

        generateNewBomb();

        while (true) {
            Socket socket = serverSocket.accept();
            PlayerHandler player = new PlayerHandler(socket, players.size() + 1);
            players.add(player);
            playerLives.put(player, MAX_LIVES);
            new Thread(player).start();
            
            player.sendMessage("🎮 Welcome Player #" + player.getPlayerId() + "!");
            player.sendMessage("💖 You have " + MAX_LIVES + " lives");
            player.sendMessage("⏱️ You have " + GUESS_TIMEOUT + " seconds per turn");
            player.sendMessage("🎯 Guess numbers between 1-10 to avoid the bomb!");
            
            System.out.println("👤 Player #" + player.getPlayerId() + " joined. Total players: " + players.size());
            
            // Auto start game if we have at least 2 players
            if (players.size() >= 2 && !gameInProgress) {
                startGame();
            } else if (players.size() == 1) {
                broadcast("⏳ Waiting for more players to join...", null);
            }
>>>>>>> development
        }
    }
}

    private static void generateNewBomb() {
        bombNumber = new Random().nextInt(10) + 1;
        usedNumbers.clear();
        System.out.println("💣 New bomb generated at number: " + bombNumber);
    }

    public static synchronized void startGame() throws IOException {
        if (players.size() < 2) {
            broadcast("⏳ Need at least 2 players to start the game", null);
            return;
        }
        
        if (!gameInProgress) {
            gameInProgress = true;
            gameActive = true;
            currentPlayerIndex = 0;
            
            broadcast("🎮 GAME STARTED! Total players: " + players.size(), null);
            broadcast("🎯 Avoid the bomb! Numbers 1-10 available", null);
            logResult("Game started with " + players.size() + " players");
            
            // Add a small delay to ensure all messages are sent before starting turns
            new Thread(() -> {
                try {
                    Thread.sleep(1000); // 1 second delay
                    nextTurn();
                } catch (InterruptedException | IOException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    public static synchronized void nextTurn() throws IOException {
        if (!gameActive || !gameInProgress) return;
        
        // Count alive players
        long alivePlayers = players.stream()
                .mapToInt(Server::getPlayerLives)
                .filter(lives -> lives > 0)
                .count();
        
        // Check win condition
        if (alivePlayers <= 1) {
            endGame();
            return;
        }
        
        // Find next alive player
        PlayerHandler currentPlayer = findNextAlivePlayer();
        
        if (currentPlayer != null) {
            String statusMsg = String.format("🎯 Player #%d's turn (Lives: %d) | Alive players: %d", 
                    currentPlayer.getPlayerId(), getPlayerLives(currentPlayer), alivePlayers);
            broadcast(statusMsg, null);
            
            if (!usedNumbers.isEmpty()) {
                broadcast("🚫 Used numbers: " + usedNumbers.toString(), null);
            }
            
            // Give a small delay before sending YOUR_TURN to ensure other messages are received first
            new Thread(() -> {
                try {
                    Thread.sleep(500); // 0.5 second delay
                    currentPlayer.sendMessage("YOUR_TURN");
                } catch (InterruptedException | IOException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    private static PlayerHandler findNextAlivePlayer() {
        int attempts = 0;
        while (attempts < players.size()) {
            if (currentPlayerIndex >= players.size()) {
                currentPlayerIndex = 0;
            }
            
            PlayerHandler player = players.get(currentPlayerIndex);
            if (getPlayerLives(player) > 0) {
                return player;
            }
            
            currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
            attempts++;
        }
        return null;
    }

    private static void endGame() throws IOException {
        PlayerHandler winner = players.stream()
                .filter(p -> getPlayerLives(p) > 0)
                .findFirst()
                .orElse(null);
        
        if (winner != null) {
            broadcast("🏆 GAME OVER! Player #" + winner.getPlayerId() + " WINS!", null);
            winner.sendMessage("🎉 CONGRATULATIONS! You won the game!");
            logResult("Player #" + winner.getPlayerId() + " wins the game");
        } else {
            broadcast("🤝 GAME OVER! No survivors - It's a draw!", null);
            logResult("Game ended in a draw - no survivors");
        }
        
        gameActive = false;
        gameInProgress = false;
        
        broadcast("🔄 Game will reset in 5 seconds...", null);
        new Thread(Server::resetGame).start();
    }

    public static synchronized void checkNumber(int guessed, PlayerHandler player) throws IOException {
        if (!gameActive || !gameInProgress) return;

        usedNumbers.add(guessed);
        logResult("Player #" + player.getPlayerId() + " guessed " + guessed);

        if (guessed == bombNumber) {
            // Player hit the bomb
            int currentLives = playerLives.get(player) - 1;
            playerLives.put(player, currentLives);
            
            player.sendMessage("💥 BOOM! You hit the bomb!");
            broadcast("💣 Player #" + player.getPlayerId() + " hit the bomb! (Number: " + guessed + ")", player);
            
            if (currentLives > 0) {
                player.sendMessage("💖 Lives remaining: " + currentLives);
                broadcast("💖 Player #" + player.getPlayerId() + " has " + currentLives + " lives left", player);
                logResult("Player #" + player.getPlayerId() + " hit bomb, " + currentLives + " lives remaining");
            } else {
                player.sendMessage("💀 ELIMINATED! No lives remaining!");
                broadcast("💀 Player #" + player.getPlayerId() + " has been ELIMINATED!", player);
                logResult("Player #" + player.getPlayerId() + " eliminated");
            }
            
            // Generate new bomb and continue
            generateNewBomb();
            broadcast("🔄 New bomb generated! All numbers available again.", null);
            
        } else {
            // Safe guess
            player.sendMessage("✅ SAFE! Number " + guessed + " is not the bomb.");
            broadcast("✅ Player #" + player.getPlayerId() + " chose " + guessed + " - SAFE!", player);
            logResult("Player #" + player.getPlayerId() + " guessed " + guessed + " safely");
        }
        
        // Move to next player
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
        nextTurn();
    }

    public static synchronized void handlePlayerTimeout(PlayerHandler player) throws IOException {
        if (!gameActive || !gameInProgress) return;
        
        // Generate random number that hasn't been used
        Random random = new Random();
        int randomGuess;
        int attempts = 0;
        
        do {
            randomGuess = random.nextInt(10) + 1;
            attempts++;
        } while (usedNumbers.contains(randomGuess) && attempts < 10);
        
        // If all numbers used, pick any random number
        if (attempts >= 10) {
            randomGuess = random.nextInt(10) + 1;
        }
        
        player.sendMessage("⏰ TIME'S UP! System chose number " + randomGuess + " for you.");
        broadcast("⏰ Player #" + player.getPlayerId() + " timed out. System chose " + randomGuess, player);
        
        logResult("Player #" + player.getPlayerId() + " timed out, system chose " + randomGuess);
        checkNumber(randomGuess, player);
    }

    public static void broadcast(String message, PlayerHandler sender) throws IOException {
        for (PlayerHandler player : players) {
            if (player != sender && player.isConnected()) {
                player.sendMessage(message);
            }
        }
    }

    public static void logResult(String log) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter("logs/game_log.txt", true))) {
            bw.write(new Date() + " - " + log);
            bw.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void resetGame() {
        try {
            Thread.sleep(5000); // 5 second delay
            
            // Reset game state
            generateNewBomb();
            gameActive = true;
            gameInProgress = false;
            currentPlayerIndex = 0;
            
            // Reset all player lives
            for (PlayerHandler player : players) {
                if (player.isConnected()) {
                    playerLives.put(player, MAX_LIVES);
                }
            }
            
            // Remove disconnected players
            players.removeIf(player -> !player.isConnected());
            
            System.out.println("🔄 Game reset completed. Active players: " + players.size());
            
            // Notify remaining players
            for (PlayerHandler player : players) {
                if (player.isConnected()) {
                    player.sendMessage("RESET_GAME");
                    player.sendMessage("🔄 NEW GAME! You have " + MAX_LIVES + " lives again.");
                }
            }
            
            // Auto restart if enough players
            if (players.size() >= 2) {
                startGame();
            } else {
                broadcast("⏳ Waiting for more players to join...", null);
            }
            
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }

    // Getter methods
    public static int getGuessTimeout() {
        return GUESS_TIMEOUT;
    }
    
    public static int getPlayerLives(PlayerHandler player) {
        Integer lives = playerLives.get(player);
        return lives != null ? Math.max(0, lives) : 0;
    }
    
    public static Set<Integer> getUsedNumbers() {
        return new HashSet<>(usedNumbers);
    }
    
    public static boolean isGameInProgress() {
        return gameInProgress;
    }
    
    public static List<PlayerHandler> getPlayers() {
        return new ArrayList<>(players);
    }
}