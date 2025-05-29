import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class GameServer {
     private static int PORT = 12345;
    private List<ClientHandler> clients;
    private static final String HOST = "0.0.0.0";
    private ServerSocket serverSocket;

     public GameServer() {
        clients = new CopyOnWriteArrayList<>();

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
        }
    }

    public void removeClient(ClientHandler client) {
        clients.remove(client);
        System.out.println("Client disconnected. Total clients: " + clients.size());
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


