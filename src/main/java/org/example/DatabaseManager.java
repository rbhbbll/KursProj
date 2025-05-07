package org.example;

import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;

public class DatabaseManager {
    private static final String URL = "jdbc:postgresql://172.20.7.54:5432/tyr";
    private static final String USER = "st2092";
    private static final String PASSWORD = "pwd_2092";

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public User authenticateUser(String username, String password) throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT id, username, password_hash, role FROM public.users WHERE username = ?")) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String storedHash = rs.getString("password_hash");
                if (BCrypt.checkpw(password, storedHash)) {
                    return new User(rs.getInt("id"), rs.getString("username"), rs.getString("role"));
                }
            }
            return null;
        }
    }

    public void registerUser(String username, String password, String role) throws SQLException {
        String passwordHash = BCrypt.hashpw(password, BCrypt.gensalt());
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("INSERT INTO public.users (username, password_hash, role) VALUES (?, ?, ?)")) {
            stmt.setString(1, username);
            stmt.setString(2, passwordHash);
            stmt.setString(3, role);
            stmt.executeUpdate();
        }
    }

    public void makeBooking(int clientId, int tourId) throws SQLException {
        try (Connection conn = getConnection();
             CallableStatement cstmt = conn.prepareCall("{CALL public.make_booking(?, ?)}")) {
            cstmt.setInt(1, clientId);
            cstmt.setInt(2, tourId);
            cstmt.execute();
        }
    }

    public ResultSet executeQuery(String query) throws SQLException {
        Connection conn = getConnection();
        Statement stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
        return stmt.executeQuery(query);
    }
}