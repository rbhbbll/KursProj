package org.example;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Date;
import java.util.Objects;

public class InitialWindow extends JFrame {
    private final DatabaseManager dbManager;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JComboBox<String> roleCombo;
    // Client fields panel
    private JPanel clientFieldsPanel;
    private JTextField fullNameField;
    private JTextField phoneField;
    private JTextField emailField;
    private JTextField passportField;
    private JTextField birthDateField;

    public InitialWindow(DatabaseManager dbManager) {
        this.dbManager = dbManager;
        setTitle("Регистрация");
        setSize(400, 500);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        // Main panel with card layout to switch between admin and client registration
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // User account fields panel
        JPanel userFieldsPanel = new JPanel(new GridLayout(4, 2, 10, 10));
        
        userFieldsPanel.add(new JLabel("Имя пользователя:"));
        usernameField = new JTextField();
        userFieldsPanel.add(usernameField);

        userFieldsPanel.add(new JLabel("Пароль:"));
        passwordField = new JPasswordField();
        userFieldsPanel.add(passwordField);

        userFieldsPanel.add(new JLabel("Роль:"));
        roleCombo = new JComboBox<>(new String[]{"администратор", "клиент"});
        roleCombo.addActionListener(e -> updateFieldsVisibility());
        userFieldsPanel.add(roleCombo);

        // Client information fields panel
        clientFieldsPanel = new JPanel(new GridLayout(5, 2, 10, 10));
        clientFieldsPanel.setBorder(BorderFactory.createTitledBorder("Информация о клиенте"));

        clientFieldsPanel.add(new JLabel("ФИО:"));
        fullNameField = new JTextField();
        clientFieldsPanel.add(fullNameField);

        clientFieldsPanel.add(new JLabel("Телефон:"));
        phoneField = new JTextField();
        clientFieldsPanel.add(phoneField);

        clientFieldsPanel.add(new JLabel("Электронная почта:"));
        emailField = new JTextField();
        clientFieldsPanel.add(emailField);

        clientFieldsPanel.add(new JLabel("Номер паспорта:"));
        passportField = new JTextField();
        clientFieldsPanel.add(passportField);

        clientFieldsPanel.add(new JLabel("Дата рождения (ГГГГ-ММ-ДД):"));
        birthDateField = new JTextField();
        clientFieldsPanel.add(birthDateField);

        // Buttons panel
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton submitButton = new JButton("Зарегистрироваться");
        submitButton.addActionListener(e -> handleSubmit());
        JButton loginButton = new JButton("Войти");
        loginButton.addActionListener(e -> openLogin());
        
        buttonsPanel.add(submitButton);
        buttonsPanel.add(loginButton);

        // Add panels to main panel
        mainPanel.add(userFieldsPanel, BorderLayout.NORTH);
        mainPanel.add(clientFieldsPanel, BorderLayout.CENTER);
        mainPanel.add(buttonsPanel, BorderLayout.SOUTH);

        add(mainPanel);
        
        // Initialize visibility
        updateFieldsVisibility();
    }

    private void updateFieldsVisibility() {
        String role = (String) roleCombo.getSelectedItem();
        clientFieldsPanel.setVisible("клиент".equals(role));
        pack(); // Resize window to fit content
    }

    private void openLogin() {
        dispose();
        new LoginWindow(dbManager).setVisible(true);
    }

    private void handleSubmit() {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());
        String role = selectRole();

        // Basic validation
        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Имя пользователя и пароль обязательны.", "Ошибка", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try (Connection conn = dbManager.getConnection()) {
            // Check if user already exists
            if (dbManager.authenticateUser(username, password) != null) {
                JOptionPane.showMessageDialog(this, "Пользователь с таким именем уже существует.", "Ошибка", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // For new registration, validate client fields if role is "client"
            if ("client".equals(role)) {
                String fullName = fullNameField.getText();
                String phone = phoneField.getText();
                String email = emailField.getText();
                String passport = passportField.getText();
                String birthDateStr = birthDateField.getText();

                if (fullName.isEmpty() || birthDateStr.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "ФИО и дата рождения обязательны для клиента.", "Ошибка", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                try {
                    Date birthDate = Date.valueOf(birthDateStr);
                    
                    // First register the user
                    dbManager.registerUser(conn, username, password, role);
                    
                    // Then register the client
                    try (var cstmt = conn.prepareCall("CALL public.register_new_client(?, ?, ?, ?, ?)")) {
                        cstmt.setString(1, fullName);
                        cstmt.setString(2, phone);
                        cstmt.setString(3, email);
                        cstmt.setString(4, passport);
                        cstmt.setDate(5, birthDate);
                        cstmt.execute();
                    }
                } catch (IllegalArgumentException ex) {
                    JOptionPane.showMessageDialog(this, "Неверный формат даты. Используйте ГГГГ-ММ-ДД.", "Ошибка", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            } else {
                // For admin, just register the user
                dbManager.registerUser(conn, username, password, role);
            }

            conn.commit();
            JOptionPane.showMessageDialog(this, "Регистрация успешна! Теперь вы можете войти.", "Успех", JOptionPane.INFORMATION_MESSAGE);
            openLogin();
            
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Ошибка базы данных: " + ex.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String selectRole() {
        String role = (String) roleCombo.getSelectedItem();
        if(Objects.equals(role, "администратор")){
            role = "admin";
        } else if (Objects.equals(role, "клиент")) {
            role = "client";
        }else {
            throw new IllegalArgumentException("Unknown role: " + role);
        }
        return role;
    }
}