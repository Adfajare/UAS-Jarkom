package server;

import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    private static final int PORT = 8010;
    private static List<PlayerHandler> players = new ArrayList<>();
    private static int bombNumber;
    private static boolean gameActive = true;

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
            new Thread(player).start();
        }
    }

    public static synchronized void checkNumber(int guessed, PlayerHandler player) throws IOException {
        if (!gameActive) return;

          if (guessed == bombNumber) {
            player.sendMessage("💥 BOOM! Kamu kalah.");
            broadcast("💣 Pemain " + player.getPlayerId() + " terkena bom!", player);
            gameActive = false;
            logResult("Player " + player.getPlayerId() + " guessed " + guessed + " and lost.");
            
            broadcast("🔁 Game akan direset dalam 3 detik...", null);
            new Thread(Server::resetGame).start(); // jalankan di thread baru
        } else {
            player.sendMessage("✅ Aman! Giliran pemain lain.");
            broadcast("Pemain " + player.getPlayerId() + " menebak angka " + guessed + " dan aman.", player);
            broadcast("ANGKA_DIPILIH:" + guessed, null);

        }
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
        Thread.sleep(3000); // tunggu 3 detik sebelum reset
        bombNumber = new Random().nextInt(9) + 1; // angka 1–9
        gameActive = true;
        System.out.println("🔁 Game di-reset. Angka bom baru: " + bombNumber);

        // Beritahu semua pemain
        for (PlayerHandler player : players) {
            player.sendMessage("RESET_GAME");
        }

    } catch (InterruptedException | IOException e) {
        e.printStackTrace();
    }
}
}
