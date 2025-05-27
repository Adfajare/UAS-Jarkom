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
    private Timer timeoutTimer;

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
        sendMessage("YOUR_TURN"); // Kirim sinyal ke client bahwa ini gilirannya
        
        isMyTurn = true;
        
        // Cancel timer sebelumnya jika ada
        if (timeoutTimer != null) {
            timeoutTimer.cancel();
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

    @Override
    public void run() {
        try {
            String msg;
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
            System.out.println("Pemain #" + playerId + " keluar.");
        } finally {
            // Cleanup
            if (timeoutTimer != null) {
                timeoutTimer.cancel();
            }
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}