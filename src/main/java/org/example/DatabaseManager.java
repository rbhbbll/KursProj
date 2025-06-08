package org.example;

import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.util.Vector;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
    private static final String URL = "jdbc:postgresql://172.20.7.54:5432/tyr";
    private static final String USER = "st2092";
    private static final String PASSWORD = "pwd_2092";
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

    public void makeBooking(Connection conn, int clientId, int tourId) throws SQLException {
        try (CallableStatement cstmt = conn.prepareCall("CALL public.make_booking(?, ?)")) {
            cstmt.setInt(1, clientId);
            cstmt.setInt(2, tourId);
            cstmt.execute();
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