package com.securevault;

import com.securevault.service.EncryptionService;
import com.securevault.utils.AppPreferences;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Locale;
import java.util.ResourceBundle;

public class Main extends Application {
    public static byte[] appKey;
    public static ResourceBundle bundle;

    @Override
    public void start(Stage stage) throws Exception {

        // Key üretimi / yükleme
        Path keyFile = Paths.get(System.getProperty("user.home"), ".securevault_key");
        if (Files.exists(keyFile)) {
            appKey = Base64.getDecoder().decode(Files.readString(keyFile));
        } else {
            appKey = EncryptionService.generateKey();
            Files.writeString(keyFile, Base64.getEncoder().encodeToString(appKey));
        }

        // Kaydedilmiş dili yükle
        String lang = AppPreferences.getLanguage(); // "tr", "en", "de", "es"
        if (lang == null || lang.isBlank()) lang = "tr";

        Locale locale = switch (lang) {
            case "en" -> Locale.ENGLISH;
            case "de" -> Locale.GERMAN;
            case "es" -> new Locale("es");
            default -> new Locale("tr");
        };

        try {
            bundle = ResourceBundle.getBundle("lang.messages", locale);
        } catch (Exception e) {
            System.err.println("ResourceBundle bulunamadı, varsayılan Türkçe yüklenecek.");
            bundle = ResourceBundle.getBundle("lang.messages", new Locale("tr"));
        }

        //  FXML yükleme
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"), bundle);
        Parent root = loader.load();

        // 4️⃣ Scene oluşturma ve stil ekleme
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());

        stage.setTitle(bundle.getString("app.title"));
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
