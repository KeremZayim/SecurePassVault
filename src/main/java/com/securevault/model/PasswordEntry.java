package com.securevault.model;

public class PasswordEntry {
    private int id;
    private String title;
    private String username;
    private String password; // Şifrelenmiş
    private String notes;

    public PasswordEntry(int id, String title, String username, String password, String notes) {
        this.id = id;
        this.title = title;
        this.username = username;
        this.password = password;
        this.notes = notes;
    }

    // Getters ve Setters
    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getNotes() { return notes; }

    public void setId(int id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setUsername(String username) { this.username = username; }
    public void setPassword(String password) { this.password = password; }
    public void setNotes(String notes) { this.notes = notes; }
}
