package org.example;

import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
    private static final String URL = "jdbc:postgresql://172.20.7.54:5432/tyr";
    private static final String USER = "st2092";
    private static final String PASSWORD = "pwd_2092";

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public User authenticateUser(String username, String password) throws SQLException {
        String query = "SELECT id, username, password_hash, role FROM public.users WHERE username = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String storedHash = rs.getString("password_hash");
                try {
                    if (BCrypt.checkpw(password, storedHash)) {
                        return new User(rs.getInt("id"), rs.getString("username"), rs.getString("role"));
                    }
                } catch (IllegalArgumentException e) {
                    throw new SQLException("Invalid password hash format: " + e.getMessage());
                }
            }
            return null;
        } catch (SQLException e) {
            throw new SQLException("Authentication error: " + e.getMessage(), e);
        }
    }

    public List<String> getTableColumns(String tableName) {
        List<String> columns = new ArrayList<>();
        try (Connection conn = getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            ResultSet rs = meta.getColumns(null, null, tableName, null);
            while (rs.next()) {
                columns.add(rs.getString("COLUMN_NAME"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return columns;
    }

    public ResultSet executeQuery(String query) throws SQLException {
        Connection conn = getConnection();
        Statement stmt = conn.createStatement();
        return stmt.executeQuery(query);
    }

    public void makeBooking(int clientId, int tourId) throws SQLException {
        String call = "{CALL public.make_booking(?, ?)}";
        try (Connection conn = getConnection();
             CallableStatement cstmt = conn.prepareCall(call)) {
            cstmt.setInt(1, clientId);
            cstmt.setInt(2, tourId);
            cstmt.execute();
        }
    }

    public void addTourFull(String name, int tourTypeId, int locationId, double price, int days,
                            int[] serviceIds, int[] attractionIds) throws SQLException {
        String call = "{CALL public.add_tour_full(?, ?, ?, ?, ?, ?, ?)}";
        try (Connection conn = getConnection();
             CallableStatement cstmt = conn.prepareCall(call)) {
            cstmt.setString(1, name);
            cstmt.setInt(2, tourTypeId);
            cstmt.setInt(3, locationId);
            cstmt.setDouble(4, price);
            cstmt.setInt(5, days);
            cstmt.setArray(6, conn.createArrayOf("INTEGER", toObjectArray(serviceIds)));
            cstmt.setArray(7, conn.createArrayOf("INTEGER", toObjectArray(attractionIds)));
            cstmt.execute();
        }
    }

    public void deleteClient(int clientId) throws SQLException {
        String call = "{CALL public.delete_client_with_log(?)}";
        try (Connection conn = getConnection();
             CallableStatement cstmt = conn.prepareCall(call)) {
            cstmt.setInt(1, clientId);
            cstmt.execute();
        }
    }

    public ResultSet getToursByService(String serviceName) throws SQLException {
        String query = "SELECT * FROM public.get_available_tours_by_service(?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, serviceName);
            return pstmt.executeQuery();
        }
    }

    public ResultSet getToursByPriceRange(double minPrice, double maxPrice) throws SQLException {
        String query = "SELECT * FROM public.get_tours_by_price_range(?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setDouble(1, minPrice);
            pstmt.setDouble(2, maxPrice);
            return pstmt.executeQuery();
        }
    }

    private Object[] toObjectArray(int[] arr) {
        Object[] result = new Object[arr.length];
        for (int i = 0; i < arr.length; i++) {
            result[i] = arr[i];
        }
        return result;
    }
}