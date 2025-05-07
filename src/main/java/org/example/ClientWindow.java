package org.example;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.CallableStatement;
import java.sql.SQLException;
import java.sql.Date;

public class ClientWindow extends JPanel {
    private final DatabaseManager dbManager;
    private JTextField fullNameField, phoneField, emailField, passportField, birthDateField, usernameField, passwordField;
    private JCheckBox registerNewUserCheckBox;

    public ClientWindow(DatabaseManager dbManager) {
        this.dbManager = dbManager;
        setLayout(new BorderLayout());

        JPanel panel = new JPanel(new GridLayout(9, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        panel.add(new JLabel("ФИО:"));
        fullNameField = new JTextField();
        panel.add(fullNameField);

        panel.add(new JLabel("Телефон:"));
        phoneField = new JTextField();
        panel.add(phoneField);

        panel.add(new JLabel("Электронная почта:"));
        emailField = new JTextField();
        panel.add(emailField);

        panel.add(new JLabel("Номер паспорта:"));
        passportField = new JTextField();
        panel.add(passportField);

        panel.add(new JLabel("Дата рождения (ГГГГ-ММ-ДД):"));
        birthDateField = new JTextField();
        panel.add(birthDateField);

        panel.add(new JLabel("Имя пользователя (для нового):"));
        usernameField = new JTextField();
        panel.add(usernameField);

        panel.add(new JLabel("Пароль (для нового):"));
        passwordField = new JPasswordField();
        panel.add(passwordField);

        registerNewUserCheckBox = new JCheckBox("Зарегистрировать как нового пользователя (роль: клиент)");
        panel.add(registerNewUserCheckBox);

        JButton submitButton = new JButton("Подтвердить");
        submitButton.addActionListener(e -> submitClient());
        panel.add(submitButton);

        add(panel, BorderLayout.CENTER);
    }

    private void submitClient() {
        try (Connection conn = dbManager.getConnection()) {
            String fullName = fullNameField.getText();
            String phone = phoneField.getText();
            String email = emailField.getText();
            String passport = passportField.getText();
            String birthDateStr = birthDateField.getText();

            Date birthDate = null;
            try {
                birthDate = Date.valueOf(birthDateStr);
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(this, "Неверный формат даты. Используйте ГГГГ-ММ-ДД.", "Ошибка", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (registerNewUserCheckBox.isSelected()) {
                String username = usernameField.getText();
                String password = new String(((JPasswordField) passwordField).getPassword());
                if (username.isEmpty() || password.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Имя пользователя и пароль обязательны для регистрации нового пользователя.", "Ошибка", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                dbManager.registerUser(conn, username, password, "client");
                try (CallableStatement cstmt = conn.prepareCall("CALL public.register_new_client(?, ?, ?, ?, ?)")) {
                    cstmt.setString(1, fullName);
                    cstmt.setString(2, phone);
                    cstmt.setString(3, email);
                    cstmt.setString(4, passport);
                    cstmt.setDate(5, birthDate);
                    cstmt.execute();
                }
                conn.commit();
                JOptionPane.showMessageDialog(this, "Клиент и пользователь успешно зарегистрированы! Теперь можно войти с именем: " + username, "Успех", JOptionPane.INFORMATION_MESSAGE);
            } else {
                try (CallableStatement cstmt = conn.prepareCall("CALL public.register_new_client(?, ?, ?, ?, ?)")) {
                    cstmt.setString(1, fullName);
                    cstmt.setString(2, phone);
                    cstmt.setString(3, email);
                    cstmt.setString(4, passport);
                    cstmt.setDate(5, birthDate);
                    cstmt.execute();
                }
                conn.commit();
                JOptionPane.showMessageDialog(this, "Клиент успешно добавлен!", "Успех", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Ошибка: " + ex.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }
}