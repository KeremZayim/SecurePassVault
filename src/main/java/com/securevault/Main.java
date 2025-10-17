package com.securevault;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class Main extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        // FXML dosyasının resource yolunu alıyoruz
        URL fxmlLocation = Main.class.getResource("/fxml/login.fxml");

        if (fxmlLocation == null) {
            System.err.println("FXML dosyası bulunamadı! Yol doğru mu?");
            System.exit(1);
        }

        // FXMLLoader ile FXML yükleme
        FXMLLoader fxmlLoader = new FXMLLoader(fxmlLocation);
        Scene scene = new Scene(fxmlLoader.load());

        stage.setTitle("SecurePassVault");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
