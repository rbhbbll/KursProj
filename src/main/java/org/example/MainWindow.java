package org.example;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.ResultSet;
import java.sql.SQLException;

public class MainWindow extends JFrame {
    private final DatabaseManager dbManager;
    private final User user;
    private JTabbedPane tabbedPane;

    public MainWindow(DatabaseManager dbManager, User user) {
        this.dbManager = dbManager;
        this.user = user;
        setTitle("Tour Agency - " + user.getUsername());
        setSize(800, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        initUI();
    }

    private void initUI() {
        tabbedPane = new JTabbedPane();

        if (user.isAdmin()) {
            addAdminTabs();
        } else {
            addClientTabs();
        }

        add(tabbedPane);
    }

    private void addAdminTabs() {
        tabbedPane.addTab("Tours", createTablePanel("tours"));
        tabbedPane.addTab("Bookings", createTablePanel("bookings"));
        tabbedPane.addTab("Clients", createTablePanel("clients"));
        tabbedPane.addTab("Add Tour", createAddTourPanel());
        tabbedPane.addTab("Add Client", createAddClientPanel());
        tabbedPane.addTab("Tours by Service", createToursByServicePanel());
        tabbedPane.addTab("Tours by Price", createToursByPricePanel());
    }

    private void addClientTabs() {
        tabbedPane.addTab("My Bookings", createTablePanel("bookings", "client_id = " + user.getId()));
        tabbedPane.addTab("Available Tours", createTablePanel("tours"));
        tabbedPane.addTab("Book Tour", createBookingPanel());
    }

    private JPanel createTablePanel(String tableName) {
        return createTablePanel(tableName, null);
    }

    private JPanel createTablePanel(String tableName, String condition) {
        JPanel panel = new JPanel(new BorderLayout());
        JTable table = new JTable();
        JScrollPane scrollPane = new JScrollPane(table);
        panel.add(scrollPane, BorderLayout.CENTER);

        try {
            String query = "SELECT * FROM public." + tableName;
            if (condition != null) {
                query += " WHERE " + condition;
            }
            ResultSet rs = dbManager.executeQuery(query);
            table.setModel(convertToTableModel(rs));
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }

        if (user.isAdmin() && tableName.equals("clients")) {
            JButton deleteButton = new JButton("Delete Selected Client");
            deleteButton.addActionListener(e -> {
                int selectedRow = table.getSelectedRow();
                if (selectedRow >= 0) {
                    int clientId = (Integer) table.getValueAt(selectedRow, 0);
                    int confirm = JOptionPane.showConfirmDialog(this,
                            "Are you sure you want to delete client ID " + clientId + "?",
                            "Confirm Deletion", JOptionPane.YES_NO_OPTION);
                    if (confirm == JOptionPane.YES_OPTION) {
                        try {
                            dbManager.deleteClient(clientId);
                            refreshTable(table, tableName, condition);
                            JOptionPane.showMessageDialog(this, "Client deleted successfully!");
                        } catch (SQLException ex) {
                            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                } else {
                    JOptionPane.showMessageDialog(this, "Please select a client to delete.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            });
            panel.add(deleteButton, BorderLayout.SOUTH);
        }

        return panel;
    }

    private JPanel createAddTourPanel() {
        JPanel panel = new JPanel();
        JButton addTourButton = new JButton("Open Tour Creation");
        addTourButton.addActionListener(e -> new TourWindow(dbManager).setVisible(true));
        panel.add(addTourButton);
        return panel;
    }

    private JPanel createAddClientPanel() {
        JPanel panel = new JPanel();
        JButton addClientButton = new JButton("Open Client Creation");
        addClientButton.addActionListener(e -> new ClientWindow(dbManager).setVisible(true));
        panel.add(addClientButton);
        return panel;
    }

    private JPanel createBookingPanel() {
        JPanel panel = new JPanel();
        JButton bookTourButton = new JButton("Open Booking");
        bookTourButton.addActionListener(e -> new BookingWindow(dbManager, user).setVisible(true));
        panel.add(bookTourButton);
        return panel;
    }

    private JPanel createToursByServicePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JTextField serviceField = new JTextField(20);
        JButton searchButton = new JButton("Search Tours by Service");
        JTable table = new JTable();
        JScrollPane scrollPane = new JScrollPane(table);
        JPanel inputPanel = new JPanel();
        inputPanel.add(new JLabel("Service Name:"));
        inputPanel.add(serviceField);
        inputPanel.add(searchButton);
        panel.add(inputPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        searchButton.addActionListener(e -> {
            String serviceName = serviceField.getText();
            try {
                ResultSet rs = dbManager.getToursByService(serviceName);
                table.setModel(convertToTableModel(rs));
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        return panel;
    }

    private JPanel createToursByPricePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JTextField minPriceField = new JTextField(10);
        JTextField maxPriceField = new JTextField(10);
        JButton searchButton = new JButton("Search Tours by Price Range");
        JTable table = new JTable();
        JScrollPane scrollPane = new JScrollPane(table);
        JPanel inputPanel = new JPanel();
        inputPanel.add(new JLabel("Min Price:"));
        inputPanel.add(minPriceField);
        inputPanel.add(new JLabel("Max Price:"));
        inputPanel.add(maxPriceField);
        inputPanel.add(searchButton);
        panel.add(inputPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        searchButton.addActionListener(e -> {
            try {
                double minPrice = Double.parseDouble(minPriceField.getText());
                double maxPrice = Double.parseDouble(maxPriceField.getText());
                ResultSet rs = dbManager.getToursByPriceRange(minPrice, maxPrice);
                table.setModel(convertToTableModel(rs));
            } catch (NumberFormatException | SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        return panel;
    }

    private DefaultTableModel convertToTableModel(ResultSet rs) throws SQLException {
        DefaultTableModel model = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        java.sql.ResultSetMetaData meta = rs.getMetaData();
        int columnCount = meta.getColumnCount();
        for (int i = 1; i <= columnCount; i++) {
            model.addColumn(meta.getColumnName(i));
        }

        while (rs.next()) {
            Object[] row = new Object[columnCount];
            for (int i = 1; i <= columnCount; i++) {
                row[i - 1] = rs.getObject(i);
            }
            model.addRow(row);
        }

        return model;
    }

    private void refreshTable(JTable table, String tableName, String condition) {
        try {
            String query = "SELECT * FROM public." + tableName;
            if (condition != null) {
                query += " WHERE " + condition;
            }
            ResultSet rs = dbManager.executeQuery(query);
            table.setModel(convertToTableModel(rs));
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}