package com.securevault.controller;

import com.securevault.service.DatabaseService;
import com.securevault.utils.AppPreferences;
import com.securevault.utils.LocaleManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ResourceBundle;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Label messageLabel;
    @FXML private Label usernameLabel;
    @FXML private Label passwordLabel;
    @FXML private MenuItem turkishItem, englishItem, germanItem, spanishItem;

    private final DatabaseService dbService = new DatabaseService();

    @FXML
    private void initialize() {
        // Mevcut dile göre etiketleri ayarla
        applyTranslations();

        // Giriş butonu
        loginButton.setOnAction(e -> handleLogin());

        // Dil değiştirme menüleri
        turkishItem.setOnAction(e -> changeLanguage("tr"));
        englishItem.setOnAction(e -> changeLanguage("en"));
        germanItem.setOnAction(e -> changeLanguage("de"));
        spanishItem.setOnAction(e -> changeLanguage("es"));
    }

    private void changeLanguage(String langCode) {
        LocaleManager.setLocale(langCode);
        applyTranslations(); // Arayüzü güncelle
        AppPreferences.setLanguage(langCode);

        // Alert ile bilgilendirme
        Alert alert = new Alert(Alert.AlertType.INFORMATION, "Uygulama dil değiştirildi. Lütfen yeniden başlatın.", ButtonType.OK);
        alert.showAndWait();
    }

    private void applyTranslations() {
        try {
            ResourceBundle bundle = LocaleManager.getBundle();
            usernameLabel.setText(bundle.getString("login.username"));
            passwordLabel.setText(bundle.getString("login.password"));
            loginButton.setText(bundle.getString("login.button"));
            messageLabel.setText(""); // Hata mesajını sıfırla
        } catch (Exception e) {
            System.err.println("Error applying translations: " + e.getMessage());
            // Hata durumunda default değerler
            usernameLabel.setText("Username");
            passwordLabel.setText("Password");
            loginButton.setText("Login");
            messageLabel.setText("");
        }
    }

    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        ResourceBundle bundle = LocaleManager.getBundle();

        if (username.isEmpty() || password.isEmpty()) {
            messageLabel.setText(bundle.getString("login.empty"));
            return;
        }

        Integer userId = dbService.validateUserAndGetId(username, password);

        if (userId != null) {
            openVault(userId);
        } else {
            messageLabel.setText(bundle.getString("login.invalid"));
        }
    }

    private void openVault(int userId) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/vault.fxml"),
                    LocaleManager.getBundle()
            );

            loader.setControllerFactory(param -> {
                try {
                    return new VaultController(userId);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            Stage stage = new Stage();
            stage.setScene(new Scene(loader.load()));
            stage.setTitle(LocaleManager.get("app.title"));
            stage.show();

            // Login ekranını kapat
            ((Stage) loginButton.getScene().getWindow()).close();

        } catch (IOException e) {
            e.printStackTrace();
            showError("Error opening vault: " + e.getMessage());
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}