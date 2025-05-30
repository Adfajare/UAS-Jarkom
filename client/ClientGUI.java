import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class ClientGUI extends JFrame {
    private static String SERVER_HOST = "localhost";
    private static int SERVER_PORT = 12345;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private boolean connected = false;

    // GUI Components
    private JTextField nameField;
    private JTextField hostField;
    private JTextField portField;
    private JButton connectButton;
    private JButton disconnectButton;
    private JLabel statusLabel;

    private String playerName;

     public ClientGUI() {
        setupEventListeners();
    }

 
     private void setupEventListeners() {
        connectButton.addActionListener(e -> connectToServer());
        disconnectButton.addActionListener(e -> disconnect());

        nameField.addActionListener(e -> connectToServer());

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                disconnect();

            }
        });
    }
    private void connectToServer() {
        playerName = nameField.getText().trim();
        if (playerName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a player name.");
            return;
        }

        String host = hostField.getText().trim();
        if (host.isEmpty()) {
            host = "localhost";
        }

        int port;
        try {
            port = Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid port number.");
            return;
        }

        try {
            socket = new Socket(host, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Send player name
            out.println("NAME:" + playerName);

            connected = true;
            updateConnectionState();

            // Start listening for server messages
            new Thread(this::listenToServer).start();

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                    "Failed to connect to " + host + ":" + port + "\n" + e.getMessage() +
                            "\n\nTips:\n- Check if server is running\n- Try a different port\n- Verify host address");
        }
    }

    private void disconnect() {
        connected = false;
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing connection: " + e.getMessage());
        }
        updateConnectionState();
    }

    private void updateConnectionState() {
        connectButton.setEnabled(!connected);
        disconnectButton.setEnabled(connected);
        nameField.setEnabled(!connected);
        hostField.setEnabled(!connected);
        portField.setEnabled(!connected);

        if (connected) {
            statusLabel.setText("Connected as " + playerName);
        } else {
            statusLabel.setText("Not connected");
        }
    }

    private void listenToServer() {
        
    }
}
