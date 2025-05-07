package org.example;

import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class MainWindow extends JFrame {
    private final DatabaseManager dbManager;
    private final User user;

    public MainWindow(DatabaseManager dbManager, User user) {
        this.dbManager = dbManager;
        this.user = user;
        setTitle("Tour Agency - Welcome, " + user.getUsername());
        setSize(600, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        if (user.isAdmin()) {
            JTabbedPane tabbedPane = new JTabbedPane();
            tabbedPane.addTab("Add Client", new ClientWindow(dbManager).getContentPane());
            tabbedPane.addTab("Add Tour", new TourWindow(dbManager).getContentPane());
            tabbedPane.addTab("Make Booking", new BookingWindow(dbManager, user));
            add(tabbedPane);
        } else {
            add(new BookingWindow(dbManager, user));
        }
    }

    private int getRowCount(String query) {
        try {
            ResultSet rs = dbManager.executeQuery(query);
            if (rs.last()) {
                return rs.getRow();
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
        return 0;
    }
}