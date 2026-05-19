package com.project.artconnect.ui;

import com.project.artconnect.model.Gallery;
import com.project.artconnect.service.GalleryService;
import com.project.artconnect.util.ServiceProvider;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.awt.Desktop;
import java.net.URI;
import java.util.stream.Collectors;

public class GalleryController {

    @FXML private TableView<Gallery>               galleryTable;
    @FXML private TableColumn<Gallery, String>     nameColumn;
    @FXML private TableColumn<Gallery, String>     addressColumn;
    @FXML private TableColumn<Gallery, String>     hoursColumn;
    @FXML private TableColumn<Gallery, String>     ratingColumn;

    @FXML private Label    detailsNameLabel;
    @FXML private Label    detailsOwnerLabel;
    @FXML private Label    detailsContactLabel;
    @FXML private Label    detailsHoursLabel;
    @FXML private Label    detailsRatingLabel;
    @FXML private Hyperlink detailsWebsiteLink;
    @FXML private TextArea detailsExhibitionsArea;

    private final GalleryService galleryService = ServiceProvider.getGalleryService();

    @FXML
    public void initialize() {
        // Colonnes
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        addressColumn.setCellValueFactory(new PropertyValueFactory<>("address"));
        hoursColumn.setCellValueFactory(new PropertyValueFactory<>("openingHours"));

        // Colonne note — étoiles visuelles
        ratingColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(toStars(cellData.getValue().getRating())));

        galleryTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldVal, newVal) -> showGalleryDetails(newVal));

        detailsExhibitionsArea.setEditable(false);
        detailsExhibitionsArea.setWrapText(true);

        loadGalleries();
        clearDetails();
    }

    // ------------------------------------------------------------------
    // Chargement
    // ------------------------------------------------------------------

    private void loadGalleries() {
        Task<java.util.List<Gallery>> task = new Task<>() {
            @Override
            protected java.util.List<Gallery> call() {
                return galleryService.getAllGalleries();
            }
        };
        task.setOnSucceeded(e ->
                galleryTable.setItems(FXCollections.observableArrayList(task.getValue())));
        Thread t = new Thread(task, "artconnect-galleries");
        t.setDaemon(true);
        t.start();
    }

    // ------------------------------------------------------------------
    // Affichage des détails
    // ------------------------------------------------------------------

    private void showGalleryDetails(Gallery gallery) {
        if (gallery == null) { clearDetails(); return; }

        detailsNameLabel.setText(orDash(gallery.getName())
                + "  |  " + orDash(gallery.getAddress()));

        detailsOwnerLabel.setText(orDash(gallery.getOwnerName()));

        // Téléphone
        detailsContactLabel.setText(orDash(gallery.getContactPhone()));

        // Horaires
        detailsHoursLabel.setText(orDash(gallery.getOpeningHours()));

        // Note avec étoiles + valeur numérique
        double rating = gallery.getRating();
        detailsRatingLabel.setText(toStars(rating)
                + String.format("  (%.1f / 5.0)", rating));

        // Site web — lien cliquable
        if (gallery.getWebsite() != null && !gallery.getWebsite().isBlank()) {
            detailsWebsiteLink.setText(gallery.getWebsite());
            detailsWebsiteLink.setOnAction(e -> openUrl(gallery.getWebsite()));
            detailsWebsiteLink.setDisable(false);
        } else {
            detailsWebsiteLink.setText("Non renseigné");
            detailsWebsiteLink.setDisable(true);
        }

        // Expositions liées
        detailsExhibitionsArea.setText(gallery.getExhibitions().isEmpty()
                ? "Aucune exposition liée."
                : gallery.getExhibitions().stream()
                .map(ex -> ex.getTitle()
                        + (ex.getTheme() != null ? " — " + ex.getTheme() : "")
                        + (ex.getStartDate() != null ? " (" + ex.getStartDate() + ")" : ""))
                .collect(Collectors.joining("\n")));
    }

    private void clearDetails() {
        detailsNameLabel.setText("-");
        detailsOwnerLabel.setText("-");
        detailsContactLabel.setText("-");
        detailsHoursLabel.setText("-");
        detailsRatingLabel.setText("-");
        detailsWebsiteLink.setText("-");
        detailsWebsiteLink.setDisable(true);
        detailsExhibitionsArea.setText("-");
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /**
     * Convertit une note numérique en étoiles Unicode.
     * Ex: 4.2 → "★★★★☆"
     */
    private String toStars(double rating) {
        int full  = (int) Math.round(rating);
        int empty = 5 - full;
        return "★".repeat(Math.max(0, full)) + "☆".repeat(Math.max(0, empty));
    }

    /**
     * Ouvre une URL dans le navigateur par défaut.
     */
    private void openUrl(String url) {
        try {
            String fullUrl = url.startsWith("http") ? url : "https://" + url;
            Desktop.getDesktop().browse(new URI(fullUrl));
        } catch (Exception e) {
            System.err.println("Impossible d'ouvrir l'URL : " + url);
        }
    }

    private String orDash(String v) { return v == null || v.isBlank() ? "-" : v; }
}