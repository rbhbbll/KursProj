package org.example;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;

public class LoginWindow extends JFrame {
    private final DatabaseManager dbManager;
    private JTextField usernameField;
    private JPasswordField passwordField;

    public LoginWindow(DatabaseManager dbManager) {
        this.dbManager = dbManager;
        setTitle("Вход в систему");
        setSize(300, 200);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridLayout(3, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        panel.add(new JLabel("Имя пользователя:"));
        usernameField = new JTextField();
        panel.add(usernameField);

        panel.add(new JLabel("Пароль:"));
        passwordField = new JPasswordField();
        panel.add(passwordField);

        JButton loginButton = new JButton("Войти");
        loginButton.addActionListener(e -> authenticate());
        panel.add(loginButton);

        JButton registerButton = new JButton("Регистрация");
        registerButton.addActionListener(e -> openRegistration());
        panel.add(registerButton);

        add(panel);
    }

    private void authenticate() {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Имя пользователя и пароль обязательны.", "Ошибка", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            User user = dbManager.authenticateUser(username, password);
            if (user != null) {
                dispose();
                new MainWindow(dbManager, user).setVisible(true);
            } else {
                JOptionPane.showMessageDialog(this, "Неверное имя пользователя или пароль", "Ошибка", JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Ошибка базы данных: " + ex.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openRegistration() {
        dispose();
        new InitialWindow(dbManager).setVisible(true);
    }
}