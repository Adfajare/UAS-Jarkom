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
    private JButton[] numberButtons = new JButton[9];
    private boolean inputAllowed = false;

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
        JScrollPane scroll = new JScrollPane(area);
        scroll.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(scroll, BorderLayout.CENTER);

        // Panel tombol
        buttonPanel = new JPanel(new GridLayout(3, 3, 12, 12));
        buttonPanel.setBackground(new Color(245, 248, 255));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 40, 20, 40));
        add(buttonPanel, BorderLayout.SOUTH);

        generateNumberButtons();

        setVisible(true);

        try {
            client = new Client(host, port);
            new Thread(this::readMessages).start();
        } catch (IOException e) {
            showError("❌ Gagal terhubung ke server.");
        }
    }

    private void generateNumberButtons() {
        regenerateButtons();
    }

    private void regenerateButtons() {
        buttonPanel.removeAll();
        List<Integer> numbers = new ArrayList<>();
        for (int i = 1; i <= 9; i++) numbers.add(i);
        Collections.shuffle(numbers);

        for (int i = 0; i < 9; i++) {
            int number = numbers.get(i);
            JButton button = new JButton(String.valueOf(number));
            button.setFont(new Font("Poppins", Font.BOLD, 22));
            button.setBackground(new Color(0x3f78e0));
            button.setForeground(Color.WHITE);
            button.setFocusPainted(false);
            button.setCursor(new Cursor(Cursor.HAND_CURSOR));
            button.setBorder(BorderFactory.createLineBorder(new Color(0x3f78e0), 2));
            button.setOpaque(true);

            // Hover effect
            button.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseEntered(java.awt.event.MouseEvent evt) {
                    if (button.isEnabled()) {
                        button.setBackground(new Color(0x2f5cc0));
                    }
                }

                public void mouseExited(java.awt.event.MouseEvent evt) {
                    if (button.isEnabled()) {
                        button.setBackground(new Color(0x3f78e0));
                    }
                }
            });

            button.addActionListener(e -> {
                if (!inputAllowed) return;
                try {
                    client.send(String.valueOf(number));
                    button.setEnabled(false);
                    inputAllowed = false;
                } catch (IOException ex) {
                    showError("❌ Gagal mengirim angka.");
                }
            });

            numberButtons[i] = button;
            buttonPanel.add(button);
        }

        buttonPanel.revalidate();
        buttonPanel.repaint();
        inputAllowed = true;
    }

    private void disableButtonByNumber(int number) {
        for (JButton btn : numberButtons) {
            if (btn != null && btn.getText().equals(String.valueOf(number))) {
                btn.setEnabled(false);
                btn.setBackground(new Color(200, 200, 200));
            }
        }
    }

    private void readMessages() {
        try {
            String line;
            while ((line = client.getReader().readLine()) != null) {
                area.append(line + "\n");

                if (line.contains("Selamat datang")) {
                    SwingUtilities.invokeLater(this::enableAllButtons);
                }

                if (line.equals("RESET_GAME")) {
                    SwingUtilities.invokeLater(() -> {
                        area.append("🔁 Game telah direset!\n");
                        regenerateButtons();
                    });
                }

                if (line.startsWith("ANGKA_DIPILIH:")) {
                    int angka = Integer.parseInt(line.split(":")[1]);
                    SwingUtilities.invokeLater(() -> disableButtonByNumber(angka));
                }

                if (line.contains("Aman") || line.contains("giliran")) {
                    inputAllowed = true;
                }

                if (line.contains("BOOM") || line.contains("terkena bom")) {
                    SwingUtilities.invokeLater(this::disableAllButtons);
                    inputAllowed = false;
                }
            }
        } catch (IOException e) {
            showError("⚠️ Koneksi terputus.");
        }
    }

    private void enableAllButtons() {
        for (JButton btn : numberButtons) {
            if (btn != null && btn.isEnabled()) {
                btn.setBackground(new Color(0x3f78e0));
                btn.setForeground(Color.WHITE);
                btn.setEnabled(true);
            }
        }
        inputAllowed = true;
    }

    private void disableAllButtons() {
        for (JButton btn : numberButtons) {
            if (btn != null) {
                btn.setEnabled(false);
                btn.setBackground(new Color(180, 180, 180));
            }
        }
    }

    private void showError(String message) {
        area.append("ERROR: " + message + "\n");
        disableAllButtons();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ClientGUI("localhost", 8010));
    }
}
