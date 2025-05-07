package org.example;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.CallableStatement;
import java.sql.SQLException;

public class ClientWindow extends JFrame {
    private final DatabaseManager dbManager;
    private JTextField fullNameField, phoneField, emailField, passportField, birthDateField, usernameField, passwordField;
    private JCheckBox registerNewUserCheckBox;

    public ClientWindow(DatabaseManager dbManager) {
        this.dbManager = dbManager;
        setTitle("Add Client");
        setSize(400, 400);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel panel = new JPanel(new GridLayout(9, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        panel.add(new JLabel("Full Name:"));
        fullNameField = new JTextField();
        panel.add(fullNameField);

        panel.add(new JLabel("Phone:"));
        phoneField = new JTextField();
        panel.add(phoneField);

        panel.add(new JLabel("Email:"));
        emailField = new JTextField();
        panel.add(emailField);

        panel.add(new JLabel("Passport Number:"));
        passportField = new JTextField();
        panel.add(passportField);

        panel.add(new JLabel("Birth Date (YYYY-MM-DD):"));
        birthDateField = new JTextField();
        panel.add(birthDateField);

        panel.add(new JLabel("Username (for new user):"));
        usernameField = new JTextField();
        panel.add(usernameField);

        panel.add(new JLabel("Password (for new user):"));
        passwordField = new JPasswordField();
        panel.add(passwordField);

        registerNewUserCheckBox = new JCheckBox("Register as new user (role: client)");
        panel.add(registerNewUserCheckBox);

        JButton submitButton = new JButton("Submit");
        submitButton.addActionListener(e -> submitClient());
        panel.add(submitButton);

        add(panel);
    }

    private void submitClient() {
        try {
            String fullName = fullNameField.getText();
            String phone = phoneField.getText();
            String email = emailField.getText();
            String passport = passportField.getText();
            String birthDate = birthDateField.getText();

            if (registerNewUserCheckBox.isSelected()) {
                String username = usernameField.getText();
                String password = new String(((JPasswordField) passwordField).getPassword());
                if (username.isEmpty() || password.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Username and password are required for new user registration.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                dbManager.registerUser(username, password, "client");
                try (Connection conn = dbManager.getConnection();
                     CallableStatement cstmt = conn.prepareCall("{CALL public.register_new_client(?, ?, ?, ?, ?)}")) {
                    cstmt.setString(1, fullName);
                    cstmt.setString(2, phone);
                    cstmt.setString(3, email);
                    cstmt.setString(4, passport);
                    cstmt.setString(5, birthDate);
                    cstmt.execute();
                }
                JOptionPane.showMessageDialog(this, "Client and user registered successfully! You can now log in with username: " + username, "Success", JOptionPane.INFORMATION_MESSAGE);
            } else {
                try (Connection conn = dbManager.getConnection();
                     CallableStatement cstmt = conn.prepareCall("{CALL public.register_new_client(?, ?, ?, ?, ?)}")) {
                    cstmt.setString(1, fullName);
                    cstmt.setString(2, phone);
                    cstmt.setString(3, email);
                    cstmt.setString(4, passport);
                    cstmt.setString(5, birthDate);
                    cstmt.execute();
                }
                JOptionPane.showMessageDialog(this, "Client added successfully!");
            }
            dispose();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}