package com.securevault.service;

import com.securevault.model.PasswordEntry;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class DatabaseService {

    // 🔹 Tek bir DB kullanıyoruz
    private static final String DB_URL = "jdbc:sqlite:securevault.db";

    public DatabaseService() {
        createPasswordTable();
        createUserTable();
    }

    // --- PASSWORD TABLOSU ---
    private void createPasswordTable() {
        String sql = """
                CREATE TABLE IF NOT EXISTS passwords (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    title TEXT NOT NULL,
                    username TEXT,
                    password TEXT NOT NULL,
                    notes TEXT
                );
                """;
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // --- USERS TABLOSU ---
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
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // --- USER LOGIN DOĞRULAMA ---
    public boolean validateUser(String username, String password) {
        String sql = "SELECT master_password_hash, salt FROM users WHERE username = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String storedHash = rs.getString("master_password_hash");
                String salt = rs.getString("salt");

                String computedHash = hashPassword(password, salt);

                return storedHash.equals(computedHash);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    // --- Şifreleme (SHA-256 + salt) ---
    private String hashPassword(String password, String salt) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(Base64.getDecoder().decode(salt));
        byte[] hashed = md.digest(password.getBytes());
        StringBuilder sb = new StringBuilder();
        for (byte b : hashed) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    // --- PASSWORD CRUD ---
    public void addPassword(PasswordEntry entry) {
        String sql = "INSERT INTO passwords(title, username, password, notes) VALUES(?,?,?,?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, entry.getTitle());
            pstmt.setString(2, entry.getUsername());
            pstmt.setString(3, entry.getPassword());
            pstmt.setString(4, entry.getNotes());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<PasswordEntry> getAllPasswords() {
        List<PasswordEntry> list = new ArrayList<>();
        String sql = "SELECT * FROM passwords";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                PasswordEntry entry = new PasswordEntry(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("notes")
                );
                list.add(entry);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public void deletePassword(int id) {
        String sql = "DELETE FROM passwords WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updatePassword(PasswordEntry entry) {
        String sql = "UPDATE passwords SET title=?, username=?, password=?, notes=? WHERE id=?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, entry.getTitle());
            pstmt.setString(2, entry.getUsername());
            pstmt.setString(3, entry.getPassword());
            pstmt.setString(4, entry.getNotes());
            pstmt.setInt(5, entry.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
