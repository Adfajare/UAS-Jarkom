package server;

import java.io.*;
import java.net.*;
import java.util.*;

public class PlayerHandler implements Runnable {
    private Socket socket;
    private BufferedReader in;
    private BufferedWriter out;
    private int playerId;
    private boolean isMyTurn = false;
<<<<<<< HEAD
    private Timer timeoutTimer;
=======
    private boolean isConnected = true;
>>>>>>> development

    public PlayerHandler(Socket socket, int playerId) {
        this.socket = socket;
        this.playerId = playerId;
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        } catch (IOException e) {
            e.printStackTrace();
            isConnected = false;
        }
    }

    public int getPlayerId() {
        return playerId;
    }

    public boolean isConnected() {
        return isConnected && !socket.isClosed();
    }

    public void sendMessage(String message) throws IOException {
        if (!isConnected()) return;
        
        try {
            out.write(message);
            out.newLine();
            out.flush();
        } catch (IOException e) {
            isConnected = false;
            throw e;
        }
    }

    public void waitForGuessWithTimeout() throws IOException {
<<<<<<< HEAD
        sendMessage("⏱ Giliran Anda! Masukkan angka 1-10 (Timeout: " + Server.getGuessTimeout() + " detik):");
        sendMessage("Nyawa tersisa: " + Server.getPlayerLives(this));
        sendMessage("YOUR_TURN"); // Kirim sinyal ke client bahwa ini gilirannya
=======
        // Check if player is still alive
        if (Server.getPlayerLives(this) <= 0) {
            sendMessage("💀 You are eliminated and cannot play anymore.");
            return;
        }
        
        sendMessage("🎯 YOUR TURN! Enter a number (1-10):");
        sendMessage("💖 Lives: " + Server.getPlayerLives(this) + " | ⏱️ Time: " + Server.getGuessTimeout() + "s");
        
        // Show used numbers if any
        if (!Server.getUsedNumbers().isEmpty()) {
            sendMessage("🚫 Used: " + Server.getUsedNumbers().toString());
        }
>>>>>>> development
        
        isMyTurn = true;
        
<<<<<<< HEAD
        // Cancel timer sebelumnya jika ada
        if (timeoutTimer != null) {
            timeoutTimer.cancel();
=======
        Future<String> future = executor.submit(() -> {
            try {
                String input;
                while (isMyTurn && isConnected() && (input = in.readLine()) != null) {
                    return input.trim();
                }
                return null;
            } catch (IOException e) {
                isConnected = false;
                return null;
            }
        });
        
        try {
            String input = future.get(Server.getGuessTimeout(), TimeUnit.SECONDS);
            
            if (input != null && isMyTurn && isConnected()) {
                if (processGuess(input)) {
                    isMyTurn = false;
                }
            } else if (isMyTurn && isConnected()) {
                // Timeout occurred
                isMyTurn = false;
                Server.handlePlayerTimeout(this);
            }
            
        } catch (TimeoutException e) {
            // Timeout handling
            future.cancel(true);
            if (isMyTurn && isConnected()) {
                isMyTurn = false;
                Server.handlePlayerTimeout(this);
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            isConnected = false;
        } finally {
            executor.shutdown();
>>>>>>> development
        }
        
        // Set timer untuk timeout
        timeoutTimer = new Timer();
        timeoutTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (isMyTurn) {
                    isMyTurn = false;
                    try {
                        Server.handlePlayerTimeout(PlayerHandler.this);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }, Server.getGuessTimeout() * 1000);
    }

    private boolean processGuess(String input) throws IOException {
        // Print user input to console
        System.out.println("Player #" + playerId + " input: " + input);
        
        try {
            int guess = Integer.parseInt(input);
            
            if (guess < 1 || guess > 10) {
                sendMessage("❌ Number must be between 1-10! Try again:");
                return false; // Continue waiting for valid input
            }
            
            if (Server.getUsedNumbers().contains(guess)) {
                sendMessage("❌ Number " + guess + " already used! Choose another:");
                return false; // Continue waiting for valid input
            }
            
            // Valid guess
            Server.checkNumber(guess, this);
            return true; // End turn
            
        } catch (NumberFormatException e) {
            sendMessage("❌ Invalid input! Enter a number between 1-10:");
            return false; // Continue waiting for valid input
        }
    }

    @Override
    public void run() {
        try {
            sendMessage("🎮 Connected successfully! Player #" + playerId);
            sendMessage("⏳ Waiting for game to start...");
            
            String msg;
<<<<<<< HEAD
            while ((msg = in.readLine()) != null) {
                System.out.println("Player #" + playerId + " mengirim: " + msg); // Debug log
                
                // Handle input angka dari client
                try {
                    int guess = Integer.parseInt(msg.trim());
                    
                    if (isMyTurn) {
                        if (guess >= 1 && guess <= 10) {
                            // Cancel timeout timer
                            if (timeoutTimer != null) {
                                timeoutTimer.cancel();
                            }
                            
                            isMyTurn = false;
                            Server.checkNumber(guess, this);
                        } else {
                            sendMessage("❌ Angka harus antara 1-10!");
=======
            while (isConnected() && (msg = in.readLine()) != null) {
                msg = msg.trim();
                
                // Print all received messages to console
                System.out.println("Player #" + playerId + " received: " + msg);
                
                // Handle special server messages
                if (msg.equals("YOUR_TURN")) {
                    if (Server.getPlayerLives(this) > 0 && Server.isGameInProgress()) {
                        waitForGuessWithTimeout();
                    } else if (Server.getPlayerLives(this) <= 0) {
                        sendMessage("💀 You are eliminated and cannot play.");
                    }
                } else if (msg.equals("RESET_GAME")) {
                    isMyTurn = false;
                    sendMessage("⏳ Waiting for game to start...");
                } else if (msg.startsWith("ANGKA_DIPILIH:")) {
                    // Number chosen by another player - ignore
                    continue;
                } else {
                    // Handle regular input when it's not player's turn
                    if (!isMyTurn) {
                        try {
                            Integer.parseInt(msg);
                            System.out.println("Player #" + playerId + " tried to guess " + msg + " when not their turn");
                            if (Server.isGameInProgress()) {
                                if (Server.getPlayerLives(this) > 0) {
                                    sendMessage("⏳ Not your turn. Please wait...");
                                } else {
                                    sendMessage("💀 You are eliminated and cannot play.");
                                }
                            } else {
                                sendMessage("⏳ Game hasn't started yet. Please wait...");
                            }
                        } catch (NumberFormatException e) {
                            // Ignore non-numeric input
                            System.out.println("Player #" + playerId + " sent non-numeric input: " + msg);
>>>>>>> development
                        }
                    } else {
                        // Jika bukan giliran
                        sendMessage("⏳ Bukan giliran Anda. Menunggu pemain lain...");
                    }
                } catch (NumberFormatException e) {
                    // Input bukan angka, abaikan atau beri pesan error
                    if (isMyTurn) {
                        sendMessage("❌ Input tidak valid. Masukkan angka 1-10!");
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("👤 Player #" + playerId + " disconnected.");
        } finally {
<<<<<<< HEAD
            // Cleanup
            if (timeoutTimer != null) {
                timeoutTimer.cancel();
            }
=======
            isConnected = false;
>>>>>>> development
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}