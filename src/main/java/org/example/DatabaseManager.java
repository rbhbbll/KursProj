package org.example;

import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.util.Vector;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.math.BigDecimal;

public class DatabaseManager {
    private static final String URL = "jdbc:postgresql://10.5.114.149:5432/kirill123";
    private static final String USER = "alex";
    private static final String PASSWORD = "1234";
    private String currentUserRole;

    public Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
        conn.setAutoCommit(false);
        return conn;
    }

    public void setCurrentUserRole(String role) {
        this.currentUserRole = role;
    }

    public String getCurrentUserRole() {
        return currentUserRole;
    }

    public User authenticateUser(String username, String password) throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT id, username, password_hash, role FROM public.users WHERE username = ?")) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                String storedHash = rs.getString("password_hash");
                if (BCrypt.checkpw(password, storedHash)) {
                    int id = rs.getInt("id");
                    String role = rs.getString("role");
                    setCurrentUserRole(role);
                    return new User(id, username, role);
                }
            }
            return null;
        }
    }

    public void registerUser(Connection conn, String username, String password, String role) throws SQLException {
        String passwordHash = BCrypt.hashpw(password, BCrypt.gensalt());
        try (PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO public.users (username, password_hash, role) VALUES (?, ?, ?)")) {
            stmt.setString(1, username);
            stmt.setString(2, passwordHash);
            stmt.setString(3, role);
            stmt.executeUpdate();
        }
    }

    // Регистрация клиента с привязкой к пользователю
    public void registerClientWithUser(Connection conn, String username, String password, String fullName, 
                                     String phone, String email, String passport, java.sql.Date birthDate) throws SQLException {
        // Сначала регистрируем пользователя
        registerUser(conn, username, password, "client");
        
        // Получаем ID только что созданного пользователя
        int userId;
        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT id FROM public.users WHERE username = ?")) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                userId = rs.getInt("id");
            } else {
                throw new SQLException("Не удалось найти созданного пользователя");
            }
        }
        
        // Регистрируем клиента через обновленную процедуру
        registerClient(fullName, phone, email, passport, birthDate, userId);
    }

    // Обновленный метод регистрации клиента с использованием новой процедуры
    public void registerClient(String fullName, String phone, String email, String passport, java.sql.Date birthDate, int userId) throws SQLException {
        String sql = "CALL public.register_new_client(?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             CallableStatement stmt = conn.prepareCall(sql)) {
            stmt.setString(1, fullName);
            stmt.setString(2, phone);
            stmt.setString(3, email);
            stmt.setString(4, passport);
            stmt.setDate(5, birthDate);
            stmt.setInt(6, userId);
            stmt.execute();
        }
    }

    public void makeBooking(Connection conn, int clientId, int tourId) throws SQLException {
        try (CallableStatement cstmt = conn.prepareCall("CALL public.make_booking(?, ?)")) {
            cstmt.setInt(1, clientId);
            cstmt.setInt(2, tourId);
            cstmt.execute();
        }
    }

    // Обновленный метод бронирования с проверкой прав
    public void makeBooking(int clientId, int tourId, int userId, String userRole) throws SQLException {
        String sql = "CALL public.make_booking(?, ?, ?, ?)";
        try (Connection conn = getConnection();
             CallableStatement stmt = conn.prepareCall(sql)) {
            stmt.setInt(1, clientId);
            stmt.setInt(2, tourId);
            stmt.setInt(3, userId);
            stmt.setString(4, userRole);
            stmt.execute();
        }
    }

    // Получить ID клиента по ID пользователя
    public int getClientIdByUserId(int userId) throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT id FROM public.clients WHERE user_id = ?")) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                int clientId = rs.getInt("id");
                System.out.println("DEBUG: Найден клиент с ID " + clientId + " для пользователя " + userId);
                return clientId;
            }
            System.out.println("DEBUG: Клиент не найден для пользователя " + userId);
            return -1; // Клиент не найден
        }
    }

    public ResultSet executeQuery(String query) throws SQLException {
        Connection conn = getConnection();
        Statement stmt = conn.createStatement();
        return stmt.executeQuery(query);
    }

    public Vector<Vector<Object>> getTableData(String query) throws SQLException {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            Vector<Vector<Object>> data = new Vector<>();
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            
            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                for (int i = 1; i <= columnCount; i++) {
                    row.add(rs.getObject(i));
                }
                data.add(row);
            }
            return data;
        }
    }

    public Vector<String> getColumnNames(String query) throws SQLException {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            Vector<String> columnNames = new Vector<>();
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            
            for (int i = 1; i <= columnCount; i++) {
                columnNames.add(metaData.getColumnName(i));
            }
            return columnNames;
        }
    }

    public List<String> getTableColumns(String tableName) throws SQLException {
        List<String> columns = new ArrayList<>();
        try (Connection conn = getConnection();
             ResultSet rs = conn.getMetaData().getColumns(null, "public", tableName, null)) {
            while (rs.next()) {
                columns.add(rs.getString("COLUMN_NAME"));
            }
        }
        return columns;
    }

    public void executeUpdate(String query, Object... params) throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            stmt.executeUpdate();
            conn.commit();
        }
    }

    public void callProcedure(String procedureName, Object... params) throws SQLException {
        try (Connection conn = getConnection()) {
            String sql = "{CALL " + procedureName + "(" + String.join(",", java.util.Collections.nCopies(params.length, "?")) + ")}";
            try (CallableStatement stmt = conn.prepareCall(sql)) {
                for (int i = 0; i < params.length; i++) {
                    stmt.setObject(i + 1, params[i]);
                }
                stmt.execute();
                conn.commit();
            }
        }
    }

    public boolean deleteClient(int clientId) throws SQLException {
        try (Connection conn = getConnection()) {
            // First check if client has any bookings
            try (PreparedStatement checkStmt = conn.prepareStatement(
                "SELECT COUNT(*) FROM public.bookings WHERE client_id = ?"
            )) {
                checkStmt.setInt(1, clientId);
                ResultSet rs = checkStmt.executeQuery();
                rs.next();
                if (rs.getInt(1) > 0) {
                    return false;
                }
            }
            
            // If no bookings, proceed with deletion
            try (CallableStatement stmt = conn.prepareCall("CALL public.delete_client_with_log(?)")) {
                stmt.setInt(1, clientId);
                stmt.execute();
                conn.commit();
                return true;
            }
        }
    }

    // Удаление тура (только для администраторов)
    public boolean deleteTour(int tourId, int userId, String userRole) throws SQLException {
        try (Connection conn = getConnection()) {
            String sql = "CALL public.delete_tour(?, ?, ?)";
            try (CallableStatement stmt = conn.prepareCall(sql)) {
                stmt.setInt(1, tourId);
                stmt.setInt(2, userId);
                stmt.setString(3, userRole);
                stmt.execute();
                conn.commit();
                return true;
            }
        } catch (SQLException ex) {
            if (ex.getMessage().contains("Недостаточно прав") || ex.getMessage().contains("не найден")) {
                throw new SQLException("Ошибка удаления тура: " + ex.getMessage());
            }
            throw ex;
        }
    }

    // Получить все доступные сервисы
    public ResultSet getAllServices() throws SQLException {
        try (Connection conn = getConnection()) {
            String sql = "SELECT id, name, price FROM public.services ORDER BY name";
            return conn.createStatement().executeQuery(sql);
        }
    }

    // Получить сервисы конкретного тура
    public ResultSet getTourServices(int tourId) throws SQLException {
        try (Connection conn = getConnection()) {
            String sql = "SELECT * FROM public.get_tour_services(?)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, tourId);
            return stmt.executeQuery();
        }
    }

    // Получить сервисы тура как List<Map> для удобства использования в UI
    public List<Map<String, Object>> getTourServicesAsList(int tourId) throws SQLException {
        List<Map<String, Object>> services = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM public.get_tour_services(?)")) {
            stmt.setInt(1, tourId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> service = new HashMap<>();
                    service.put("service_id", rs.getInt("service_id"));
                    service.put("service_name", rs.getString("service_name"));
                    service.put("price", rs.getDouble("price"));
                    services.add(service);
                }
            }
        }
        return services;
    }

    // Получить все достопримечательности
    public ResultSet getAllAttractions() throws SQLException {
        try (Connection conn = getConnection()) {
            String sql = "SELECT id, name FROM public.attractions ORDER BY name";
            return conn.createStatement().executeQuery(sql);
        }
    }

    // Получить информацию о локации тура (phone, fax)
    public Map<String, String> getTourLocationInfo(int tourId) throws SQLException {
        Map<String, String> locationInfo = new HashMap<>();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT l.phone, l.fax FROM public.locations l " +
                 "JOIN public.tours t ON l.id = t.location_id " +
                 "WHERE t.id = ?")) {
            stmt.setInt(1, tourId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    locationInfo.put("phone", rs.getString("phone"));
                    locationInfo.put("fax", rs.getString("fax"));
                }
            }
        }
        return locationInfo;
    }

    // Обновленный метод добавления тура с поддержкой сервисов и достопримечательностей
    public void addTour(String name, int tourTypeId, int locationId, double pricePerDay, int days, 
                       Integer[] serviceIds, Integer[] attractionIds) throws SQLException {
        try (Connection conn = getConnection()) {
            // Сначала удаляем старую процедуру и создаем новую
            try (Statement dropStmt = conn.createStatement()) {
                dropStmt.execute("DROP PROCEDURE IF EXISTS public.add_tour_full(character varying, integer, integer, double precision, integer, integer[], integer[])");
            }
            
            try (Statement createStmt = conn.createStatement()) {
                createStmt.execute("CREATE OR REPLACE PROCEDURE public.add_tour_full(IN p_name VARCHAR, IN p_tour_type_id INTEGER, IN p_location_id INTEGER, IN p_price NUMERIC, IN p_days INTEGER, IN p_service_ids INTEGER[], IN p_attraction_ids INTEGER[]) " +
                    "LANGUAGE plpgsql " +
                    "AS $$ " +
                    "DECLARE v_tour_id INT; " +
                    "BEGIN " +
                    "INSERT INTO public.tours (name, tour_type_id, location_id, price, days) " +
                    "VALUES (p_name, p_tour_type_id, p_location_id, p_price, p_days) " +
                    "RETURNING id INTO v_tour_id; " +
                    "IF p_service_ids IS NOT NULL THEN " +
                    "INSERT INTO public.tour_services (tour_id, service_id) " +
                    "SELECT v_tour_id, UNNEST(p_service_ids); " +
                    "END IF; " +
                    "IF p_attraction_ids IS NOT NULL THEN " +
                    "INSERT INTO public.tour_attractions (tour_id, attraction_id) " +
                    "SELECT v_tour_id, UNNEST(p_attraction_ids); " +
                    "END IF; " +
                    "END; $$");
            }
            
            String sql = "CALL public.add_tour_full(?, ?, ?, ?, ?, ?, ?)";
            try (CallableStatement stmt = conn.prepareCall(sql)) {
                stmt.setString(1, name);
                stmt.setInt(2, tourTypeId);
                stmt.setInt(3, locationId);
                stmt.setBigDecimal(4, new BigDecimal(String.valueOf(pricePerDay)));
                stmt.setInt(5, days);
                
                Array servicesArray = serviceIds != null && serviceIds.length > 0 ? 
                    conn.createArrayOf("integer", serviceIds) : null;
                stmt.setArray(6, servicesArray);
                
                Array attractionsArray = attractionIds != null && attractionIds.length > 0 ? 
                    conn.createArrayOf("integer", attractionIds) : null;
                stmt.setArray(7, attractionsArray);
                
                stmt.execute();
                conn.commit();
            }
        } catch (SQLException ex) {
            if (ex.getMessage().contains("не существует") || ex.getMessage().contains("конфликт типов")) {
                throw new SQLException("Ошибка процедуры: " + ex.getMessage());
            }
            throw ex;
        }
    }

    // Обновленный метод бронирования с выбором услуг
    public void bookTour(int clientId, int tourId, int userId, String userRole, Integer[] serviceIds) throws SQLException {
        try (Connection conn = getConnection()) {
            // Сначала удаляем старую процедуру и создаем новую
            try (Statement dropStmt = conn.createStatement()) {
                dropStmt.execute("DROP PROCEDURE IF EXISTS public.make_booking(integer, integer, integer, varchar, integer[])");
            }
            
            try (Statement createStmt = conn.createStatement()) {
                createStmt.execute("CREATE OR REPLACE PROCEDURE public.make_booking(IN p_client_id INTEGER, IN p_tour_id INTEGER, IN p_user_id INTEGER, IN p_user_role VARCHAR, IN p_service_ids INTEGER[] DEFAULT NULL) " +
                    "LANGUAGE plpgsql " +
                    "AS $$ " +
                    "DECLARE v_client_id INTEGER; " +
                    "BEGIN " +
                    "IF NOT public.can_user_book_for_client(p_user_id, p_client_id, p_user_role) THEN " +
                    "RAISE EXCEPTION 'Недостаточно прав для бронирования за этого клиента'; " +
                    "END IF; " +
                    "INSERT INTO public.bookings (client_id, tour_id) VALUES (p_client_id, p_tour_id); " +
                    "IF p_service_ids IS NOT NULL THEN " +
                    "RAISE NOTICE 'Услуги % выбраны для бронирования', p_service_ids; " +
                    "END IF; " +
                    "END; $$");
            }
            
            String sql = "CALL public.make_booking(?, ?, ?, ?, ?)";
            try (CallableStatement stmt = conn.prepareCall(sql)) {
                stmt.setInt(1, clientId);
                stmt.setInt(2, tourId);
                stmt.setInt(3, userId);
                stmt.setString(4, userRole);
                
                Array servicesArray = serviceIds != null && serviceIds.length > 0 ? 
                    conn.createArrayOf("integer", serviceIds) : null;
                stmt.setArray(5, servicesArray);
                
                stmt.execute();
                conn.commit();
            }
        } catch (SQLException ex) {
            if (ex.getMessage().contains("не существует") || ex.getMessage().contains("конфликт типов")) {
                throw new SQLException("Ошибка процедуры бронирования: " + ex.getMessage());
            }
            throw ex;
        }
    }

    // Получить туры с информацией об услугах
    public ResultSet getToursWithServices() throws SQLException {
        try (Connection conn = getConnection()) {
            String sql = "SELECT t.id, t.name, tt.name AS type, l.city AS location, l.country, " +
                        "t.price_per_day, t.days, " +
                        "ARRAY_TO_STRING((SELECT ARRAY_AGG(s.service_name) FROM get_tour_services(t.id) s), ', ') AS services " +
                        "FROM tours t " +
                        "JOIN tour_types tt ON t.tour_type_id = tt.id " +
                        "JOIN locations l ON t.location_id = l.id";
            return conn.createStatement().executeQuery(sql);
        }
    }

    // Получить информацию о туре для расчета цены с услугами
    public ResultSet getTourPriceInfo(int tourId) throws SQLException {
        try (Connection conn = getConnection()) {
            String sql = "SELECT price_per_day, days FROM public.tours WHERE id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, tourId);
            return stmt.executeQuery();
        }
    }

    // Получить цены выбранных сервисов
    public double getServicesTotalPrice(int[] serviceIds) throws SQLException {
        if (serviceIds == null || serviceIds.length == 0) {
            return 0.0;
        }
        
        try (Connection conn = getConnection()) {
            StringBuilder sql = new StringBuilder("SELECT SUM(price) FROM public.services WHERE id IN (");
            for (int i = 0; i < serviceIds.length; i++) {
                if (i > 0) sql.append(",");
                sql.append("?");
            }
            sql.append(")");
            
            PreparedStatement stmt = conn.prepareStatement(sql.toString());
            for (int i = 0; i < serviceIds.length; i++) {
                stmt.setInt(i + 1, serviceIds[i]);
            }
            
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getDouble(1);
            }
            return 0.0;
        }
    }
}