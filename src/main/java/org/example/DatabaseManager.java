package org.example;

import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.util.Vector;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
    private static final String URL = "jdbc:postgresql://10.5.114.135:5432/kirill123";
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
}