package client;

import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ClientGUI extends JFrame {
    private JTextArea area;
    private Client client;
    private JPanel buttonPanel;
    private JButton[] numberButtons = new JButton[10]; // Changed to 10 for numbers 1-10
    private boolean inputAllowed = false;
    private boolean isMyTurn = false;

    public ClientGUI(String host, int port) {
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (Exception e) {
            e.printStackTrace();
        }

        setTitle("💣 Tebak Bom - Client");
        setSize(500, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        getContentPane().setBackground(new Color(245, 248, 255));

        // Header
        JLabel header = new JLabel("💣 Tebak Angka, Jangan Kena BOM!", SwingConstants.CENTER);
        header.setFont(new Font("Poppins", Font.BOLD, 20));
        header.setBorder(BorderFactory.createEmptyBorder(15, 10, 10, 10));
        add(header, BorderLayout.NORTH);

        // Log area
        area = new JTextArea();
        area.setEditable(false);
        area.setFont(new Font("Consolas", Font.PLAIN, 14));
        area.setBackground(Color.WHITE);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        JScrollPane scroll = new JScrollPane(area);
        scroll.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        add(scroll, BorderLayout.CENTER);

        // Panel tombol - Changed to 2x5 grid for numbers 1-10
        buttonPanel = new JPanel(new GridLayout(2, 5, 12, 12));
        buttonPanel.setBackground(new Color(245, 248, 255));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 40, 20, 40));
        add(buttonPanel, BorderLayout.SOUTH);

        generateNumberButtons();

        setVisible(true);

        try {
            client = new Client(host, port);
            new Thread(this::readMessages).start();
        } catch (IOException e) {
            showError("❌ Gagal terhubung ke server: " + host + ":" + port);
        }
    }

    private void generateNumberButtons() {
        buttonPanel.removeAll();
        
        // Create buttons for numbers 1-10
        for (int i = 1; i <= 10; i++) {
            JButton button = new JButton(String.valueOf(i));
            button.setFont(new Font("Poppins", Font.BOLD, 20));
            button.setBackground(new Color(0x3f78e0));
            button.setForeground(Color.WHITE);
            button.setFocusPainted(false);
            button.setCursor(new Cursor(Cursor.HAND_CURSOR));
            button.setBorder(BorderFactory.createLineBorder(new Color(0x3f78e0), 2));
            button.setOpaque(true);
            button.setEnabled(false); // Start disabled

            final int number = i;

            // Hover effect
            button.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseEntered(java.awt.event.MouseEvent evt) {
                    if (button.isEnabled() && isMyTurn) {
                        button.setBackground(new Color(0x2f5cc0));
                    }
                }

                public void mouseExited(java.awt.event.MouseEvent evt) {
                    if (button.isEnabled() && isMyTurn) {
                        button.setBackground(new Color(0x3f78e0));
                    }
                }
            });

            button.addActionListener(e -> {
                if (!isMyTurn || !inputAllowed) {
                    area.append("⏳ Not your turn or input not allowed!\n");
                    return;
                }
                try {
                    client.send(String.valueOf(number));
                    area.append("📤 You chose: " + number + "\n");
                    disableAllButtons();
                    isMyTurn = false;
                    inputAllowed = false;
                } catch (IOException ex) {
                    showError("❌ Gagal mengirim angka: " + ex.getMessage());
                }
            });

            numberButtons[i-1] = button;
            buttonPanel.add(button);
        }

        buttonPanel.revalidate();
        buttonPanel.repaint();
    }

    private void disableButtonByNumber(int number) {
        if (number >= 1 && number <= 10) {
            JButton btn = numberButtons[number - 1];
            if (btn != null) {
                btn.setEnabled(false);
                btn.setBackground(new Color(200, 200, 200));
                btn.setForeground(new Color(100, 100, 100));
            }
        }
    }

    private void readMessages() {
        try {
            String line;
            while ((line = client.getReader().readLine()) != null) {
                final String message = line;
                SwingUtilities.invokeLater(() -> {
                    area.append(message + "\n");
                    
                    // Auto-scroll to bottom
                    area.setCaretPosition(area.getDocument().getLength());
                    
                    // Handle special server messages
                    if (message.equals("YOUR_TURN")) {
                        isMyTurn = true;
                        inputAllowed = true;
                        enableAvailableButtons();
                        area.append("🎯 IT'S YOUR TURN! Choose a number!\n");
                    } 
                    else if (message.equals("RESET_GAME")) {
                        area.append("🔁 Game reset - new round starting!\n");
                        resetAllButtons();
                        isMyTurn = false;
                        inputAllowed = false;
                    }
                    else if (message.startsWith("ANGKA_DIPILIH:")) {
                        try {
                            int number = Integer.parseInt(message.split(":")[1]);
                            disableButtonByNumber(number);
                        } catch (NumberFormatException e) {
                            // Ignore invalid format
                        }
                    }
                    else if (message.contains("🚫 Used:")) {
                        // Parse used numbers and disable corresponding buttons
                        String usedStr = message.substring(message.indexOf("[") + 1, message.indexOf("]"));
                        if (!usedStr.trim().isEmpty()) {
                            String[] usedNumbers = usedStr.split(",");
                            for (String numStr : usedNumbers) {
                                try {
                                    int num = Integer.parseInt(numStr.trim());
                                    disableButtonByNumber(num);
                                } catch (NumberFormatException e) {
                                    // Ignore invalid numbers
                                }
                            }
                        }
                    }
                    else if (message.contains("🔄 New bomb generated")) {
                        // Reset button availability when new bomb is generated
                        resetAllButtons();
                    }
                    else if (message.contains("BOOM") || message.contains("ELIMINATED") || 
                             message.contains("eliminated") || message.contains("💀")) {
                        disableAllButtons();
                        isMyTurn = false;
                        inputAllowed = false;
                    }
                    else if (message.contains("SAFE") || message.contains("✅")) {
                        isMyTurn = false;
                        inputAllowed = false;
                        disableAllButtons();
                    }
                });
            }
        } catch (IOException e) {
            SwingUtilities.invokeLater(() -> showError("⚠️ Connection lost: " + e.getMessage()));
        }
    }

    private void enableAvailableButtons() {
        for (JButton btn : numberButtons) {
            if (btn != null && btn.isEnabled()) {
                btn.setBackground(new Color(0x3f78e0));
                btn.setForeground(Color.WHITE);
            }
        }
    }

    private void resetAllButtons() {
        for (JButton btn : numberButtons) {
            if (btn != null) {
                btn.setEnabled(true);
                btn.setBackground(new Color(0x3f78e0));
                btn.setForeground(Color.WHITE);
            }
        }
    }

    private void disableAllButtons() {
        for (JButton btn : numberButtons) {
            if (btn != null) {
                btn.setEnabled(false);
                btn.setBackground(new Color(180, 180, 180));
                btn.setForeground(new Color(100, 100, 100));
            }
        }
    }

    private void showError(String message) {
        area.append("ERROR: " + message + "\n");
        area.setCaretPosition(area.getDocument().getLength());
        disableAllButtons();
        isMyTurn = false;
        inputAllowed = false;
    }

    public static void main(String[] args) {
        // You can change the IP address here to match your server
        String serverIP = "192.168.3.161"; // Change this to your server's IP
        int serverPort = 8010;
        
        SwingUtilities.invokeLater(() -> {
            new ClientGUI(serverIP, serverPort);
        });
    }
}
