package server;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class PlayerHandler implements Runnable {
    private Socket socket;
    private BufferedReader in;
    private BufferedWriter out;
    private int playerId;
    private boolean isMyTurn = false;

    public PlayerHandler(Socket socket, int playerId) {
        this.socket = socket;
        this.playerId = playerId;
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            sendMessage("Selamat datang Pemain #" + playerId + "! Tebak angka antara 1-10:");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getPlayerId() {
        return playerId;
    }

    public void sendMessage(String message) throws IOException {
        out.write(message);
        out.newLine();
        out.flush();
    }

    public void setTurn(boolean myTurn) {
        this.isMyTurn = myTurn;
    }

    // Method untuk menunggu input dengan timeout
    public void waitForGuessWithTimeout() throws IOException {
        sendMessage("⏱ Giliran Anda! Masukkan angka 1-10 (Timeout: " + Server.getGuessTimeout() + " detik):");
        sendMessage("Nyawa tersisa: " + Server.getPlayerLives(this));
        
        isMyTurn = true;
        ExecutorService executor = Executors.newSingleThreadExecutor();
        
        Future<String> future = executor.submit(() -> {
            try {
                String input;
                while (isMyTurn && (input = in.readLine()) != null) {
                    return input;
                }
                return null;
            } catch (IOException e) {
                return null;
            }
        });
        
        try {
            String input = future.get(Server.getGuessTimeout(), TimeUnit.SECONDS);
            
            if (input != null && isMyTurn) {
                try {
                    int guess = Integer.parseInt(input.trim());
                    if (guess >= 1 && guess <= 10) {
                        isMyTurn = false;
                        Server.checkNumber(guess, this);
                    } else {
                        sendMessage("❌ Angka harus antara 1-10!");
                        waitForGuessWithTimeout(); // Minta input lagi
                    }
                } catch (NumberFormatException e) {
                    sendMessage("❌ Input tidak valid. Masukkan angka 1-10!");
                    waitForGuessWithTimeout(); // Minta input lagi
                }
            } else if (isMyTurn) {
                // Timeout terjadi
                isMyTurn = false;
                Server.handlePlayerTimeout(this);
            }
            
        } catch (TimeoutException e) {
            // Timeout terjadi
            future.cancel(true);
            if (isMyTurn) {
                isMyTurn = false;
                Server.handlePlayerTimeout(this);
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        } finally {
            executor.shutdown();
        }
    }

    @Override
    public void run() {
        try {
            String msg;
            while ((msg = in.readLine()) != null) {
                // Handle special messages dari server
                if (msg.equals("YOUR_TURN")) {
                    waitForGuessWithTimeout();
                } else if (msg.equals("RESET_GAME")) {
                    isMyTurn = false;
                    // Game direset, tunggu giliran berikutnya
                } else if (msg.startsWith("ANGKA_DIPILIH:")) {
                    // Angka sudah dipilih player lain
                    continue;
                } else {
                    // Jika bukan giliran, abaikan input
                    if (!isMyTurn) {
                        try {
                            int guessed = Integer.parseInt(msg.trim());
                            sendMessage("⏳ Bukan giliran Anda. Menunggu pemain lain...");
                        } catch (NumberFormatException e) {
                            // Abaikan input yang bukan angka
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Pemain #" + playerId + " keluar.");
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}