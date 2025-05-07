package org.example;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.SQLException;

public class BookingWindow extends JPanel {
    private final DatabaseManager dbManager;
    private final User user;
    private JTextField clientIdField, tourIdField;

    public BookingWindow(DatabaseManager dbManager, User user) {
        this.dbManager = dbManager;
        this.user = user;
        setLayout(new BorderLayout());

        JPanel panel = new JPanel(new GridLayout(3, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        panel.add(new JLabel("ID клиента:"));
        clientIdField = new JTextField();
        panel.add(clientIdField);

        panel.add(new JLabel("ID тура:"));
        tourIdField = new JTextField();
        panel.add(tourIdField);

        JButton submitButton = new JButton("Подтвердить");
        submitButton.addActionListener(e -> submitBooking());
        panel.add(submitButton);

        add(panel, BorderLayout.CENTER);
    }

    private void submitBooking() {
        try (Connection conn = dbManager.getConnection()) {
            int clientId = Integer.parseInt(clientIdField.getText());
            int tourId = Integer.parseInt(tourIdField.getText());

            dbManager.makeBooking(conn, clientId, tourId);
            conn.commit();
            JOptionPane.showMessageDialog(this, "Бронирование успешно создано!", "Успех", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Ошибка: " + ex.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Пожалуйста, введите корректные числа для ID клиента и ID тура.", "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }
}