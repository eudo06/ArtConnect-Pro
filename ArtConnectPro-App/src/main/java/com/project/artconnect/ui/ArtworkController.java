package com.project.artconnect.ui;

import com.project.artconnect.model.Artist;
import com.project.artconnect.model.ArtworkTag;
import com.project.artconnect.model.Review;
import com.project.artconnect.service.impl.JdbcCommunityService;
import com.project.artconnect.model.Artwork;
import com.project.artconnect.service.ArtistService;
import com.project.artconnect.persistence.JdbcArtworkDao;
import com.project.artconnect.service.ArtworkService;
import com.project.artconnect.util.ServiceProvider;
import com.project.artconnect.util.SessionManager;
import com.project.artconnect.util.ToastManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.Slider;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;

import java.util.Optional;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

public class ArtworkController {

    @FXML private TableView<Artwork>           artworkTable;
    @FXML private TableColumn<Artwork, String> titleColumn;
    @FXML private TableColumn<Artwork, String> typeColumn;
    @FXML private TableColumn<Artwork, Double> priceColumn;
    @FXML private TableColumn<Artwork, String> statusColumn;
    @FXML private TableColumn<Artwork, String> artistColumn;
    @FXML private Label   detailsTitleLabel;
    @FXML private Label   detailsArtistLabel;
    @FXML private Label   detailsMetaLabel;
    @FXML private Label   detailsPriceLabel;
    @FXML private TextArea detailsDescriptionArea;
    @FXML private Label   detailsRatingLabel;
    @FXML private Label   detailsTagsLabel;
    @FXML private ComboBox<ArtworkTag> tagFilter;
    @FXML private Button  btnAdd;
    @FXML private Button  btnEdit;
    @FXML private Button  btnDelete;

    private final ArtworkService artworkService = ServiceProvider.getArtworkService();
    private final JdbcArtworkDao       artworkDao       = new JdbcArtworkDao();
    private final JdbcCommunityService communityService = new JdbcCommunityService();
    @FXML private Button btnAvis;
    @FXML private TextArea detailsReviewsArea;
    private final ArtistService  artistService  = ServiceProvider.getArtistService();

    @FXML
    public void initialize() {
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        artistColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getArtist() != null
                        ? cellData.getValue().getArtist().getName() : "Unknown"));

        // Colorer les lignes selon le statut de l'oeuvre
        artworkTable.setRowFactory(tv -> new javafx.scene.control.TableRow<Artwork>() {
            @Override
            protected void updateItem(Artwork artwork, boolean empty) {
                super.updateItem(artwork, empty);
                if (empty || artwork == null || artwork.getStatus() == null) {
                    setStyle("");
                } else {
                    switch (artwork.getStatus()) {
                        case FOR_SALE -> setStyle("-fx-background-color: #EAF4EE;"); // vert clair
                        case SOLD     -> setStyle("-fx-background-color: #F0F0F0; -fx-text-fill: #999999;"); // gris
                        case EXHIBITED-> setStyle("-fx-background-color: #EEF2FA;"); // bleu clair
                        default       -> setStyle("");
                    }
                }
            }
        });

        artworkTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldVal, newVal) -> {
                    showArtworkDetails(newVal);
                    updateButtonStates(newVal);
                });
        detailsDescriptionArea.setEditable(false);
        detailsDescriptionArea.setWrapText(true);

        applyRoleRestrictions();

        // Charger les tags pour le filtre
        runDbActionAsync("Load Tags",
                () -> artworkDao.findAllTags(),
                tags -> {
                    List<ArtworkTag> allTags = new ArrayList<>();
                    allTags.add(new ArtworkTag("-- Tous les tags --"));
                    allTags.addAll(tags);
                    if (tagFilter != null) {
                        tagFilter.setItems(
                                FXCollections.<ArtworkTag>observableArrayList(allTags));
                        tagFilter.setValue(allTags.get(0));
                        tagFilter.setOnAction(e -> handleTagFilter());
                    }
                });

        refreshTable();
        clearArtworkDetails();
    }

    // ------------------------------------------------------------------
    // Restrictions par rôle
    // ------------------------------------------------------------------

    private void applyRoleRestrictions() {
        SessionManager session = SessionManager.getInstance();

        // Seul l'admin peut ajouter ou supprimer
        boolean isAdmin = session.isAdmin();
        btnAdd.setVisible(isAdmin);
        btnAdd.setManaged(isAdmin);
        btnDelete.setVisible(isAdmin);
        btnDelete.setManaged(isAdmin);

        // Edit : admin toujours, artiste seulement sur sélection (géré dans updateButtonStates)
        boolean canEdit = isAdmin || session.isArtist();
        btnEdit.setVisible(canEdit);
        btnEdit.setManaged(canEdit);
        btnEdit.setDisable(true); // activé à la sélection
    }

    /**
     * Met à jour l'état du bouton Edit selon l'œuvre sélectionnée.
     * Un artiste ne peut éditer que ses propres œuvres.
     */
    @FXML
    private void handleLaisserAvis() {
        Artwork selected = artworkTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Dialog<Review> dialog = new Dialog<>();
        dialog.setTitle("Laisser un avis");
        dialog.setHeaderText("Évaluer : " + selected.getTitle());
        dialog.getDialogPane().setPrefWidth(420);
        dialog.setResizable(true);

        ButtonType saveBtn = new ButtonType("Publier", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        Slider ratingSlider = new Slider(1, 5, 3);
        ratingSlider.setMajorTickUnit(1);
        ratingSlider.setMinorTickCount(0);
        ratingSlider.setSnapToTicks(true);
        ratingSlider.setShowTickLabels(true);
        ratingSlider.setShowTickMarks(true);

        Label ratingDisplay = new Label("Note : 3 / 5");
        ratingSlider.valueProperty().addListener((obs, o, n) ->
                ratingDisplay.setText("Note : " + n.intValue() + " / 5"));

        TextArea commentArea = new TextArea();
        commentArea.setPromptText("Votre commentaire (optionnel)...");
        commentArea.setPrefRowCount(4);
        commentArea.setWrapText(true);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.addRow(0, new Label("Note"),       ratingSlider);
        grid.addRow(1, new Label(""),           ratingDisplay);
        grid.addRow(2, new Label("Commentaire"), commentArea);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(bt -> {
            if (bt != saveBtn) return null;
            Review r = new Review();
            r.setRating((int) ratingSlider.getValue());
            r.setComment(commentArea.getText().trim());
            return r;
        });

        dialog.showAndWait().ifPresent(review -> {
            SessionManager session = SessionManager.getInstance();
            runDbActionAsync("Laisser un avis", () -> {
                communityService.addReview(
                        selected.getTitle(),
                        session.getFullName(),
                        review.getRating(),
                        review.getComment());
                return communityService.getReviewsByArtworkTitle(selected.getTitle());
            }, reviews -> {
                showReviews(reviews);
                // Mettre à jour la note moyenne
                double avg = artworkDao.getAverageRating(selected.getTitle());
                String ratingText = avg == 0.0 ? "Aucun avis"
                        : String.format("Note moyenne : %.1f / 5", avg);
                if (detailsRatingLabel != null) detailsRatingLabel.setText(ratingText);
                toastSuccess("Avis publié — Merci pour votre retour !");
            });
        });
    }

    private void showReviews(java.util.List<Review> reviews) {
        if (detailsReviewsArea == null) return;
        if (reviews.isEmpty()) {
            detailsReviewsArea.setText("Aucun avis pour le moment.");
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (Review r : reviews) {
            sb.append("★".repeat(r.getRating()))
                    .append("☆".repeat(5 - r.getRating()))
                    .append("  ")
                    .append(r.getReviewer() != null ? r.getReviewer().getName() : "Anonyme")
                    .append(r.getReviewDate() != null ? "  (" + r.getReviewDate() + ")" : "")
                    .append("\n");
            if (r.getComment() != null && !r.getComment().isBlank())
                sb.append("  ").append(r.getComment()).append("\n");
            sb.append("\n");
        }
        detailsReviewsArea.setText(sb.toString().trim());
    }

    private void updateButtonStates(Artwork selected) {
        SessionManager session = SessionManager.getInstance();
        if (btnAvis != null && session.isPremiumMember())
            btnAvis.setDisable(selected == null);
        if (session.isAdmin()) {
            btnEdit.setDisable(selected == null);
            btnDelete.setDisable(selected == null);
        } else if (session.isArtist()) {
            // L'artiste peut éditer uniquement si l'œuvre lui appartient
            boolean isOwner = selected != null
                    && selected.getArtist() != null
                    && selected.getArtist().getName().equals(session.getFullName());
            btnEdit.setDisable(!isOwner);
        }
    }

    // ------------------------------------------------------------------
    // Actions CRUD
    // ------------------------------------------------------------------

    @FXML
    private void handleTagFilter() {
        if (tagFilter == null || tagFilter.getValue() == null) return;
        String tagName = tagFilter.getValue().getName();
        if (tagName.startsWith("--")) {
            refreshTable();
        } else {
            runDbActionAsync("Filter by Tag",
                    () -> artworkDao.findByTag(tagName),
                    artworks -> artworkTable.setItems(
                            FXCollections.observableArrayList(artworks)));
        }
    }

    @FXML
    private void handleAddArtwork() {
        if (!SessionManager.getInstance().isAdmin()) return;
        showArtworkDialog(null).ifPresent(artwork ->
                runDbActionAsync("Add Artwork", () -> {
                    artworkService.createArtwork(artwork);
                    return artworkService.getAllArtworks();
                }, artworks -> {
                    artworkTable.setItems(FXCollections.observableArrayList(artworks));
                    artworkService.getArtworkByTitle(artwork.getTitle())
                            .ifPresent(a -> artworkTable.getSelectionModel().select(a));
                })
        );
    }

    @FXML
    private void handleEditArtwork() {
        Artwork selected = artworkTable.getSelectionModel().getSelectedItem();
        if (selected == null) { toastInfo("Veuillez sélectionner une oeuvre à modifier."); return; }

        SessionManager session = SessionManager.getInstance();
        // Double vérification côté contrôleur
        if (!session.isAdmin()) {
            boolean isOwner = selected.getArtist() != null
                    && selected.getArtist().getName().equals(session.getFullName());
            if (!isOwner) {
                showError("Edit Artwork", "Vous ne pouvez modifier que vos propres œuvres.");
                return;
            }
        }

        showArtworkDialog(selected).ifPresent(artwork ->
                runDbActionAsync("Edit Artwork", () -> {
                    artworkService.updateArtwork(artwork);
                    return artworkService.getAllArtworks();
                }, artworks -> {
                    artworkTable.setItems(FXCollections.observableArrayList(artworks));
                    artworkService.getArtworkByTitle(artwork.getTitle())
                            .ifPresent(a -> artworkTable.getSelectionModel().select(a));
                })
        );
    }

    @FXML
    private void handleDeleteArtwork() {
        if (!SessionManager.getInstance().isAdmin()) return;
        Artwork selected = artworkTable.getSelectionModel().getSelectedItem();
        if (selected == null) { toastInfo("Veuillez sélectionner une oeuvre à supprimer."); return; }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Delete Artwork");
        confirmation.setHeaderText("Delete \"" + selected.getTitle() + "\"?");
        confirmation.setContentText("Cette action sera persistée en base.");
        confirmation.showAndWait()
                .filter(ButtonType.OK::equals)
                .ifPresent(bt ->
                        runDbActionAsync("Delete Artwork", () -> {
                            artworkService.deleteArtwork(selected.getTitle());
                            return artworkService.getAllArtworks();
                        }, artworks -> {
                            artworkTable.setItems(FXCollections.observableArrayList(artworks));
                            clearArtworkDetails();
                        })
                );
    }

    // ------------------------------------------------------------------
    // Dialog
    // ------------------------------------------------------------------

    private Optional<Artwork> showArtworkDialog(Artwork existing) {
        Dialog<Artwork> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Add Artwork" : "Edit Artwork");
        dialog.setHeaderText(existing == null
                ? "Create a new artwork stored in the database."
                : "Update the selected artwork in the database.");

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        dialog.getDialogPane().setPrefWidth(540);
        dialog.getDialogPane().setPrefHeight(480);
        dialog.setResizable(true);

        TextField titleField = new TextField(existing == null ? "" : safe(existing.getTitle()));
        ComboBox<String> artistField = new ComboBox<>();
        artistField.setItems(FXCollections.observableArrayList(
                artistService.getAllArtists().stream().map(Artist::getName).toList()));

        SessionManager session = SessionManager.getInstance();
        if (session.isArtist()) {
            // L'artiste ne peut choisir que lui-même
            artistField.setValue(session.getFullName());
            artistField.setDisable(true);
        } else {
            artistField.setValue(existing != null && existing.getArtist() != null
                    ? existing.getArtist().getName() : null);
        }

        TextField typeField  = new TextField(existing == null ? "" : safe(existing.getType()));
        TextField yearField  = new TextField(existing == null || existing.getCreationYear() == null
                ? "" : existing.getCreationYear().toString());
        TextField priceField = new TextField(existing == null ? "" : Double.toString(existing.getPrice()));
        ComboBox<Artwork.Status> statusField = new ComboBox<>();
        statusField.setItems(FXCollections.observableArrayList(Artwork.Status.values()));
        statusField.setValue(existing == null ? Artwork.Status.FOR_SALE : existing.getStatus());

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPrefWidth(480);
        ColumnConstraints col0 = new ColumnConstraints(100);
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setHgrow(javafx.scene.layout.Priority.ALWAYS);
        grid.getColumnConstraints().addAll(col0, col1);
        grid.addRow(0, new Label("Title"),  titleField);
        grid.addRow(1, new Label("Artist"), artistField);
        grid.addRow(2, new Label("Type"),   typeField);
        grid.addRow(3, new Label("Year"),   yearField);
        grid.addRow(4, new Label("Price"),  priceField);
        grid.addRow(5, new Label("Status"), statusField);
        dialog.getDialogPane().setContent(grid);

        Node saveButton = dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.setDisable(titleField.getText().isBlank() || artistField.getValue() == null);
        titleField.textProperty().addListener((obs, o, n) ->
                saveButton.setDisable(n == null || n.isBlank() || artistField.getValue() == null));
        artistField.valueProperty().addListener((obs, o, n) ->
                saveButton.setDisable(titleField.getText().isBlank() || n == null));

        dialog.setResultConverter(bt -> {
            if (bt != saveButtonType) return null;
            Artist artist = artistService.getArtistByName(artistField.getValue())
                    .orElseThrow(() -> new IllegalStateException("Unknown artist: " + artistField.getValue()));
            Integer year  = parseInteger(yearField.getText());
            double  price = parseDouble(priceField.getText());
            Artwork artwork = new Artwork();
            artwork.setTitle(titleField.getText().trim());
            artwork.setArtist(artist);
            artwork.setType(blankToNull(typeField.getText()));
            artwork.setCreationYear(year);
            artwork.setPrice(price);
            artwork.setStatus(statusField.getValue() == null ? Artwork.Status.FOR_SALE : statusField.getValue());
            return artwork;
        });

        try { return dialog.showAndWait(); }
        catch (IllegalArgumentException ex) { showError(dialog.getTitle(), ex.getMessage()); return Optional.empty(); }
    }

    // ------------------------------------------------------------------
    // Async + affichage
    // ------------------------------------------------------------------

    private <T> void runDbActionAsync(String title, Callable<T> dbAction, Consumer<T> onSuccess) {
        Task<T> task = new Task<>() { @Override protected T call() throws Exception { return dbAction.call(); } };
        task.setOnSucceeded(e -> onSuccess.accept(task.getValue()));
        task.setOnFailed(e -> toastError(buildFriendlyMessage(task.getException())));
        Thread t = new Thread(task, "artconnect-db-" + title.toLowerCase().replace(" ", "-"));
        t.setDaemon(true); t.start();
    }

    private void refreshTable() {
        runDbActionAsync("Load Artworks", artworkService::getAllArtworks,
                artworks -> artworkTable.setItems(FXCollections.observableArrayList(artworks)));
    }

    private void showArtworkDetails(Artwork artwork) {
        if (artwork == null) { clearArtworkDetails(); return; }
        detailsTitleLabel.setText(orDash(artwork.getTitle()));
        detailsArtistLabel.setText(artwork.getArtist() == null ? "-" : orDash(artwork.getArtist().getName()));
        detailsMetaLabel.setText(String.format("%s | %s | %s",
                orDash(artwork.getType()),
                artwork.getCreationYear() == null ? "-" : artwork.getCreationYear().toString(),
                artwork.getStatus() == null ? "-" : artwork.getStatus().name()));
        // Formater le prix proprement : 15 000 EUR ou 15 000,50 EUR
        double price = artwork.getPrice();
        String priceStr;
        if (price == 0.0) {
            priceStr = "Prix non défini";
        } else if (price == Math.floor(price)) {
            priceStr = String.format("%,.0f EUR", price).replace(',', ' '); // espace fine
        } else {
            priceStr = String.format("%,.2f EUR", price);
        }
        detailsPriceLabel.setText(priceStr);
        detailsDescriptionArea.setText(orDash(artwork.getDescription()));

        // Afficher les tags
        if (detailsTagsLabel != null) {
            runDbActionAsync("Load Tags for Artwork",
                    () -> artworkDao.findTagsByArtworkTitle(artwork.getTitle()),
                    tags -> detailsTagsLabel.setText(
                            tags.isEmpty() ? "Aucun tag"
                                    : tags.stream().map(t -> "#" + t.getName())
                                    .reduce((a, b) -> a + "  " + b).orElse("")));
        }

        // Charger et afficher les avis
        if (detailsReviewsArea != null) {
            runDbActionAsync("Load Reviews",
                    () -> communityService.getReviewsByArtworkTitle(artwork.getTitle()),
                    this::showReviews);
        }

        // Appel à fn_artwork_average_rating en base
        double avg = artworkDao.getAverageRating(artwork.getTitle());
        String ratingText = avg == 0.0 ? "Aucun avis" : String.format("Note moyenne : %.1f / 5", avg);
        if (detailsRatingLabel != null) detailsRatingLabel.setText(ratingText);
    }

    private void clearArtworkDetails() {
        detailsTitleLabel.setText("-"); detailsArtistLabel.setText("-");
        detailsMetaLabel.setText("-");  detailsPriceLabel.setText("-");
        detailsDescriptionArea.setText("-");
        if (detailsRatingLabel != null) detailsRatingLabel.setText("-");
        if (detailsTagsLabel != null) detailsTagsLabel.setText("-");
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private String buildFriendlyMessage(Throwable t) {
        String msg = t.getMessage() == null ? "" : t.getMessage().toLowerCase();
        if (msg.contains("duplicate entry")) return "Ce titre existe déjà en base.";
        if (msg.contains("unknown artist"))  return "L'artiste sélectionné est introuvable.";
        return t.getMessage() == null ? "Erreur lors de l'opération." : t.getMessage();
    }

    private Integer parseInteger(String v) { return (v == null || v.isBlank()) ? null : Integer.parseInt(v.trim()); }
    private double  parseDouble(String v)  { return (v == null || v.isBlank()) ? 0.0 : Double.parseDouble(v.trim()); }
    private void showInfo(String t, String c)  { Alert a = new Alert(Alert.AlertType.INFORMATION); a.setTitle(t); a.setHeaderText(null); a.setContentText(c); a.showAndWait(); }
    private void showError(String t, String c) { Alert a = new Alert(Alert.AlertType.ERROR);       a.setTitle(t); a.setHeaderText(null); a.setContentText(c); a.showAndWait(); }
    private String safe(String v)       { return v == null ? "" : v; }
    private String orDash(String v)     { return v == null || v.isBlank() ? "-" : v; }
    private String blankToNull(String v){ return v == null || v.isBlank() ? null : v.trim(); }
    private String collectMessages(Throwable t) { StringBuilder sb = new StringBuilder(); while (t != null) { if (t.getMessage() != null) sb.append(t.getMessage()).append(' '); t = t.getCause(); } return sb.toString(); }

    // ------------------------------------------------------------------
    // Toast notifications
    // ------------------------------------------------------------------

    private void toastSuccess(String message) {
        javafx.stage.Window w = getWindow();
        if (w != null) ToastManager.success(w, message);
    }

    private void toastError(String message) {
        javafx.stage.Window w = getWindow();
        if (w != null) ToastManager.error(w, message);
    }

    private void toastInfo(String message) {
        javafx.stage.Window w = getWindow();
        if (w != null) ToastManager.info(w, message);
    }

    private javafx.stage.Window getWindow() {
        try {
            if (artworkTable != null && artworkTable.getScene() != null)
                return artworkTable.getScene().getWindow();
        } catch (Exception ignored) {}
        return null;
    }

}