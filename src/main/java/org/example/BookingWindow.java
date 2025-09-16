package org.example;

import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

public class BookingWindow extends JPanel {
    private final DatabaseManager dbManager;
    private final User user;
    private JComboBox<String> clientCombo, tourCombo;
    private Map<String, Integer> clientIds = new HashMap<>();
    private Map<String, Integer> tourIds = new HashMap<>();
    private Map<String, Integer> serviceIds = new HashMap<>();
    private JLabel priceLabel;
    private JLabel phoneLabel;
    private JLabel faxLabel;
    private JList<String> servicesList;
    private DefaultListModel<String> servicesModel;
    private int currentTourId = -1;

    public BookingWindow(DatabaseManager dbManager, User user) {
        this.dbManager = dbManager;
        this.user = user;
        setLayout(new BorderLayout());

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Основные поля бронирования
        JPanel basicFieldsPanel = new JPanel(new GridLayout(5, 2, 10, 10));
        
        // Компонент выбора клиента
        basicFieldsPanel.add(new JLabel("Выберите клиента:"));
        clientCombo = new JComboBox<>();
        loadClients();
        basicFieldsPanel.add(clientCombo);

        // Компонент выбора тура
        basicFieldsPanel.add(new JLabel("Выберите тур:"));
        tourCombo = new JComboBox<>();
        loadTours();
        tourCombo.addActionListener(e -> {
            updatePrice();
            loadTourServices();
            updateLocationInfo();
        });
        basicFieldsPanel.add(tourCombo);

        // Отображение цены
        basicFieldsPanel.add(new JLabel("Итоговая цена:"));
        priceLabel = new JLabel("0.00 руб.");
        basicFieldsPanel.add(priceLabel);

        // Отображение контактной информации локации
        basicFieldsPanel.add(new JLabel("Телефон локации:"));
        phoneLabel = new JLabel("-");
        basicFieldsPanel.add(phoneLabel);

        basicFieldsPanel.add(new JLabel("Факс локации:"));
        faxLabel = new JLabel("-");
        basicFieldsPanel.add(faxLabel);

        mainPanel.add(basicFieldsPanel, BorderLayout.NORTH);

        // Панель выбора сервисов
        JPanel servicesPanel = new JPanel(new BorderLayout());
        servicesPanel.setBorder(BorderFactory.createTitledBorder("Выберите дополнительные услуги"));
        
        servicesModel = new DefaultListModel<>();
        servicesList = new JList<>(servicesModel);
        servicesList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        servicesList.setCellRenderer(new CheckboxListCellRenderer());
        servicesList.addListSelectionListener(e -> updatePrice());
        
        JScrollPane servicesScrollPane = new JScrollPane(servicesList);
        servicesScrollPane.setPreferredSize(new Dimension(400, 150));
        servicesPanel.add(servicesScrollPane, BorderLayout.CENTER);

        mainPanel.add(servicesPanel, BorderLayout.CENTER);

        // Кнопка подтверждения
        JButton submitButton = new JButton("Подтвердить бронирование");
        submitButton.addActionListener(e -> submitBooking());
        mainPanel.add(submitButton, BorderLayout.SOUTH);

        add(mainPanel, BorderLayout.CENTER);
    }

    private void loadClients() {
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, full_name FROM public.clients ORDER BY full_name")) {
            
            while (rs.next()) {
                String fullName = rs.getString("full_name");
                int id = rs.getInt("id");
                clientIds.put(fullName, id);
                clientCombo.addItem(fullName);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, 
                "Ошибка при загрузке списка клиентов: " + ex.getMessage(), 
                "Ошибка", 
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadTours() {
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SELECT t.id, CONCAT(t.name, ' (', l.city, ' - ', tt.name, ')') AS tour_info, " +
                 "t.price_per_day, t.days " +
                 "FROM public.tours t " +
                 "JOIN public.locations l ON t.location_id = l.id " +
                 "JOIN public.tour_types tt ON t.tour_type_id = tt.id " +
                 "ORDER BY t.name")) {
            
            while (rs.next()) {
                String tourInfo = rs.getString("tour_info");
                int id = rs.getInt("id");
                tourIds.put(tourInfo, id);
                tourCombo.addItem(tourInfo);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, 
                "Ошибка при загрузке списка туров: " + ex.getMessage(), 
                "Ошибка", 
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadTourServices() {
        String selectedTour = (String) tourCombo.getSelectedItem();
        if (selectedTour != null) {
            currentTourId = tourIds.get(selectedTour);
            servicesModel.clear();
            serviceIds.clear();
            
            try (Connection conn = dbManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                     "SELECT * FROM public.get_tour_services(?)")) {
                
                stmt.setInt(1, currentTourId);
                ResultSet rs = stmt.executeQuery();
                
                while (rs.next()) {
                    int serviceId = rs.getInt("service_id");
                    String serviceName = rs.getString("service_name");
                    double price = rs.getDouble("price");
                    String displayName = String.format("%s (%.2f руб.)", serviceName, price);
                    
                    serviceIds.put(displayName, serviceId);
                    servicesModel.addElement(displayName);
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, 
                    "Ошибка при загрузке сервисов тура: " + ex.getMessage(), 
                    "Ошибка", 
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void updateLocationInfo() {
        String selectedTour = (String) tourCombo.getSelectedItem();
        if (selectedTour != null) {
            try {
                int tourId = tourIds.get(selectedTour);
                Map<String, String> locationInfo = dbManager.getTourLocationInfo(tourId);
                
                String phone = locationInfo.get("phone");
                String fax = locationInfo.get("fax");
                
                phoneLabel.setText(phone != null ? phone : "-");
                faxLabel.setText(fax != null ? fax : "-");
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, 
                    "Ошибка при загрузке информации о локации: " + ex.getMessage(), 
                    "Ошибка", 
                    JOptionPane.ERROR_MESSAGE);
            }
        } else {
            phoneLabel.setText("-");
            faxLabel.setText("-");
        }
    }

    private void updatePrice() {
        String selectedTour = (String) tourCombo.getSelectedItem();
        if (selectedTour != null) {
            try {
                int tourId = tourIds.get(selectedTour);
                
                // Получаем базовую цену тура
                try (Connection conn = dbManager.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(
                         "SELECT price_per_day, days FROM public.tours WHERE id = ?")) {
                    
                    stmt.setInt(1, tourId);
                    ResultSet rs = stmt.executeQuery();
                    
                    double basePrice = 0.0;
                    if (rs.next()) {
                        double pricePerDay = rs.getDouble("price_per_day");
                        int days = rs.getInt("days");
                        basePrice = pricePerDay * days;
                    }
                    
                    // Получаем цены выбранных сервисов
                    List<Integer> selectedServiceIds = new ArrayList<>();
                    for (int i = 0; i < servicesList.getModel().getSize(); i++) {
                        if (servicesList.isSelectedIndex(i)) {
                            String serviceDisplayName = servicesList.getModel().getElementAt(i);
                            selectedServiceIds.add(serviceIds.get(serviceDisplayName));
                        }
                    }
                    
                    int[] serviceIdsArray = selectedServiceIds.stream().mapToInt(i -> i).toArray();
                    double servicesPrice = dbManager.getServicesTotalPrice(serviceIdsArray);
                    
                    double totalPrice = basePrice + servicesPrice;
                    priceLabel.setText(String.format("%.2f руб.", totalPrice));
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, 
                    "Ошибка при расчете цены: " + ex.getMessage(), 
                    "Ошибка", 
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void submitBooking() {
        String selectedClient = (String) clientCombo.getSelectedItem();
        String selectedTour = (String) tourCombo.getSelectedItem();

        if (selectedClient == null || selectedTour == null) {
            JOptionPane.showMessageDialog(this, 
                "Пожалуйста, выберите клиента и тур", 
                "Предупреждение", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            int clientId = clientIds.get(selectedClient);
            int tourId = tourIds.get(selectedTour);

            // Получаем выбранные сервисы
            List<Integer> selectedServiceIds = new ArrayList<>();
            for (int i = 0; i < servicesList.getModel().getSize(); i++) {
                if (servicesList.isSelectedIndex(i)) {
                    String serviceDisplayName = servicesList.getModel().getElementAt(i);
                    selectedServiceIds.add(serviceIds.get(serviceDisplayName));
                }
            }

            Integer[] serviceIdsArray = selectedServiceIds.toArray(new Integer[0]);

            // Используем новый метод бронирования с сервисами
            dbManager.bookTour(clientId, tourId, user.getId(), user.getRole(), serviceIdsArray);

            JOptionPane.showMessageDialog(this, 
                "Бронирование успешно создано!", 
                "Успех", 
                JOptionPane.INFORMATION_MESSAGE);

            // Сбрасываем выбор
            clientCombo.setSelectedIndex(0);
            tourCombo.setSelectedIndex(0);
            servicesList.clearSelection();
            priceLabel.setText("0.00 руб.");
            phoneLabel.setText("-");
            faxLabel.setText("-");
            
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, 
                "Ошибка при создании бронирования: " + ex.getMessage(), 
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