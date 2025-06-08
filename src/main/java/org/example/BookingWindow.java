package org.example;

import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class BookingWindow extends JPanel {
    private final DatabaseManager dbManager;
    private final User user;
    private JComboBox<String> clientCombo, tourCombo;
    private Map<String, Integer> clientIds = new HashMap<>();
    private Map<String, Integer> tourIds = new HashMap<>();
    private JLabel priceLabel;

    public BookingWindow(DatabaseManager dbManager, User user) {
        this.dbManager = dbManager;
        this.user = user;
        setLayout(new BorderLayout());

        JPanel panel = new JPanel(new GridLayout(4, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Компонент выбора клиента
        panel.add(new JLabel("Выберите клиента:"));
        clientCombo = new JComboBox<>();
        loadClients();
        panel.add(clientCombo);

        // Компонент выбора тура
        panel.add(new JLabel("Выберите тур:"));
        tourCombo = new JComboBox<>();
        loadTours();
        tourCombo.addActionListener(e -> updatePrice());
        panel.add(tourCombo);

        // Отображение цены
        panel.add(new JLabel("Итоговая цена:"));
        priceLabel = new JLabel("0.00");
        panel.add(priceLabel);

        // Кнопка подтверждения
        JButton submitButton = new JButton("Подтвердить бронирование");
        submitButton.addActionListener(e -> submitBooking());
        panel.add(submitButton);

        add(panel, BorderLayout.CENTER);
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

    private void updatePrice() {
        String selectedTour = (String) tourCombo.getSelectedItem();
        if (selectedTour != null) {
            try (Connection conn = dbManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                     "SELECT (price_per_day * days) as total_price " +
                     "FROM public.tours WHERE id = ?")) {
                
                stmt.setInt(1, tourIds.get(selectedTour));
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    double totalPrice = rs.getDouble("total_price");
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

        try (Connection conn = dbManager.getConnection()) {
            int clientId = clientIds.get(selectedClient);
            int tourId = tourIds.get(selectedTour);

            dbManager.makeBooking(conn, clientId, tourId);
            conn.commit();

            JOptionPane.showMessageDialog(this, 
                "Бронирование успешно создано!", 
                "Успех", 
                JOptionPane.INFORMATION_MESSAGE);

            // Сбрасываем выбор
            clientCombo.setSelectedIndex(0);
            tourCombo.setSelectedIndex(0);
            
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, 
                "Ошибка при создании бронирования: " + ex.getMessage(), 
                "Ошибка", 
                JOptionPane.ERROR_MESSAGE);
        }
    }
}