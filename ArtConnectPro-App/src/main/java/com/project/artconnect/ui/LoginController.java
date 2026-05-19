package com.project.artconnect.ui;

import com.project.artconnect.service.impl.LoginService;
import com.project.artconnect.service.impl.LoginService.DemoAccount;
import com.project.artconnect.util.SessionManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

public class LoginController {

    @FXML private ComboBox<DemoAccount> accountBox;
    @FXML private TextField             emailField;
    @FXML private PasswordField         passwordField;
    @FXML private Label                 helpLabel;
    @FXML private Button                loginButton;

    private final LoginService loginService = new LoginService();

    @FXML
    public void initialize() {
        // Désactiver le bouton pendant le chargement des comptes demo
        loginButton.setDisable(true);
        helpLabel.setText("Chargement des comptes demo...");

        // Charger les comptes demo en arrière-plan
        Task<List<DemoAccount>> loadTask = new Task<>() {
            @Override
            protected List<DemoAccount> call() {
                return loginService.loadDemoAccounts();
            }
        };

        loadTask.setOnSucceeded(e -> {
            List<DemoAccount> accounts = loadTask.getValue();
            accountBox.setItems(FXCollections.observableArrayList(accounts));
            loginButton.setDisable(false);
            helpLabel.setText("Sélectionnez un compte demo ou saisissez vos identifiants.");
        });

        loadTask.setOnFailed(e ->
                helpLabel.setText("Base inaccessible — saisissez vos identifiants manuellement."));

        new Thread(loadTask, "artconnect-login-load").start();

        // Quand on sélectionne un compte demo → pré-remplir les champs
        accountBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                emailField.setText(newVal.email());
                passwordField.setText(newVal.password());
                helpLabel.setText("Compte sélectionné : " + newVal.label());
            }
        });
    }

    @FXML
    private void handleLogin() {
        String email    = emailField.getText();
        String password = passwordField.getText();

        if (email.isBlank() || password.isBlank()) {
            helpLabel.setText("Veuillez saisir un email et un mot de passe.");
            return;
        }

        loginButton.setDisable(true);
        helpLabel.setText("Connexion en cours...");

        Task<Boolean> loginTask = new Task<>() {
            @Override
            protected Boolean call() {
                return loginService.login(email, password);
            }
        };

        loginTask.setOnSucceeded(e -> {
            boolean success = loginTask.getValue();
            if (success) {
                openMainView();
            } else {
                helpLabel.setText("Email ou mot de passe incorrect, ou compte inactif.");
                loginButton.setDisable(false);
            }
        });

        loginTask.setOnFailed(e -> {
            Throwable ex = loginTask.getException();
            helpLabel.setText("Erreur : " + (ex.getMessage() != null ? ex.getMessage() : "connexion impossible."));
            loginButton.setDisable(false);
        });

        new Thread(loginTask, "artconnect-login").start();
    }

    private void openMainView() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/project/artconnect/ui/MainView.fxml"));
            Scene scene = new Scene(loader.load(), 1200, 800);

            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setTitle("ArtConnect Pro — " + SessionManager.getInstance().getRoleLabel()
                    + " : " + SessionManager.getInstance().getFullName());
            stage.setScene(scene);
            stage.show();
        } catch (IOException ex) {
            helpLabel.setText("Impossible de charger l'interface principale.");
            ex.printStackTrace();
        }
    }
}
