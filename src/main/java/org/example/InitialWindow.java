package org.example;


import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;

public class InitialWindow extends JFrame {
    private final DatabaseManager dbManager;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JComboBox<String> roleCombo;

    public InitialWindow(DatabaseManager dbManager) {
        this.dbManager = dbManager;
        setTitle("Register or Log In");
        setSize(300, 200);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridLayout(4, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        panel.add(new JLabel("Username:"));
        usernameField = new JTextField();
        panel.add(usernameField);

        panel.add(new JLabel("Password:"));
        passwordField = new JPasswordField();
        panel.add(passwordField);

        panel.add(new JLabel("Role:"));
        roleCombo = new JComboBox<>(new String[]{"admin", "client"});
        panel.add(roleCombo);

        JButton submitButton = new JButton("Submit");
        submitButton.addActionListener(e -> handleSubmit());
        panel.add(submitButton);

        add(panel);
    }

    private void handleSubmit() {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());
        String role = (String) roleCombo.getSelectedItem();

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Username and password are required.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            User existingUser = dbManager.authenticateUser(username, password);
            if (existingUser != null) {
                // User exists, proceed to MainWindow
                dispose();
                new MainWindow(dbManager, existingUser).setVisible(true);
            } else {
                // Register new user
                dbManager.registerUser(username, password, role);
                User newUser = dbManager.authenticateUser(username, password);
                if (newUser != null) {
                    JOptionPane.showMessageDialog(this, "User registered successfully! Logging in...", "Success", JOptionPane.INFORMATION_MESSAGE);
                    dispose();
                    new MainWindow(dbManager, newUser).setVisible(true);
                } else {
                    JOptionPane.showMessageDialog(this, "Registration failed. Please try again.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}