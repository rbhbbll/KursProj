package org.example;

import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class TourWindow extends JPanel {
    private final DatabaseManager dbManager;
    private JTextField nameField, priceField, daysField;
    private JComboBox<String> typeCombo, locationCombo;
    private Map<String, Integer> tourTypeIds = new HashMap<>();
    private Map<String, Integer> locationIds = new HashMap<>();

    public TourWindow(DatabaseManager dbManager) {
        this.dbManager = dbManager;
        setLayout(new BorderLayout());

        JPanel panel = new JPanel(new GridLayout(6, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        panel.add(new JLabel("Название:"));
        nameField = new JTextField();
        panel.add(nameField);

        panel.add(new JLabel("Тип тура:"));
        typeCombo = new JComboBox<>();
        loadTourTypes();
        panel.add(typeCombo);

        panel.add(new JLabel("Локация:"));
        locationCombo = new JComboBox<>();
        loadLocations();
        panel.add(locationCombo);

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

    private void loadTourTypes() {
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, name FROM public.tour_types")) {
            
            while (rs.next()) {
                String name = rs.getString("name");
                int id = rs.getInt("id");
                tourTypeIds.put(name, id);
                typeCombo.addItem(name);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, 
                "Ошибка при загрузке типов туров: " + ex.getMessage(), 
                "Ошибка", 
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadLocations() {
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, country, city, place_name FROM public.locations")) {
            
            while (rs.next()) {
                String displayName = String.format("%s, %s - %s", 
                    rs.getString("country"),
                    rs.getString("city"),
                    rs.getString("place_name"));
                int id = rs.getInt("id");
                locationIds.put(displayName, id);
                locationCombo.addItem(displayName);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, 
                "Ошибка при загрузке локаций: " + ex.getMessage(), 
                "Ошибка", 
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void submitTour() {
        try (Connection conn = dbManager.getConnection()) {
            String name = nameField.getText();
            String selectedType = (String) typeCombo.getSelectedItem();
            String selectedLocation = (String) locationCombo.getSelectedItem();
            
            if (name.isEmpty() || selectedType == null || selectedLocation == null) {
                JOptionPane.showMessageDialog(this, 
                    "Пожалуйста, заполните все поля", 
                    "Ошибка", 
                    JOptionPane.ERROR_MESSAGE);
                return;
            }

            int typeId = tourTypeIds.get(selectedType);
            int locationId = locationIds.get(selectedLocation);
            double price;
            int days;

            try {
                price = Double.parseDouble(priceField.getText());
                days = Integer.parseInt(daysField.getText());
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, 
                    "Пожалуйста, введите корректные числовые значения для цены и дней", 
                    "Ошибка", 
                    JOptionPane.ERROR_MESSAGE);
                return;
            }

            try (CallableStatement cstmt = conn.prepareCall("CALL public.add_new_tour(?, ?, ?, ?, ?)")) {
                cstmt.setString(1, name);
                cstmt.setInt(2, typeId);
                cstmt.setInt(3, locationId);
                cstmt.setDouble(4, price);
                cstmt.setInt(5, days);
                cstmt.execute();
            }

            conn.commit();
            JOptionPane.showMessageDialog(this, 
                "Тур успешно добавлен!", 
                "Успех", 
                JOptionPane.INFORMATION_MESSAGE);

            // Clear fields after successful submission
            nameField.setText("");
            priceField.setText("");
            daysField.setText("");
            typeCombo.setSelectedIndex(0);
            locationCombo.setSelectedIndex(0);

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, 
                "Ошибка при добавлении тура: " + ex.getMessage(), 
                "Ошибка", 
                JOptionPane.ERROR_MESSAGE);
        }
    }
}