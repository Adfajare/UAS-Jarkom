package server;

import java.io.*;
import java.net.*;

public class PlayerHandler implements Runnable {
    private Socket socket;
    private BufferedReader in;
    private BufferedWriter out;
    private int playerId;

    public PlayerHandler(Socket socket, int playerId) {
        this.socket = socket;
        this.playerId = playerId;
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            sendMessage("Selamat datang Pemain #" + playerId + "! Tebak angka antara 1-9:");
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

    @Override
    public void run() {
        String msg;
        try {
            while ((msg = in.readLine()) != null) {
                try {
                    int guessed = Integer.parseInt(msg.trim());
                    Server.checkNumber(guessed, this);
                } catch (NumberFormatException e) {
                    sendMessage("Input tidak valid. Masukkan angka 1–9.");
                }
            }
        } catch (IOException e) {
            System.out.println("Pemain #" + playerId + " keluar.");
        }
    }
}
