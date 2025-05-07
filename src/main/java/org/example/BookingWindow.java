package org.example;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class BookingWindow extends JPanel {
    private final DatabaseManager dbManager;
    private final User user;
    private JComboBox<Integer> clientCombo;
    private JComboBox<Integer> tourCombo;

    public BookingWindow(DatabaseManager dbManager, User user) {
        this.dbManager = dbManager;
        this.user = user;
        setLayout(new GridLayout(4, 2, 10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        clientCombo = new JComboBox<>();
        tourCombo = new JComboBox<>();
        loadComboBoxes();

        add(new JLabel("Client ID:"));
        add(clientCombo);
        add(new JLabel("Tour ID:"));
        add(tourCombo);

        JButton submitButton = new JButton("Submit");
        submitButton.addActionListener(e -> submitBooking());
        add(submitButton);

        if (user.isAdmin()) {
            loadComboBox(clientCombo, "SELECT id FROM public.clients");
        } else {
            clientCombo.addItem(user.getId());
        }
    }

    private void loadComboBoxes() {
        loadComboBox(clientCombo, "SELECT id FROM public.clients");
        loadComboBox(tourCombo, "SELECT id FROM public.tours");
    }

    private void loadComboBox(JComboBox<Integer> comboBox, String query) {
        comboBox.removeAllItems();
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                comboBox.addItem(rs.getInt(1));
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error loading data: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void submitBooking() {
        int clientId = (Integer) clientCombo.getSelectedItem();
        int tourId = (Integer) tourCombo.getSelectedItem();
        if (clientId != 0 && tourId != 0) {
            try {
                dbManager.makeBooking(clientId, tourId);
                JOptionPane.showMessageDialog(this, "Booking added successfully!");
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(this, "Please select valid Client ID and Tour ID.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}