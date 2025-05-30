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
    private JLabel scoreLabel;
    private JLabel timerLabel;
    private JLabel turnLabel;
    private JLabel turnTimerLabel;
    private JTextArea leaderboardArea;
    private JButton[] numberButtons;
    private JButton stopButton;
    private JPanel gamePanel;
    private JPanel connectionPanel;

    private String playerName;
    private javax.swing.Timer gameTimer;
    private int timeRemaining;
    private javax.swing.Timer turnCountdownTimer;
    private int turnTimeRemaining;
    private String currentTurnPlayer;
    private boolean isMyTurn;
    private boolean playerStopped;

    public ClientGUI() {
        initializeGUI();
        setupEventListeners();
    }

    private void initializeGUI() {
        setTitle("AWAS BOM - Game Client");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Connection Panel
        connectionPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        nameField = new JTextField(15);
        nameField.setText("Player" + (int) (Math.random() * 1000));
        hostField = new JTextField(SERVER_HOST, 10);
        portField = new JTextField(String.valueOf(SERVER_PORT), 5);
        connectButton = new JButton("Connect");
        disconnectButton = new JButton("Disconnect");
        disconnectButton.setEnabled(false);

        gbc.gridx = 0;
        gbc.gridy = 0;
        connectionPanel.add(new JLabel("Name:"), gbc);
        gbc.gridx = 1;
        connectionPanel.add(nameField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        connectionPanel.add(new JLabel("Host:"), gbc);
        gbc.gridx = 1;
        connectionPanel.add(hostField, gbc);

        gbc.gridx = 2;
        gbc.gridy = 1;
        connectionPanel.add(new JLabel("Port:"), gbc);
        gbc.gridx = 3;
        connectionPanel.add(portField, gbc);

        gbc.gridx = 4;
        gbc.gridy = 0;
        connectionPanel.add(connectButton, gbc);
        gbc.gridy = 1;
        connectionPanel.add(disconnectButton, gbc);

        // Status Panel
        JPanel statusPanel = new JPanel(new GridLayout(5, 1));
        statusLabel = new JLabel("Not connected", SwingConstants.CENTER);
        scoreLabel = new JLabel("Score: 0", SwingConstants.CENTER);
        timerLabel = new JLabel("Time: --", SwingConstants.CENTER);
        turnLabel = new JLabel("Turn: --", SwingConstants.CENTER);
        turnTimerLabel = new JLabel("Turn time: --", SwingConstants.CENTER);

        statusPanel.add(statusLabel);
        statusPanel.add(scoreLabel);
        statusPanel.add(timerLabel);
        statusPanel.add(turnLabel);
        statusPanel.add(turnTimerLabel);

        // Game Panel
        gamePanel = new JPanel(new GridLayout(4, 5, 5, 5));
        numberButtons = new JButton[70];

        for (int i = 0; i < 70; i++) {
            final int number = i + 1;
            numberButtons[i] = new JButton(String.valueOf(number));
            numberButtons[i].setEnabled(false);
            numberButtons[i].addActionListener(e -> makeGuess(number));
            gamePanel.add(numberButtons[i]);
        }

        // Control Panel
        JPanel controlPanel = new JPanel();
        stopButton = new JButton("STOP GAME");
        stopButton.setEnabled(false);
        stopButton.setBackground(Color.RED);
        stopButton.setForeground(Color.WHITE);
        stopButton.addActionListener(e -> stopGame());
        controlPanel.add(stopButton);

        // Leaderboard Panel
        JPanel leaderboardPanel = new JPanel(new BorderLayout());
        leaderboardPanel.setBorder(BorderFactory.createTitledBorder("Leaderboard"));
        leaderboardArea = new JTextArea(8, 20);
        leaderboardArea.setEditable(false);
        leaderboardArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(leaderboardArea);
        leaderboardPanel.add(scrollPane, BorderLayout.CENTER);

        // Layout
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(connectionPanel, BorderLayout.NORTH);
        topPanel.add(statusPanel, BorderLayout.CENTER);

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(gamePanel, BorderLayout.CENTER);
        centerPanel.add(controlPanel, BorderLayout.SOUTH);

        add(topPanel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
        add(leaderboardPanel, BorderLayout.EAST);

        pack();
        setLocationRelativeTo(null);
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
        resetGameState();
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
        try {
            String message;
            while (connected && (message = in.readLine()) != null) {
                final String finalMessage = message;
                SwingUtilities.invokeLater(() -> handleServerMessage(finalMessage));
            }
        } catch (IOException e) {
            if (connected) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, "Connection lost: " + e.getMessage());
                    disconnect();
                });
            }
        }
    }

    private void handleServerMessage(String message) {
        String[] parts = message.split(":");
        String command = parts[0];

        switch (command) {
            case "WELCOME":
                statusLabel.setText("Welcome " + parts[1] + "! Waiting for game to start...");
                break;

            case "GAME_START":
                int duration = Integer.parseInt(parts[1]);
                startGame(duration);
                break;

            case "GAME_END":
                String winner = parts[1];
                endGame(winner);
                break;

            case "TURN_ORDER":
                String[] players = parts[1].split(",");
                statusLabel.setText("Turn order: " + String.join(" → ", players));
                break;

            case "TURN_START":
                String turnPlayer = parts[1];
                handleTurnStart(turnPlayer);
                break;

            case "GUESS_RESULT":
                int number = Integer.parseInt(parts[1]);
                boolean isBomb = Boolean.parseBoolean(parts[2]);
                boolean alreadyGuessed = Boolean.parseBoolean(parts[3]);
                String guessingPlayer = parts.length > 4 ? parts[4] : "Unknown";
                handleGuessResult(number, isBomb, alreadyGuessed, guessingPlayer);
                break;

            case "ERROR":
                JOptionPane.showMessageDialog(this, parts[1], "Error", JOptionPane.WARNING_MESSAGE);
                break;

            case "LEADERBOARD":
                updateLeaderboard(parts[1]);
                break;

            case "PLAYER_STOPPED":
                String stoppedPlayer = parts[1];
                statusLabel.setText(stoppedPlayer + " stopped the game!");
                break;
        }
    }

    private void handleTurnStart(String turnPlayer) {
        currentTurnPlayer = turnPlayer;
        isMyTurn = turnPlayer.equals(playerName);

        
        if (isMyTurn && !playerStopped) {
            turnLabel.setText("YOUR TURN!");
            turnLabel.setForeground(Color.GREEN);
            enableNumberButtons(true);
        } else {
            turnLabel.setText("Turn: " + turnPlayer);
            turnLabel.setForeground(Color.BLACK);
            enableNumberButtons(false);
        }
        
        turnTimeRemaining = 10;
        turnTimerLabel.setText("Turn time: " + turnTimeRemaining);
        
        if (turnCountdownTimer != null) {
            turnCountdownTimer.stop();
        }
        turnCountdownTimer = new javax.swing.Timer(1000, e -> {
            turnTimeRemaining--;
            turnTimerLabel.setText("Turn time: " + turnTimeRemaining);
            if (turnTimeRemaining <= 0) {
                turnCountdownTimer.stop();
                turnTimerLabel.setText("Turn time: --");
            }
        });
        turnCountdownTimer.start();
    }

    private void enableNumberButtons(boolean enabled) {
        for (JButton button : numberButtons) {
            if (enabled && !playerStopped) {
                button.setEnabled(true);
            } else {
                button.setEnabled(false);
            }
        }
    }

    private void startGame(int duration) {
        statusLabel.setText("Game started! Duration: " + duration + " seconds");
        timeRemaining = duration;
        playerStopped = false;

        // Reset and disable all number buttons initially
        for (JButton button : numberButtons) {
            button.setEnabled(false);
            button.setBackground(null);
        }
        stopButton.setEnabled(true);

        // Start countdown timer
        if (gameTimer != null) {
            gameTimer.stop();
        }
        gameTimer = new javax.swing.Timer(1000, e -> {
            timeRemaining--;
            timerLabel.setText("Time: " + timeRemaining);
            if (timeRemaining <= 0) {
                gameTimer.stop();
            }
        });
        gameTimer.start();

        timerLabel.setText("Time: " + timeRemaining);
        turnLabel.setText("Turn: Waiting...");
        turnTimerLabel.setText("Turn time: --");
    }

    private void endGame(String winner) {
        statusLabel.setText("Game ended! Winner: " + winner);
        playerStopped = false;

        // Disable game controls
        for (JButton button : numberButtons) {
            button.setEnabled(false);
        }
        stopButton.setEnabled(false);

        if (gameTimer != null) {
            gameTimer.stop();
        }
        if (turnCountdownTimer != null) {
            turnCountdownTimer.stop();
        }
        timerLabel.setText("Time: --");
        turnLabel.setText("Turn: --");
        turnTimerLabel.setText("Turn time: --");
    }

    private void makeGuess(int number) {
        if (connected && out != null && isMyTurn && !playerStopped) {
            out.println("GUESS:" + number);
            // Immediately disable buttons to prevent multiple clicks
            enableNumberButtons(false);
            turnLabel.setText("Waiting for result...");
        }
    }

    private void stopGame() {
        if (connected && out != null) {
            out.println("STOP");
            stopButton.setEnabled(false);
            playerStopped = true;
            enableNumberButtons(false);
            turnLabel.setText("You stopped playing");
            turnLabel.setForeground(Color.RED);
        }
    }

    private void handleGuessResult(int number, boolean isBomb, boolean alreadyGuessed, String guessingPlayer) {
        JButton button = numberButtons[number - 1];

        if (alreadyGuessed) {
            button.setBackground(Color.GRAY);
            button.setText(number + " (×)");
        } else if (isBomb) {
            button.setBackground(Color.RED);
            button.setText(number + " (💣)");
        } else {
            button.setBackground(Color.GREEN);
            button.setText(number + " (✓)");
        }
        button.setEnabled(false);
        
        // Show who made the guess
        if (!guessingPlayer.equals("Unknown")) {
            String message = guessingPlayer + " picked " + number;
            if (isBomb) message += " - BOMB!";
            else if (alreadyGuessed) message += " - Already picked!";
            statusLabel.setText(message);
        }
        
        // If it was my guess, disable all buttons until next turn
        if (guessingPlayer.equals(playerName)) {
            enableNumberButtons(false);
        }
    }

    private void updateLeaderboard(String leaderboardData) {
        StringBuilder sb = new StringBuilder();
        sb.append("Player           Score  Status\n");
        sb.append("--------------------------------\n");

        String[] players = leaderboardData.split(";");
        for (String playerData : players) {
            if (!playerData.trim().isEmpty()) {
                String[] parts = playerData.split(",");
                if (parts.length == 3) {
                    String name = parts[0];
                    String score = parts[1];
                    boolean stopped = Boolean.parseBoolean(parts[2]);

                     sb.append(String.format("%-15s %5s\n",
                        name + ": " + score, stopped ? "STOPPED" : "PLAYING"));

                    if (name.equals(playerName)) {
                        scoreLabel.setText("Score: " + score);
                    }
                }
            }
        }

        leaderboardArea.setText(sb.toString());
    }

    private void resetGameState() {
        for (int i = 0; i < numberButtons.length; i++) {
            JButton button = numberButtons[i];
            button.setEnabled(false);
            button.setBackground(null);
            button.setText(String.valueOf(i + 1));
        }
        stopButton.setEnabled(false);
        scoreLabel.setText("Score: 0");
        timerLabel.setText("Time: --");
        turnLabel.setText("Turn: --");
        turnTimerLabel.setText("Turn time: --");
        leaderboardArea.setText("");
        playerStopped = false;
        isMyTurn = false;
        currentTurnPlayer = null;

        if (gameTimer != null) {
            gameTimer.stop();
        }
        if (turnCountdownTimer != null) {
            turnCountdownTimer.stop();
        }
    }
}
