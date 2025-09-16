package org.example;

import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

public class TourWindow extends JPanel {
    private final DatabaseManager dbManager;
    private JTextField nameField, priceField, daysField;
    private JComboBox<String> typeCombo, locationCombo;
    private Map<String, Integer> tourTypeIds = new HashMap<>();
    private Map<String, Integer> locationIds = new HashMap<>();
    private Map<String, Integer> serviceIds = new HashMap<>();
    private Map<String, Integer> attractionIds = new HashMap<>();
    private JList<String> servicesList;
    private JList<String> attractionsList;
    private DefaultListModel<String> servicesModel;
    private DefaultListModel<String> attractionsModel;

    public TourWindow(DatabaseManager dbManager) {
        this.dbManager = dbManager;
        setLayout(new BorderLayout());

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Основные поля тура
        JPanel basicFieldsPanel = new JPanel(new GridLayout(5, 2, 10, 10));
        
        basicFieldsPanel.add(new JLabel("Название:"));
        nameField = new JTextField();
        basicFieldsPanel.add(nameField);

        basicFieldsPanel.add(new JLabel("Тип тура:"));
        typeCombo = new JComboBox<>();
        loadTourTypes();
        basicFieldsPanel.add(typeCombo);

        basicFieldsPanel.add(new JLabel("Локация:"));
        locationCombo = new JComboBox<>();
        loadLocations();
        basicFieldsPanel.add(locationCombo);

        basicFieldsPanel.add(new JLabel("Цена за день:"));
        priceField = new JTextField();
        basicFieldsPanel.add(priceField);

        basicFieldsPanel.add(new JLabel("Дни:"));
        daysField = new JTextField();
        basicFieldsPanel.add(daysField);

        mainPanel.add(basicFieldsPanel, BorderLayout.NORTH);

        // Панель выбора сервисов и достопримечательностей
        JPanel selectionPanel = new JPanel(new GridLayout(1, 2, 10, 10));
        
        // Панель выбора сервисов
        JPanel servicesPanel = new JPanel(new BorderLayout());
        servicesPanel.setBorder(BorderFactory.createTitledBorder("Выберите услуги"));
        
        servicesModel = new DefaultListModel<>();
        servicesList = new JList<>(servicesModel);
        servicesList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        servicesList.setCellRenderer(new CheckboxListCellRenderer());
        
        loadServices();
        
        JScrollPane servicesScrollPane = new JScrollPane(servicesList);
        servicesScrollPane.setPreferredSize(new Dimension(300, 150));
        servicesPanel.add(servicesScrollPane, BorderLayout.CENTER);

        // Панель выбора достопримечательностей
        JPanel attractionsPanel = new JPanel(new BorderLayout());
        attractionsPanel.setBorder(BorderFactory.createTitledBorder("Выберите достопримечательности"));
        
        attractionsModel = new DefaultListModel<>();
        attractionsList = new JList<>(attractionsModel);
        attractionsList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        attractionsList.setCellRenderer(new CheckboxListCellRenderer());
        
        loadAttractions();
        
        JScrollPane attractionsScrollPane = new JScrollPane(attractionsList);
        attractionsScrollPane.setPreferredSize(new Dimension(300, 150));
        attractionsPanel.add(attractionsScrollPane, BorderLayout.CENTER);

        selectionPanel.add(servicesPanel);
        selectionPanel.add(attractionsPanel);
        mainPanel.add(selectionPanel, BorderLayout.CENTER);

        // Кнопка подтверждения
        JButton submitButton = new JButton("Подтвердить");
        submitButton.addActionListener(e -> submitTour());
        mainPanel.add(submitButton, BorderLayout.SOUTH);

        add(mainPanel, BorderLayout.CENTER);
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

    private void loadServices() {
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, name, price FROM public.services ORDER BY name")) {
            
            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                double price = rs.getDouble("price");
                String displayName = String.format("%s (%.2f руб.)", name, price);
                
                serviceIds.put(displayName, id);
                servicesModel.addElement(displayName);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, 
                "Ошибка при загрузке сервисов: " + ex.getMessage(), 
                "Ошибка", 
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadAttractions() {
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, name FROM public.attractions ORDER BY name")) {
            
            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                
                attractionIds.put(name, id);
                attractionsModel.addElement(name);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, 
                "Ошибка при загрузке достопримечательностей: " + ex.getMessage(), 
                "Ошибка", 
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void submitTour() {
        try {
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

            // Получаем выбранные сервисы
            List<Integer> selectedServiceIds = new ArrayList<>();
            for (int i = 0; i < servicesList.getModel().getSize(); i++) {
                if (servicesList.isSelectedIndex(i)) {
                    String serviceDisplayName = servicesList.getModel().getElementAt(i);
                    selectedServiceIds.add(serviceIds.get(serviceDisplayName));
                }
            }

            // Получаем выбранные достопримечательности
            List<Integer> selectedAttractionIds = new ArrayList<>();
            for (int i = 0; i < attractionsList.getModel().getSize(); i++) {
                if (attractionsList.isSelectedIndex(i)) {
                    String attractionName = attractionsList.getModel().getElementAt(i);
                    selectedAttractionIds.add(attractionIds.get(attractionName));
                }
            }

            // Преобразуем в массивы объектов
            Integer[] serviceIdsArray = selectedServiceIds.toArray(new Integer[0]);
            Integer[] attractionIdsArray = selectedAttractionIds.toArray(new Integer[0]);

            // Используем новый метод добавления тура
            dbManager.addTour(name, typeId, locationId, price, days, serviceIdsArray, attractionIdsArray);

            JOptionPane.showMessageDialog(this, 
                "Тур успешно добавлен!", 
                "Успех", 
                JOptionPane.INFORMATION_MESSAGE);

            // Очищаем поля после успешного добавления
            nameField.setText("");
            priceField.setText("");
            daysField.setText("");
            typeCombo.setSelectedIndex(0);
            locationCombo.setSelectedIndex(0);
            servicesList.clearSelection();
            attractionsList.clearSelection();

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, 
                "Ошибка при добавлении тура: " + ex.getMessage(), 
                "Ошибка", 
                JOptionPane.ERROR_MESSAGE);
        }
    }

    // Внутренний класс для отображения чекбоксов в списке
    private static class CheckboxListCellRenderer extends JCheckBox implements ListCellRenderer<String> {
        @Override
        public Component getListCellRendererComponent(JList<? extends String> list, String value, int index,
                boolean isSelected, boolean cellHasFocus) {
            setText(value);
            setSelected(isSelected);
            setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());
            setForeground(isSelected ? list.getSelectionForeground() : list.getForeground());
            return this;
        }
    }
}