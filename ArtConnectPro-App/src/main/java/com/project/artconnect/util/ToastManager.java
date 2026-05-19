package com.project.artconnect.util;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;

/**
 * Affiche des notifications toast non bloquantes en bas de la fenêtre.
 * Les toasts disparaissent automatiquement après quelques secondes.
 *
 * Usage :
 *   ToastManager.success(stage, "Artiste ajouté avec succès !");
 *   ToastManager.error(stage, "Impossible de supprimer cet artiste.");
 *   ToastManager.info(stage, "Chargement en cours...");
 */
public class ToastManager {

    public enum Type { SUCCESS, ERROR, INFO, WARNING }

    private static final double DURATION_SUCCESS = 2.5;
    private static final double DURATION_ERROR   = 4.0;
    private static final double DURATION_DEFAULT = 3.0;

    // ------------------------------------------------------------------
    // API publique
    // ------------------------------------------------------------------

    public static void success(Window owner, String message) {
        show(owner, message, Type.SUCCESS);
    }

    public static void error(Window owner, String message) {
        show(owner, message, Type.ERROR);
    }

    public static void info(Window owner, String message) {
        show(owner, message, Type.INFO);
    }

    public static void warning(Window owner, String message) {
        show(owner, message, Type.WARNING);
    }

    // ------------------------------------------------------------------
    // Affichage
    // ------------------------------------------------------------------

    public static void show(Window owner, String message, Type type) {
        if (owner == null || message == null || message.isBlank()) return;

        // Toujours sur le thread JavaFX
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> show(owner, message, type));
            return;
        }

        // Construire le toast
        Label label = new Label(message);
        label.setWrapText(true);
        label.setMaxWidth(400);
        label.setStyle("-fx-font-size: 13; -fx-font-family: 'Calibri';");

        VBox toastBox = new VBox(label);
        toastBox.setAlignment(Pos.CENTER);
        toastBox.setPadding(new Insets(12, 20, 12, 20));
        toastBox.setMaxWidth(440);
        toastBox.setStyle(buildStyle(type));

        Popup popup = new Popup();
        popup.getContent().add(toastBox);
        popup.setAutoFix(true);

        // Positionner en bas au centre de la fenêtre owner
        double x = owner.getX() + owner.getWidth()  / 2 - 220;
        double y = owner.getY() + owner.getHeight()  - 80;
        popup.show(owner, x, y);

        // Animation : fade in → pause → fade out
        double duration;
        if (type == Type.SUCCESS) {
            duration = DURATION_SUCCESS;
        } else if (type == Type.ERROR) {
            duration = DURATION_ERROR;
        } else {
            duration = DURATION_DEFAULT;
        }

        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), toastBox);
        fadeIn.setFromValue(0); fadeIn.setToValue(1);

        PauseTransition pause = new PauseTransition(Duration.seconds(duration));

        FadeTransition fadeOut = new FadeTransition(Duration.millis(400), toastBox);
        fadeOut.setFromValue(1); fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> popup.hide());

        new SequentialTransition(fadeIn, pause, fadeOut).play();
    }

    // ------------------------------------------------------------------
    // Styles par type
    // ------------------------------------------------------------------

    private static String buildStyle(Type type) {
        String bg, border, color;
        if (type == Type.SUCCESS) {
            bg = "#2D6A4F"; border = "#1B4332"; color = "white";
        } else if (type == Type.ERROR) {
            bg = "#B85042"; border = "#7B2D26"; color = "white";
        } else if (type == Type.WARNING) {
            bg = "#B8860B"; border = "#7A5A08"; color = "white";
        } else {
            bg = "#1A1A2E"; border = "#3D5A80"; color = "white";
        }
        return String.format(
                "-fx-background-color: %s;" +
                        "-fx-border-color: %s;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 8;" +
                        "-fx-background-radius: 8;" +
                        "-fx-text-fill: %s;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 12, 0, 0, 4);",
                bg, border, color
        );
    }
}