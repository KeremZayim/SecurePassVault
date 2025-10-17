package com.securevault.controller;

import com.securevault.model.PasswordEntry;
import com.securevault.service.DatabaseService;
import com.securevault.service.EncryptionService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.VBox;
import java.util.List;

public class VaultController {

    @FXML
    private TableView<PasswordEntry> passwordTable;

    @FXML
    private TableColumn<PasswordEntry, String> titleColumn, usernameColumn, passwordColumn, notesColumn;

    @FXML
    private TextField searchField;

    @FXML
    private Button addButton, deleteButton, updateButton, copyButton;

    private final DatabaseService dbService = new DatabaseService();
    private final EncryptionService encryptionService;

    private final ObservableList<PasswordEntry> passwordList = FXCollections.observableArrayList();

    public VaultController() throws Exception {
        // Örnek key, gerçek uygulamada kullanıcıya özel saklanmalı
        byte[] key = EncryptionService.generateKey();
        encryptionService = new EncryptionService(key);
    }

    @FXML
    private void initialize() {
        // TableView kolonlarını ayarla
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        notesColumn.setCellValueFactory(new PropertyValueFactory<>("notes"));

        // Password Column: göster/gizle ve toggle
        passwordColumn.setCellFactory(col -> new TableCell<PasswordEntry, String>() {
            private final Button toggleBtn = new Button("Show");
            private boolean showing = false;

            {
                toggleBtn.setOnAction(e -> {
                    showing = !showing;
                    updateItem(getItem(), isEmpty());
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    PasswordEntry entry = getTableView().getItems().get(getIndex());
                    if (showing) {
                        try {
                            setText(encryptionService.decrypt(entry.getPassword()));
                        } catch (Exception ex) {
                            setText("ERROR");
                            ex.printStackTrace();
                        }
                        toggleBtn.setText("Hide");
                    } else {
                        setText("******");
                        toggleBtn.setText("Show");
                    }
                    setGraphic(toggleBtn);
                }
            }
        });

        loadPasswords();

        addButton.setOnAction(e -> handleAdd());
        deleteButton.setOnAction(e -> handleDelete());
        updateButton.setOnAction(e -> handleUpdate());
        copyButton.setOnAction(e -> handleCopy());

        // Arama / filtreleme
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.isBlank()) {
                passwordTable.setItems(passwordList);
            } else {
                ObservableList<PasswordEntry> filtered = FXCollections.observableArrayList();
                for (PasswordEntry entry : passwordList) {
                    if (entry.getTitle().toLowerCase().contains(newVal.toLowerCase()) ||
                            entry.getUsername().toLowerCase().contains(newVal.toLowerCase())) {
                        filtered.add(entry);
                    }
                }
                passwordTable.setItems(filtered);
            }
        });
    }

    private void loadPasswords() {
        passwordList.clear();
        List<PasswordEntry> entries = dbService.getAllPasswords();
        passwordList.addAll(entries);
        passwordTable.setItems(passwordList);
    }

    private void handleAdd() {
        Dialog<PasswordEntry> dialog = createPasswordDialog("Add New Password", null);
        dialog.showAndWait().ifPresent(entry -> {
            try {
                String encrypted = encryptionService.encrypt(entry.getPassword());
                PasswordEntry newEntry = new PasswordEntry(0, entry.getTitle(), entry.getUsername(), encrypted, entry.getNotes());
                dbService.addPassword(newEntry);
                loadPasswords();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }

    private void handleDelete() {
        PasswordEntry selected = passwordTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            dbService.deletePassword(selected.getId());
            loadPasswords();
        } else {
            showAlert("Select a password to delete!");
        }
    }

    private void handleUpdate() {
        PasswordEntry selected = passwordTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            Dialog<PasswordEntry> dialog = createPasswordDialog("Update Password", selected);
            dialog.showAndWait().ifPresent(updated -> {
                try {
                    String encrypted = encryptionService.encrypt(updated.getPassword());
                    selected.setTitle(updated.getTitle());
                    selected.setUsername(updated.getUsername());
                    selected.setPassword(encrypted);
                    selected.setNotes(updated.getNotes());
                    dbService.updatePassword(selected);
                    loadPasswords();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });
        } else {
            showAlert("Select a password to update!");
        }
    }

    private void handleCopy() {
        PasswordEntry selected = passwordTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            try {
                String decrypted = encryptionService.decrypt(selected.getPassword());
                ClipboardContent content = new ClipboardContent();
                content.putString(decrypted);
                Clipboard.getSystemClipboard().setContent(content);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            showAlert("Select a password to copy!");
        }
    }

    private Dialog<PasswordEntry> createPasswordDialog(String title, PasswordEntry entry) {
        Dialog<PasswordEntry> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField titleField = new TextField(entry != null ? entry.getTitle() : "");
        TextField usernameField = new TextField(entry != null ? entry.getUsername() : "");
        TextField passwordField = new TextField();
        if (entry != null) {
            try {
                passwordField.setText(encryptionService.decrypt(entry.getPassword()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        TextField notesField = new TextField(entry != null ? entry.getNotes() : "");

        dialog.getDialogPane().setContent(new VBox(10,
                new Label("Title:"), titleField,
                new Label("Username:"), usernameField,
                new Label("Password:"), passwordField,
                new Label("Notes:"), notesField
        ));

        dialog.setResultConverter(button -> {
            if (button == ButtonType.OK) {
                return new PasswordEntry(0,
                        titleField.getText(),
                        usernameField.getText(),
                        passwordField.getText(),
                        notesField.getText());
            }
            return null;
        });
        return dialog;
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING, message, ButtonType.OK);
        alert.showAndWait();
    }
}
