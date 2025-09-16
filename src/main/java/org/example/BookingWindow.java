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

        // Количество строк зависит от роли пользователя
        int rows = "admin".equals(user.getRole()) ? 4 : 3;
        JPanel panel = new JPanel(new GridLayout(rows, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Компонент выбора клиента (только для администраторов)
        if ("admin".equals(user.getRole())) {
            panel.add(new JLabel("Выберите клиента:"));
            clientCombo = new JComboBox<>();
            loadClients();
            panel.add(clientCombo);
        } else {
            // Для клиентов скрываем выбор клиента, но все равно загружаем данные
            clientCombo = new JComboBox<>();
            loadClients();
        }

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
        try (Connection conn = dbManager.getConnection()) {
            clientCombo.removeAllItems();
            clientIds.clear();
            
            if ("admin".equals(user.getRole())) {
                // Администратор видит всех клиентов
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT id, full_name FROM public.clients ORDER BY full_name")) {
                    
                    while (rs.next()) {
                        int id = rs.getInt("id");
                        String name = rs.getString("full_name");
                        clientCombo.addItem(name);
                        clientIds.put(name, id);
                    }
                }
            } else {
                // Клиент видит только себя
                System.out.println("DEBUG: Ищем клиента для пользователя с ID " + user.getId() + " и ролью " + user.getRole());
                int clientId = dbManager.getClientIdByUserId(user.getId());
                if (clientId != -1) {
                    try (PreparedStatement stmt = conn.prepareStatement(
                            "SELECT id, full_name FROM public.clients WHERE id = ?")) {
                        stmt.setInt(1, clientId);
                        ResultSet rs = stmt.executeQuery();
                        
                        if (rs.next()) {
                            String name = rs.getString("full_name");
                            clientCombo.addItem(name);
                            clientIds.put(name, clientId);
                            System.out.println("DEBUG: Клиент " + name + " успешно загружен");
                        }
                    }
                } else {
                    System.out.println("DEBUG: Клиент не найден для пользователя " + user.getId());
                    
                    // Временное решение: создаем клиента автоматически
                    try {
                        int newClientId = createClientForUser(user.getId(), user.getUsername());
                        if (newClientId != -1) {
                            clientCombo.addItem(user.getUsername());
                            clientIds.put(user.getUsername(), newClientId);
                            System.out.println("DEBUG: Создан новый клиент с ID " + newClientId);
                        } else {
                            JOptionPane.showMessageDialog(this, 
                                "Клиентская запись не найдена и не может быть создана.\n\n" +
                                "Отладочная информация:\n" +
                                "ID пользователя: " + user.getId() + "\n" +
                                "Имя пользователя: " + user.getUsername() + "\n" +
                                "Роль: " + user.getRole() + "\n\n" +
                                "Обратитесь к администратору для решения проблемы.", 
                                "Ошибка", 
                                JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (SQLException ex) {
                        JOptionPane.showMessageDialog(this, 
                            "Ошибка при создании клиентской записи: " + ex.getMessage(), 
                            "Ошибка", 
                            JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, 
                "Ошибка при загрузке клиентов: " + ex.getMessage(), 
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

        // Проверяем выбор в зависимости от роли
        if ("admin".equals(user.getRole())) {
            if (selectedClient == null || selectedTour == null) {
                JOptionPane.showMessageDialog(this, 
                    "Пожалуйста, выберите клиента и тур", 
                    "Предупреждение", 
                    JOptionPane.WARNING_MESSAGE);
                return;
            }
        } else {
            if (selectedTour == null) {
                JOptionPane.showMessageDialog(this, 
                    "Пожалуйста, выберите тур", 
                    "Предупреждение", 
                    JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        try (Connection conn = dbManager.getConnection()) {
            int clientId;
            int tourId = tourIds.get(selectedTour);
            
            if ("admin".equals(user.getRole())) {
                clientId = clientIds.get(selectedClient);
            } else {
                // Для клиентов получаем их собственный ID
                clientId = dbManager.getClientIdByUserId(user.getId());
            }

            // Используем новый метод бронирования с проверкой прав
            try {
                dbManager.makeBooking(clientId, tourId, user.getId(), user.getRole());
            } catch (SQLException e) {
                if (e.getMessage().contains("Недостаточно прав") || e.getMessage().contains("can_user_book_for_client")) {
                    JOptionPane.showMessageDialog(this, 
                        "У вас нет прав для этой операции! Клиенты могут бронировать туры только для себя.", 
                        "Ошибка доступа", 
                        JOptionPane.ERROR_MESSAGE);
                    return;
                } else {
                    throw e;
                }
            }

            JOptionPane.showMessageDialog(this, 
                "Бронирование успешно создано!", 
                "Успех", 
                JOptionPane.INFORMATION_MESSAGE);

            // Сбрасываем выбор
            if ("admin".equals(user.getRole())) {
                clientCombo.setSelectedIndex(0);
            }
            tourCombo.setSelectedIndex(0);
            
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, 
                "Ошибка при создании бронирования: " + ex.getMessage(), 
                "Ошибка", 
                JOptionPane.ERROR_MESSAGE);
        }
    }

    // Временный метод для создания клиента для пользователя
    private int createClientForUser(int userId, String username) throws SQLException {
        try (Connection conn = dbManager.getConnection()) {
            // Создаем клиента с минимальными данными
            String sql = "INSERT INTO public.clients (full_name, user_id) VALUES (?, ?) RETURNING id";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, username); // Используем username как имя
                stmt.setInt(2, userId);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    int newClientId = rs.getInt("id");
                    conn.commit();
                    return newClientId;
                }
            }
        }
        return -1;
    }
}