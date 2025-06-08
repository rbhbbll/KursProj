package org.example;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.Vector;

public class ViewDataPanel extends JPanel {
    private final DatabaseManager dbManager;
    private final User user;

    public ViewDataPanel(DatabaseManager dbManager, User user) {
        this.dbManager = dbManager;
        this.user = user;
        setLayout(new BorderLayout());

        if ("admin".equals(user.getRole())) {
            // Для администратора показываем все таблицы
            JTabbedPane tabbedPane = new JTabbedPane();
            tabbedPane.addTab("Пользователи", createTablePanel("SELECT * FROM public.users", "users"));
            tabbedPane.addTab("Клиенты", createTablePanel("SELECT * FROM public.clients", "clients"));
            
            // Модифицированный запрос для отображения туров с названиями типов и локаций
            String toursQuery = "SELECT t.id, t.name, " +
                              "tt.name AS tour_type, " +
                              "CONCAT(l.country, ', ', l.city, ' - ', l.place_name) AS location, " +
                              "t.price_per_day, t.days, t.price " +
                              "FROM public.tours t " +
                              "JOIN public.tour_types tt ON t.tour_type_id = tt.id " +
                              "JOIN public.locations l ON t.location_id = l.id";
            
            tabbedPane.addTab("Туры", createTablePanel(toursQuery, "tours"));

            // Модифицированный запрос для отображения бронирований с именами клиентов и названиями туров
            String bookingsQuery = "SELECT b.id, " +
                                 "c.full_name AS client_name, " +
                                 "t.name AS tour_name, " +
                                 "b.booking_date, " +
                                 "b.total_price " +
                                 "FROM public.bookings b " +
                                 "JOIN public.clients c ON b.client_id = c.id " +
                                 "JOIN public.tours t ON b.tour_id = t.id";
            
            tabbedPane.addTab("Бронирования", createTablePanel(bookingsQuery, "bookings"));
            add(tabbedPane, BorderLayout.CENTER);
        } else {
            // Для клиентов показываем только информацию о турах
            String toursQuery = "SELECT t.name AS название_тура, " +
                              "tt.name AS тип_тура, " +
                              "l.country AS страна, " +
                              "l.city AS город, " +
                              "l.place_name AS место, " +
                              "t.price_per_day AS цена_за_день, " +
                              "t.days AS дней, " +
                              "(t.price_per_day * t.days) AS полная_стоимость " +
                              "FROM public.tours t " +
                              "JOIN public.tour_types tt ON t.tour_type_id = tt.id " +
                              "JOIN public.locations l ON t.location_id = l.id";
            add(createTablePanel(toursQuery, null), BorderLayout.CENTER);
        }
    }

    private JPanel createTablePanel(String query, String tableName) {
        JPanel panel = new JPanel(new BorderLayout());
        try {
            Vector<Vector<Object>> data = dbManager.getTableData(query);
            Vector<String> columnNames = dbManager.getColumnNames(query);

            // Переименовываем заголовки столбцов для таблицы туров
            if ("tours".equals(tableName)) {
                for (int i = 0; i < columnNames.size(); i++) {
                    switch (columnNames.get(i)) {
                        case "id":
                            columnNames.set(i, "ID");
                            break;
                        case "name":
                            columnNames.set(i, "Название");
                            break;
                        case "tour_type":
                            columnNames.set(i, "Тип тура");
                            break;
                        case "location":
                            columnNames.set(i, "Локация");
                            break;
                        case "price_per_day":
                            columnNames.set(i, "Цена за день");
                            break;
                        case "days":
                            columnNames.set(i, "Дни");
                            break;
                        case "price":
                            columnNames.set(i, "Общая цена");
                            break;
                    }
                }
            }
            // Переименовываем заголовки столбцов для таблицы бронирований
            else if ("bookings".equals(tableName)) {
                for (int i = 0; i < columnNames.size(); i++) {
                    switch (columnNames.get(i)) {
                        case "id":
                            columnNames.set(i, "ID бронирования");
                            break;
                        case "client_name":
                            columnNames.set(i, "Клиент");
                            break;
                        case "tour_name":
                            columnNames.set(i, "Тур");
                            break;
                        case "booking_date":
                            columnNames.set(i, "Дата бронирования");
                            break;
                        case "total_price":
                            columnNames.set(i, "Итоговая цена");
                            break;
                    }
                }
            }

            JTable table = new JTable(data, columnNames);
            table.setFillsViewportHeight(true);
            
            // Настройка внешнего вида таблицы
            table.getTableHeader().setReorderingAllowed(false);
            table.setRowHeight(25);
            table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
            
            // Добавляем поиск
            JTextField searchField = new JTextField(20);
            searchField.addActionListener(e -> {
                String searchText = searchField.getText().toLowerCase();
                if (searchText.isEmpty()) {
                    table.clearSelection();
                    return;
                }
                
                for (int row = 0; row < table.getRowCount(); row++) {
                    for (int col = 0; col < table.getColumnCount(); col++) {
                        Object value = table.getValueAt(row, col);
                        if (value != null && value.toString().toLowerCase().contains(searchText)) {
                            table.setRowSelectionInterval(row, row);
                            table.scrollRectToVisible(table.getCellRect(row, 0, true));
                            return;
                        }
                    }
                }
            });
            
            JPanel topPanel = new JPanel(new BorderLayout());
            JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            searchPanel.add(new JLabel("Поиск:"));
            searchPanel.add(searchField);
            topPanel.add(searchPanel, BorderLayout.WEST);

            // Добавляем кнопку удаления для клиентов
            if ("clients".equals(tableName)) {
                JButton deleteButton = new JButton("Удалить клиента");
                deleteButton.addActionListener(e -> {
                    int selectedRow = table.getSelectedRow();
                    if (selectedRow == -1) {
                        JOptionPane.showMessageDialog(this, 
                            "Выберите клиента для удаления", 
                            "Предупреждение", 
                            JOptionPane.WARNING_MESSAGE);
                        return;
                    }

                    int clientId = ((Number) table.getValueAt(selectedRow, 0)).intValue();
                    int confirm = JOptionPane.showConfirmDialog(this,
                        "Вы уверены, что хотите удалить этого клиента?",
                        "Подтверждение удаления",
                        JOptionPane.YES_NO_OPTION);

                    if (confirm == JOptionPane.YES_OPTION) {
                        try {
                            boolean deleted = dbManager.deleteClient(clientId);
                            if (deleted) {
                                // Refresh the table data
                                Vector<Vector<Object>> newData = dbManager.getTableData(query);
                                table.setModel(new DefaultTableModel(newData, columnNames));
                                
                                JOptionPane.showMessageDialog(this,
                                    "Клиент успешно удален",
                                    "Успех",
                                    JOptionPane.INFORMATION_MESSAGE);
                            } else {
                                JOptionPane.showMessageDialog(this,
                                    "Невозможно удалить клиента, так как у него есть бронирования",
                                    "Ошибка",
                                    JOptionPane.ERROR_MESSAGE);
                            }
                        } catch (SQLException ex) {
                            JOptionPane.showMessageDialog(this,
                                "Ошибка при удалении клиента: " + ex.getMessage(),
                                "Ошибка",
                                JOptionPane.ERROR_MESSAGE);
                        }
                    }
                });
                JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
                buttonPanel.add(deleteButton);
                topPanel.add(buttonPanel, BorderLayout.EAST);
            }

            panel.add(topPanel, BorderLayout.NORTH);
            panel.add(new JScrollPane(table), BorderLayout.CENTER);
            
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, 
                "Ошибка загрузки данных: " + ex.getMessage(), 
                "Ошибка", 
                JOptionPane.ERROR_MESSAGE);
        }
        return panel;
    }
}