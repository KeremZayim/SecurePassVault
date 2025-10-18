package com.securevault.service;

import com.securevault.model.PasswordEntry;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.Base64;
import java.util.ArrayList;
import java.util.List;

public class DatabaseService {

    private static final String DB_URL = "jdbc:sqlite:securevault.db";

    public DatabaseService() {
        createUserTable();
        createPasswordTable();
        addDefaultUser();
    }

    private void createUserTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT UNIQUE NOT NULL,
                master_password_hash TEXT NOT NULL,
                salt TEXT NOT NULL
            );
        """;
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void createPasswordTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS passwords (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER NOT NULL,
                title TEXT NOT NULL,
                username TEXT,
                password TEXT NOT NULL,
                notes TEXT,
                FOREIGN KEY(user_id) REFERENCES users(id)
            );
        """;
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void addDefaultUser() {
        try {
            if (getUserId("admin") == null) {
                String salt = generateSalt();
                String hash = hashPassword("1234", salt);
                String sql = "INSERT INTO users(username, master_password_hash, salt) VALUES(?,?,?)";
                try (Connection conn = DriverManager.getConnection(DB_URL);
                     PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, "admin");
                    pstmt.setString(2, hash);
                    pstmt.setString(3, salt);
                    pstmt.executeUpdate();
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private String generateSalt() {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    private String hashPassword(String password, String salt) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(Base64.getDecoder().decode(salt));
        byte[] hashed = md.digest(password.getBytes());
        StringBuilder sb = new StringBuilder();
        for (byte b : hashed) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private Integer getUserId(String username) {
        String sql = "SELECT id FROM users WHERE username = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getInt("id");
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    public Integer validateUserAndGetId(String username, String password) {
        String sql = "SELECT id, master_password_hash, salt FROM users WHERE username = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                int userId = rs.getInt("id");
                String storedHash = rs.getString("master_password_hash");
                String salt = rs.getString("salt");
                if (storedHash.equals(hashPassword(password, salt))) return userId;
            }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    // --- PASSWORD CRUD ---
    public void addPassword(PasswordEntry entry, int userId) {
        String sql = "INSERT INTO passwords(user_id, title, username, password, notes) VALUES(?,?,?,?,?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setString(2, entry.getTitle());
            pstmt.setString(3, entry.getUsername());
            pstmt.setString(4, entry.getPassword());
            pstmt.setString(5, entry.getNotes());
            pstmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public List<PasswordEntry> getPasswordsForUser(int userId) {
        List<PasswordEntry> list = new ArrayList<>();
        String sql = "SELECT * FROM passwords WHERE user_id=?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                list.add(new PasswordEntry(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("notes")
                ));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public void deletePassword(int id, int userId) {
        String sql = "DELETE FROM passwords WHERE id=? AND user_id=?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.setInt(2, userId);
            pstmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void updatePassword(PasswordEntry entry, int userId) {
        String sql = "UPDATE passwords SET title=?, username=?, password=?, notes=? WHERE id=? AND user_id=?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, entry.getTitle());
            pstmt.setString(2, entry.getUsername());
            pstmt.setString(3, entry.getPassword());
            pstmt.setString(4, entry.getNotes());
            pstmt.setInt(5, entry.getId());
            pstmt.setInt(6, userId);
            pstmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }
}
