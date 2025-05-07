package org.example;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.CallableStatement;
import java.sql.SQLException;

public class TourWindow extends JPanel {
    private final DatabaseManager dbManager;
    private JTextField nameField, typeField, locationField, priceField, daysField;

    public TourWindow(DatabaseManager dbManager) {
        this.dbManager = dbManager;
        setLayout(new BorderLayout());

        JPanel panel = new JPanel(new GridLayout(6, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        panel.add(new JLabel("Название:"));
        nameField = new JTextField();
        panel.add(nameField);

        panel.add(new JLabel("ID типа:"));
        typeField = new JTextField();
        panel.add(typeField);

        panel.add(new JLabel("ID локации:"));
        locationField = new JTextField();
        panel.add(locationField);

        panel.add(new JLabel("Цена за день:"));
        priceField = new JTextField();
        panel.add(priceField);

        panel.add(new JLabel("Дни:"));
        daysField = new JTextField();
        panel.add(daysField);

        JButton submitButton = new JButton("Подтвердить");
        submitButton.addActionListener(e -> submitTour());
        panel.add(submitButton);

        add(panel, BorderLayout.CENTER);
    }

    private void submitTour() {
        try (Connection conn = dbManager.getConnection()) {
            String name = nameField.getText();
            int typeId = Integer.parseInt(typeField.getText());
            int locationId = Integer.parseInt(locationField.getText());
            double price = Double.parseDouble(priceField.getText());
            int days = Integer.parseInt(daysField.getText());

            try (CallableStatement cstmt = conn.prepareCall("CALL public.add_new_tour(?, ?, ?, ?, ?)")) {
                cstmt.setString(1, name);
                cstmt.setInt(2, typeId);
                cstmt.setInt(3, locationId);
                cstmt.setDouble(4, price);
                cstmt.setInt(5, days);
                cstmt.execute();
            }
            conn.commit();
            JOptionPane.showMessageDialog(this, "Тур успешно добавлен!", "Успех", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Ошибка: " + ex.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Пожалуйста, введите корректные числа для ID типа, ID локации, цены и дней.", "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }
}