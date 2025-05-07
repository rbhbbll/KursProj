package org.example;

import javax.swing.*;
import java.awt.*;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Vector;

public class ViewDataPanel extends JPanel {
    private final DatabaseManager dbManager;
    private final User user;

    public ViewDataPanel(DatabaseManager dbManager, User user) {
        this.dbManager = dbManager;
        this.user = user;
        setLayout(new BorderLayout());

        JTabbedPane tabbedPane = new JTabbedPane();

        tabbedPane.addTab("Пользователи", createTablePanel("SELECT * FROM public.users"));
        tabbedPane.addTab("Клиенты", createTablePanel("SELECT * FROM public.clients"));
        tabbedPane.addTab("Туры", createTablePanel("SELECT * FROM public.tours"));
        tabbedPane.addTab("Бронирования", createTablePanel("SELECT * FROM public.bookings"));

        add(tabbedPane, BorderLayout.CENTER);
    }

    private JPanel createTablePanel(String query) {
        JPanel panel = new JPanel(new BorderLayout());
        try {
            Vector<Vector<Object>> data = dbManager.getTableData(query);
            Vector<String> columnNames = dbManager.getColumnNames(query);

            JTable table = new JTable(data, columnNames);
            table.setFillsViewportHeight(true);
            JScrollPane scrollPane = new JScrollPane(table);
            panel.add(scrollPane, BorderLayout.CENTER);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Ошибка загрузки данных: " + ex.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
        return panel;
    }
}