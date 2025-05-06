package org.example;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.CallableStatement;
import java.sql.SQLException;

public class ClientWindow extends JFrame {
    private final DatabaseManager dbManager;
    private JTextField fullNameField, phoneField, emailField, passportField, birthDateField;

    public ClientWindow(DatabaseManager dbManager) {
        this.dbManager = dbManager;
        setTitle("Add Client");
        setSize(400, 300);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel panel = new JPanel(new GridLayout(6, 2, 10, 10));
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

            try (Connection conn = dbManager.getConnection();
                 CallableStatement cstmt = conn.prepareCall("{CALL public.register_new_client(?, ?, ?, ?, ?)}")) {
                cstmt.setString(1, fullName);
                cstmt.setString(2, phone);
                cstmt.setString(3, email);
                cstmt.setString(4, passport);
                cstmt.setString(5, birthDate);
                cstmt.execute();
                JOptionPane.showMessageDialog(this, "Client added successfully!");
                dispose();
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}