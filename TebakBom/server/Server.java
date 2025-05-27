package server;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class Server {
    private static final int PORT = 8010;
    private static List<PlayerHandler> players = new ArrayList<>();
    private static int bombNumber;
    private static boolean gameActive = true;
    private static Map<PlayerHandler, Integer> playerLives = new HashMap<>(); // Nyawa setiap player
    private static final int MAX_LIVES = 3;
    private static final int GUESS_TIMEOUT = 20; // 20 detik timeout
    
    // Tambahan untuk turn management
    private static int currentPlayerIndex = 0;
    private static boolean gameInProgress = false;
    private static Set<Integer> usedNumbers = new HashSet<>();

    static {
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
            
            // Beritahu player tentang nyawa mereka
            player.sendMessage("🎮 Selamat datang! Anda memiliki " + MAX_LIVES + " nyawa.");
            player.sendMessage("⏱️ Anda memiliki " + GUESS_TIMEOUT + " detik untuk menebak setiap giliran.");
            
            // Auto start game jika sudah ada 2 pemain
            if (players.size() >= 2 && !gameInProgress) {
                startGame();
            }
        }
    }

    // Method untuk memulai game
    public static synchronized void startGame() throws IOException {
        if (players.size() < 2) {
            broadcast("⏳ Menunggu pemain lain bergabung...", null);
            return;
        }
        
        if (!gameInProgress) {
            gameInProgress = true;
            currentPlayerIndex = 0;
            usedNumbers.clear();
            broadcast("🎮 Game dimulai! Total pemain: " + players.size(), null);
            nextTurn();
        }
    }

    // Method untuk giliran berikutnya
    public static synchronized void nextTurn() throws IOException {
        if (!gameActive || !gameInProgress) return;
        
        // Cari player yang masih hidup
        PlayerHandler currentPlayer = null;
        int attempts = 0;
        
        while (attempts < players.size()) {
            if (currentPlayerIndex >= players.size()) {
                currentPlayerIndex = 0;
            }
            
            currentPlayer = players.get(currentPlayerIndex);
            if (getPlayerLives(currentPlayer) > 0) {
                break;
            }
            currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
            attempts++;
        }
        
        if (currentPlayer != null && getPlayerLives(currentPlayer) > 0) {
            broadcast("🎯 Giliran Pemain #" + currentPlayer.getPlayerId(), null);
            currentPlayer.sendMessage("YOUR_TURN");
            // PlayerHandler akan menangani timeout sendiri
        }
    }

    public static synchronized void checkNumber(int guessed, PlayerHandler player) throws IOException {
        if (!gameActive) return;

        // Tambahkan ke daftar angka yang sudah digunakan
        usedNumbers.add(guessed);

        if (guessed == bombNumber) {
            // Player terkena bom, kurangi nyawa
            int currentLives = playerLives.get(player) - 1;
            playerLives.put(player, currentLives);
            
            if (currentLives > 0) {
                player.sendMessage("💥 BOOM! Anda terkena bom! Nyawa tersisa: " + currentLives);
                broadcast("💣 Pemain " + player.getPlayerId() + " terkena bom! Nyawa tersisa: " + currentLives, player);
                logResult("Player " + player.getPlayerId() + " hit bomb (guess: " + guessed + "), lives left: " + currentLives);
                
                // Ganti angka bom baru dan reset used numbers
                bombNumber = new Random().nextInt(10) + 1;
                usedNumbers.clear();
                System.out.println("Angka bom baru: " + bombNumber);
                broadcast("🔄 Angka bom baru telah digenerate!", null);
                
                // Lanjut ke pemain berikutnya
                currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
                nextTurn();
            } else {
                player.sendMessage("💥 BOOM! Anda terkena bom! Game Over - Nyawa habis!");
                broadcast("💀 Pemain " + player.getPlayerId() + " terkena bom dan nyawa habis! Game Over!", player);
                gameActive = false;
                gameInProgress = false;
                logResult("Player " + player.getPlayerId() + " hit bomb (guess: " + guessed + ") and eliminated. Game Over.");
                
                broadcast("🔁 Game akan direset dalam 3 detik...", null);
                new Thread(Server::resetGame).start();
            }
        } else {
            player.sendMessage("✅ Aman! Angka " + guessed + " bukan bom.");
            broadcast("Pemain " + player.getPlayerId() + " menebak angka " + guessed + " dan aman.", player);
            broadcast("ANGKA_DIPILIH:" + guessed, null);
            logResult("Player " + player.getPlayerId() + " guessed " + guessed + " safely.");
            
            // Lanjut ke pemain berikutnya
            currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
            nextTurn();
        }
    }

    // Method untuk menangani timeout player
    public static synchronized void handlePlayerTimeout(PlayerHandler player) throws IOException {
        if (!gameActive) return;
        
        Random random = new Random();
        int randomGuess;
        int attempts = 0;
        
        // Pastikan angka random belum dipilih dan tidak lebih dari 10 attempts
        do {
            randomGuess = random.nextInt(10) + 1;
            attempts++;
        } while (usedNumbers.contains(randomGuess) && attempts < 10);
        
        // Jika semua angka sudah digunakan, pilih angka random tetap
        if (attempts >= 10) {
            randomGuess = random.nextInt(10) + 1;
        }
        
        player.sendMessage("⏰ Waktu habis! Sistem memilih angka " + randomGuess + " untuk Anda.");
        broadcast("⏰ Pemain " + player.getPlayerId() + " kehabisan waktu. Sistem memilih angka " + randomGuess, player);
        
        logResult("Player " + player.getPlayerId() + " timed out, system chose " + randomGuess);
        checkNumber(randomGuess, player);
    }

    // Method untuk mendapatkan angka yang sudah digunakan
    private static Set<Integer> getUsedNumbers() {
        return usedNumbers;
    }

    public static void broadcast(String message, PlayerHandler sender) throws IOException {
        for (PlayerHandler player : players) {
            if (player != sender) {
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

    public static void resetGame() {
        try {
            Thread.sleep(3000);
            bombNumber = new Random().nextInt(10) + 1;
            gameActive = true;
            gameInProgress = false;
            currentPlayerIndex = 0;
            usedNumbers.clear();
            
            // Reset nyawa semua player
            for (PlayerHandler player : players) {
                playerLives.put(player, MAX_LIVES);
            }
            
            System.out.println("🔁 Game di-reset. Angka bom baru: " + bombNumber);

            for (PlayerHandler player : players) {
                player.sendMessage("RESET_GAME");
                player.sendMessage("🎮 Game baru dimulai! Anda memiliki " + MAX_LIVES + " nyawa.");
            }
            
            // Restart game otomatis jika masih ada pemain
            if (players.size() >= 2) {
                startGame();
            }

        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }

    // Getter methods untuk PlayerHandler
    public static int getGuessTimeout() {
        return GUESS_TIMEOUT;
    }
    
    public static int getPlayerLives(PlayerHandler player) {
        return playerLives.getOrDefault(player, MAX_LIVES);
    }
    
    public static int getCurrentPlayerIndex() {
        return currentPlayerIndex;
    }
    
    public static boolean isGameInProgress() {
        return gameInProgress;
    }
    
    public static List<PlayerHandler> getPlayers() {
        return players;
    }
}