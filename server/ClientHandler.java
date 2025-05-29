import java.io.*;
import java.net.*;

public class ClientHandler implements Runnable {
    private Socket socket;
    private GameServer server;
    private PrintWriter out;
    private BufferedReader in;
    private String playerName;
    
    public ClientHandler(Socket socket, GameServer server) {
        this.socket = socket;
        this.server = server;
    }

    public String getPlayerName() {
    return playerName;
}
    
    @Override
    public void run() {
        try {
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
            // Get player name
            String nameMessage = in.readLine();
            if (nameMessage != null && nameMessage.startsWith("NAME:")) {
                playerName = nameMessage.substring(5);
                server.addPlayer(playerName);
                sendMessage("WELCOME:" + playerName);
            }
            
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                handleMessage(inputLine);
            }
        } catch (IOException e) {
            System.err.println("Client handler error: " + e.getMessage());
        } finally {
            cleanup();
        }
    }
    
    private void handleMessage(String message) {
        String[] parts = message.split(":");
        String command = parts[0];
        
        switch (command) {
            case "GUESS":
                if (parts.length > 1) {
                    try {
                        int number = Integer.parseInt(parts[1]);
                        server.handleGuess(playerName, number);
                    } catch (NumberFormatException e) {
                        sendMessage("ERROR:Invalid number format");
                    }
                }
                break;
            case "STOP":
                server.handlePlayerStop(playerName);
                break;
            case "PING":
                sendMessage("PONG");
                break;
        }
    }
    
    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }
    
    private void cleanup() {
        try {
            if (socket != null) socket.close();
            if (in != null) in.close();
            if (out != null) out.close();
        } catch (IOException e) {
            System.err.println("Cleanup error: " + e.getMessage());
        }
        server.removeClient(this);
    }
}
