package com.project.artconnect;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        // L'application démarre toujours sur l'écran de connexion
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/project/artconnect/ui/LoginView.fxml"));
        Scene scene = new Scene(loader.load(), 600, 500);

        stage.setTitle("ArtConnect Pro — Connexion");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
