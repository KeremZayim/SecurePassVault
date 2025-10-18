package com.securevault.controller;

import com.securevault.Main;
import com.securevault.model.PasswordEntry;
import com.securevault.service.DatabaseService;
import com.securevault.service.EncryptionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import java.io.*;
import java.util.List;

public class VaultController {

    private final int userId;
    private final DatabaseService dbService = new DatabaseService();
    private final EncryptionService encryptionService;
    private final ObservableList<PasswordEntry> passwordList = FXCollections.observableArrayList();

    @FXML private TableView<PasswordEntry> passwordTable;
    @FXML private TableColumn<PasswordEntry, String> titleColumn, usernameColumn, notesColumn, passwordColumn;
    @FXML private TextField searchField;
    @FXML private Button addButton, deleteButton, updateButton, copyButton;
    @FXML private MenuItem importMenuItem, exportMenuItem;
    @FXML private Label notificationLabel;

    public VaultController(int userId) throws Exception {
        this.userId = userId;
        encryptionService = new EncryptionService(Main.appKey);
    }

    @FXML
    private void initialize() {
        // --- Table columns ---
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        notesColumn.setCellValueFactory(new PropertyValueFactory<>("notes"));
        passwordColumn.setCellValueFactory(new PropertyValueFactory<>("password"));

        // --- Password Show/Hide ---
        passwordColumn.setCellFactory(col -> new TableCell<>() {
            private final Label passwordLabel = new Label("******");
            private final Button toggleBtn = new Button();
            private final HBox hbox = new HBox(5, passwordLabel, toggleBtn);
            private boolean showing = false;

            @Override
            protected void updateItem(String encryptedPassword, boolean empty) {
                super.updateItem(encryptedPassword, empty);
                if (empty || encryptedPassword == null) { setGraphic(null); return; }
                PasswordEntry entry = getTableView().getItems().get(getIndex());
                try {
                    passwordLabel.setText(showing ? encryptionService.decrypt(entry.getPassword()) : "******");
                    toggleBtn.setText(showing ? "Hide" : "Show");
                } catch (Exception e) { passwordLabel.setText("******"); }
                toggleBtn.setOnAction(e -> { showing = !showing; updateItem(entry.getPassword(), false); });
                setGraphic(hbox);
            }
        });

        loadPasswords();

        // --- Button actions ---
        addButton.setOnAction(e -> handleAdd());
        deleteButton.setOnAction(e -> handleDelete());
        updateButton.setOnAction(e -> handleUpdate());
        copyButton.setOnAction(e -> handleCopy());

        // --- Import/Export ---
        importMenuItem.setOnAction(e -> showImportDialog());
        exportMenuItem.setOnAction(e -> showExportDialog());

        // --- Search / filter ---
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.isBlank()) passwordTable.setItems(passwordList);
            else {
                ObservableList<PasswordEntry> filtered = FXCollections.observableArrayList();
                for (PasswordEntry entry : passwordList) {
                    if (entry.getTitle().toLowerCase().contains(newVal.toLowerCase()) ||
                            entry.getUsername().toLowerCase().contains(newVal.toLowerCase())) filtered.add(entry);
                }
                passwordTable.setItems(filtered);
            }
        });
    }

    // ----------------- Notification -----------------
    private void showNotification(String message) {
        Platform.runLater(() -> {
            notificationLabel.setText(message);
            notificationLabel.setVisible(true);

            Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(3), e -> {
                notificationLabel.setVisible(false);
                notificationLabel.setText("");
            }));
            timeline.setCycleCount(1);
            timeline.play();
        });
    }

    // ----------------- Import/Export Dialogs -----------------
    private void showImportDialog() {
        ChoiceDialog<String> dialog = new ChoiceDialog<>("CSV", "CSV", "JSON");
        dialog.setTitle("Import");
        dialog.setHeaderText("Choose format to import");
        dialog.setContentText("Format:");

        dialog.showAndWait().ifPresent(format -> {
            if (format.equals("CSV")) importFromCSV();
            else importFromJson();
        });
    }

    private void showExportDialog() {
        ChoiceDialog<String> dialog = new ChoiceDialog<>("CSV", "CSV", "JSON");
        dialog.setTitle("Export");
        dialog.setHeaderText("Choose format to export");
        dialog.setContentText("Format:");

        dialog.showAndWait().ifPresent(format -> {
            if (format.equals("CSV")) exportToCSV();
            else exportToJson();
        });
    }

    // ----------------- CSV -----------------
    private void exportToCSV() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export to CSV");
        fileChooser.setInitialFileName("passwords.csv");
        File file = fileChooser.showSaveDialog(passwordTable.getScene().getWindow());
        if (file != null) {
            try (PrintWriter writer = new PrintWriter(file)) {
                writer.println("Title,Username,Password,Notes");
                for (PasswordEntry entry : passwordList) {
                    String line = String.format("%s,%s,%s,%s",
                            escapeCsv(entry.getTitle()),
                            escapeCsv(entry.getUsername()),
                            escapeCsv(encryptionService.decrypt(entry.getPassword())),
                            escapeCsv(entry.getNotes()));
                    writer.println(line);
                }
                showNotification("CSV başarıyla dışa aktarıldı!");
            } catch (Exception e) {
                e.printStackTrace();
                showNotification("CSV dışa aktarma başarısız: " + e.getMessage());
            }
        }
    }

    private void importFromCSV() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import from CSV");
        File file = fileChooser.showOpenDialog(passwordTable.getScene().getWindow());
        if (file != null) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line = reader.readLine(); // Skip header
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(",", -1);
                    if (parts.length >= 4) {
                        String encrypted = encryptionService.encrypt(parts[2]);
                        PasswordEntry entry = new PasswordEntry(0, parts[0], parts[1], encrypted, parts[3]);
                        dbService.addPassword(entry, userId);
                    }
                }
                loadPasswords();
                showNotification("CSV başarıyla içe aktarıldı!");
            } catch (Exception e) {
                e.printStackTrace();
                showNotification("CSV içe aktarma başarısız: " + e.getMessage());
            }
        }
    }

    // ----------------- JSON -----------------
    private void exportToJson() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export to JSON");
        fileChooser.setInitialFileName("passwords.json");
        File file = fileChooser.showSaveDialog(passwordTable.getScene().getWindow());
        if (file != null) {
            try {
                ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
                List<PasswordEntry> decryptedList = passwordList.stream().map(e -> {
                    try {
                        return new PasswordEntry(e.getId(), e.getTitle(), e.getUsername(),
                                encryptionService.decrypt(e.getPassword()), e.getNotes());
                    } catch (Exception ex) { ex.printStackTrace(); return null; }
                }).filter(e -> e != null).toList();
                mapper.writeValue(file, decryptedList);
                showNotification("JSON başarıyla dışa aktarıldı!");
            } catch (Exception e) {
                e.printStackTrace();
                showNotification("JSON dışa aktarma başarısız: " + e.getMessage());
            }
        }
    }

    private void importFromJson() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import from JSON");
        File file = fileChooser.showOpenDialog(passwordTable.getScene().getWindow());
        if (file != null) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                PasswordEntry[] entries = mapper.readValue(file, PasswordEntry[].class);
                for (PasswordEntry e : entries) {
                    String encrypted = encryptionService.encrypt(e.getPassword());
                    PasswordEntry entry = new PasswordEntry(0, e.getTitle(), e.getUsername(), encrypted, e.getNotes());
                    dbService.addPassword(entry, userId);
                }
                loadPasswords();
                showNotification("JSON başarıyla içe aktarıldı!");
            } catch (Exception e) {
                e.printStackTrace();
                showNotification("JSON içe aktarma başarısız: " + e.getMessage());
            }
        }
    }

    // ----------------- CRUD -----------------
    private void loadPasswords() {
        passwordList.clear();
        passwordList.addAll(dbService.getPasswordsForUser(userId));
        passwordTable.setItems(passwordList);
    }

    private void handleAdd() {
        Dialog<PasswordEntry> dialog = createPasswordDialog("Add New Password", null);
        dialog.showAndWait().ifPresent(entry -> {
            try {
                String encrypted = encryptionService.encrypt(entry.getPassword());
                dbService.addPassword(new PasswordEntry(0, entry.getTitle(), entry.getUsername(), encrypted, entry.getNotes()), userId);
                loadPasswords();
                showNotification("Şifre başarıyla eklendi!");
            } catch (Exception ex) { ex.printStackTrace(); }
        });
    }

    private void handleDelete() {
        PasswordEntry selected = passwordTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            dbService.deletePassword(selected.getId(), userId);
            loadPasswords();
            showNotification("Şifre başarıyla silindi!");
        } else showNotification("Silmek için bir şifre seçin!");
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
                    dbService.updatePassword(selected, userId);
                    loadPasswords();
                    showNotification("Şifre başarıyla güncellendi!");
                } catch (Exception ex) { ex.printStackTrace(); }
            });
        } else showNotification("Güncellemek için bir şifre seçin!");
    }

    private void handleCopy() {
        PasswordEntry selected = passwordTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            try {
                String decrypted = encryptionService.decrypt(selected.getPassword());
                ClipboardContent content = new ClipboardContent();
                content.putString(decrypted);
                Clipboard.getSystemClipboard().setContent(content);
                showNotification("Şifre panoya kopyalandı!");
            } catch (Exception e) { e.printStackTrace(); }
        } else showNotification("Kopyalamak için bir şifre seçin!");
    }

    // ----------------- Dialog -----------------
    private Dialog<PasswordEntry> createPasswordDialog(String title, PasswordEntry entry) {
        Dialog<PasswordEntry> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField titleField = new TextField(entry != null ? entry.getTitle() : "");
        TextField usernameField = new TextField(entry != null ? entry.getUsername() : "");
        TextField passwordField = new TextField();
        if (entry != null) {
            try { passwordField.setText(encryptionService.decrypt(entry.getPassword())); }
            catch (Exception e) { e.printStackTrace(); }
        }
        TextField notesField = new TextField(entry != null ? entry.getNotes() : "");

        dialog.getDialogPane().setContent(new VBox(10,
                new Label("Title:"), titleField,
                new Label("Username:"), usernameField,
                new Label("Password:"), passwordField,
                new Label("Notes:"), notesField
        ));

        dialog.setResultConverter(button -> {
            if (button == ButtonType.OK)
                return new PasswordEntry(0, titleField.getText(), usernameField.getText(), passwordField.getText(), notesField.getText());
            return null;
        });

        return dialog;
    }

    // ----------------- CSV Helper -----------------
    private String escapeCsv(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            value = value.replace("\"", "\"\"");
            value = "\"" + value + "\"";
        }
        return value;
    }
}
