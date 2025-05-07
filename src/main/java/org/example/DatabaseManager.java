package org.example;

import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.util.Vector;

public class DatabaseManager {
    private static final String URL = "jdbc:postgresql://172.20.7.54:5432/tyr";
    private static final String USER = "st2092";
    private static final String PASSWORD = "pwd_2092";

    public Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
        conn.setAutoCommit(false);
        return conn;
    }

    public User authenticateUser(String username, String password) throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT id, username, password_hash, role FROM public.users WHERE username = ?")) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String storedHash = rs.getString("password_hash");
                if (BCrypt.checkpw(password, storedHash)) {
                    User user = new User(rs.getInt("id"), rs.getString("username"), rs.getString("role"));
                    conn.commit();
                    return user;
                }
            }
            conn.commit();
            return null;
        }
    }

    public void registerUser(Connection conn, String username, String password, String role) throws SQLException {
        String passwordHash = BCrypt.hashpw(password, BCrypt.gensalt());
        try (PreparedStatement stmt = conn.prepareStatement("INSERT INTO public.users (username, password_hash, role) VALUES (?, ?, ?)")) {
            stmt.setString(1, username);
            stmt.setString(2, passwordHash);
            stmt.setString(3, role);
            stmt.executeUpdate();
        }
    }

    public void makeBooking(Connection conn, int clientId, int tourId) throws SQLException {
        try (CallableStatement cstmt = conn.prepareCall("CALL public.make_booking(?, ?)")) {
            cstmt.setInt(1, clientId);
            cstmt.setInt(2, tourId);
            cstmt.execute();
        }
    }

    public ResultSet executeQuery(String query) throws SQLException {
        Connection conn = getConnection();
        Statement stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
        ResultSet rs = stmt.executeQuery(query);
        conn.commit();
        return rs;
    }

    public Vector<Vector<Object>> getTableData(String query) throws SQLException {
        Vector<Vector<Object>> data = new Vector<>();
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
             ResultSet rs = stmt.executeQuery(query)) {
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                for (int i = 1; i <= columnCount; i++) {
                    row.add(rs.getObject(i));
                }
                data.add(row);
            }
        }
        return data;
    }

    public Vector<String> getColumnNames(String query) throws SQLException {
        Vector<String> columnNames = new Vector<>();
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
             ResultSet rs = stmt.executeQuery(query)) {
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            for (int i = 1; i <= columnCount; i++) {
                columnNames.add(metaData.getColumnName(i));
            }
        }
        return columnNames;
    }
}