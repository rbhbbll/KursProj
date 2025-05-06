package org.example;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

public class BookingWindow extends JFrame {
    private final DatabaseManager dbManager;
    private final User user;
    private JComboBox<Integer> clientCombo, tourCombo;

    public BookingWindow(DatabaseManager dbManager, User user) {
        this.dbManager = dbManager;
        this.user = user;
        setTitle("Make Booking");
        setSize(400, 200);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel panel = new JPanel(new GridLayout(3, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        panel.add(new JLabel("Client:"));
        clientCombo = new JComboBox<>();
        if (user.isAdmin()) {
            loadComboBox(clientCombo, "SELECT id FROM public.clients");
        } else {
            clientCombo.addItem(user.getId());
        }
        panel.add(clientCombo);

        panel.add(new JLabel("Tour:"));
        tourCombo = new JComboBox<>();
        loadComboBox(tourCombo, "SELECT id FROM public.tours");
        panel.add(tourCombo);

        JButton submitButton = new JButton("Book");
        submitButton.addActionListener(e -> submitBooking());
        panel.add(submitButton);

        add(panel);
    }

    private void loadComboBox(JComboBox<Integer> comboBox, String query) {
        try (Connection conn = dbManager.getConnection();
             ResultSet rs = conn.createStatement().executeQuery(query)) {
            while (rs.next()) {
                comboBox.addItem(rs.getInt("id"));
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void submitBooking() {
        try {
            int clientId = (Integer) clientCombo.getSelectedItem();
            int tourId = (Integer) tourCombo.getSelectedItem();
            dbManager.makeBooking(clientId, tourId);
            JOptionPane.showMessageDialog(this, "Booking created successfully!");
            dispose();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}