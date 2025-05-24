package org.example;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

public class InitialWindow extends JFrame {
    private final DatabaseManager dbManager;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JComboBox<String> roleCombo;

    public InitialWindow(DatabaseManager dbManager) {
        this.dbManager = dbManager;
        setTitle("Регистрация или вход");
        setSize(300, 200);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridLayout(4, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        panel.add(new JLabel("Имя пользователя:"));
        usernameField = new JTextField();
        panel.add(usernameField);

        panel.add(new JLabel("Пароль:"));
        passwordField = new JPasswordField();
        panel.add(passwordField);

        panel.add(new JLabel("Роль:"));
        roleCombo = new JComboBox<>(new String[]{"администратор", "клиент"});
        panel.add(roleCombo);

        JButton submitButton = new JButton("Подтвердить");
        submitButton.addActionListener(e -> handleSubmit());
        panel.add(submitButton);

        add(panel);
    }

    private void handleSubmit() {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());
        String role = selectRole();

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Имя пользователя и пароль обязательны.", "Ошибка", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try (Connection conn = dbManager.getConnection()) {
            User existingUser = dbManager.authenticateUser(username, password);
            if (existingUser != null) {
                dispose();
                new MainWindow(dbManager, existingUser).setVisible(true);
                conn.commit();
            } else {
                dbManager.registerUser(conn, username, password, role);
                User newUser = dbManager.authenticateUser(username, password);
                if (newUser != null) {
                    JOptionPane.showMessageDialog(this, "Пользователь успешно зарегистрирован! Вход...", "Успех", JOptionPane.INFORMATION_MESSAGE);
                    dispose();
                    new MainWindow(dbManager, newUser).setVisible(true);
                } else {
                    JOptionPane.showMessageDialog(this, "Регистрация не удалась. Попробуйте снова.", "Ошибка", JOptionPane.ERROR_MESSAGE);
                }
                conn.commit();
            }
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