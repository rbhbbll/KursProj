package org.example;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.CallableStatement;
import java.sql.SQLException;

public class TourWindow extends JFrame {
    private final DatabaseManager dbManager;
    private JTextField nameField, typeField, locationField, priceField, daysField;

    public TourWindow(DatabaseManager dbManager) {
        this.dbManager = dbManager;
        setTitle("Add Tour");
        setSize(400, 300);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel panel = new JPanel(new GridLayout(6, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        panel.add(new JLabel("Name:"));
        nameField = new JTextField();
        panel.add(nameField);

        panel.add(new JLabel("Type ID:"));
        typeField = new JTextField();
        panel.add(typeField);

        panel.add(new JLabel("Location ID:"));
        locationField = new JTextField();
        panel.add(locationField);

        panel.add(new JLabel("Price:"));
        priceField = new JTextField();
        panel.add(priceField);

        panel.add(new JLabel("Days:"));
        daysField = new JTextField();
        panel.add(daysField);

        JButton submitButton = new JButton("Submit");
        submitButton.addActionListener(e -> submitTour());
        panel.add(submitButton);

        add(panel);
    }

    private void submitTour() {
        try {
            String name = nameField.getText();
            int typeId = Integer.parseInt(typeField.getText());
            int locationId = Integer.parseInt(locationField.getText());
            double price = Double.parseDouble(priceField.getText());
            int days = Integer.parseInt(daysField.getText());

            try (Connection conn = dbManager.getConnection();
                 CallableStatement cstmt = conn.prepareCall("{CALL public.add_new_tour(?, ?, ?, ?, ?)}")) {
                cstmt.setString(1, name);
                cstmt.setInt(2, typeId);
                cstmt.setInt(3, locationId);
                cstmt.setDouble(4, price);
                cstmt.setInt(5, days);
                cstmt.execute();
                JOptionPane.showMessageDialog(this, "Tour added successfully!");
                dispose();
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter valid numbers for Type ID, Location ID, Price, and Days.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}