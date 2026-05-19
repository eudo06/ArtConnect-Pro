package com.project.artconnect.ui;

import com.project.artconnect.model.Artwork;
import com.project.artconnect.model.Exhibition;
import com.project.artconnect.model.Gallery;
import com.project.artconnect.service.ExhibitionService;
import com.project.artconnect.service.GalleryService;
import com.project.artconnect.util.ServiceProvider;
import com.project.artconnect.util.SessionManager;
import com.project.artconnect.util.ToastManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ExhibitionController {

    @FXML private TableView<Exhibition> exhibitionTable;
    @FXML private TableColumn<Exhibition, String> titleColumn;
    @FXML private TableColumn<Exhibition, LocalDate> dateColumn;
    @FXML private TableColumn<Exhibition, LocalDate> endDateColumn;
    @FXML private TableColumn<Exhibition, String> themeColumn;
    @FXML private TableColumn<Exhibition, String> galleryColumn;
    @FXML private TableColumn<Exhibition, String> curatorColumn;
    @FXML private Label    detailsTitleLabel;
    @FXML private Label    detailsGalleryLabel;
    @FXML private Label    detailsScheduleLabel;
    @FXML private TextArea detailsArtworksArea;
    @FXML private Button   btnAdd;
    @FXML private Button   btnEdit;
    @FXML private Button   btnDelete;

    private final ExhibitionService exhibitionService = ServiceProvider.getExhibitionService();
    private final GalleryService    galleryService    = ServiceProvider.getGalleryService();

    @FXML
    public void initialize() {
        applyRoleRestrictions();
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("startDate"));
        endDateColumn.setCellValueFactory(new PropertyValueFactory<>("endDate"));
        themeColumn.setCellValueFactory(new PropertyValueFactory<>("theme"));
        curatorColumn.setCellValueFactory(new PropertyValueFactory<>("curatorName"));
        galleryColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getGallery() != null
                        ? cellData.getValue().getGallery().getName() : "Unknown"));

        exhibitionTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldValue, newValue) -> showExhibitionDetails(newValue));
        detailsArtworksArea.setEditable(false);
        detailsArtworksArea.setWrapText(true);
        refreshTable();
        clearDetails();
    }

    // ------------------------------------------------------------------
    // Actions CRUD
    // ------------------------------------------------------------------

    private void applyRoleRestrictions() {
        SessionManager session = SessionManager.getInstance();

        // Add : Admin + Promoteur + Artiste
        boolean canAdd = session.isAdmin() || session.isPromoter() || session.isArtist();
        if (btnAdd != null) { btnAdd.setVisible(canAdd); btnAdd.setManaged(canAdd); }

        // Edit + Delete : Admin + Promoteur uniquement
        boolean canEditDelete = session.isAdmin() || session.isPromoter();
        if (btnEdit != null)   { btnEdit.setVisible(canEditDelete);   btnEdit.setManaged(canEditDelete); }
        if (btnDelete != null) { btnDelete.setVisible(canEditDelete); btnDelete.setManaged(canEditDelete); }
    }

    @FXML
    private void handleAddExhibition() {
        // Charger les galeries disponibles pour le ComboBox du dialog
        List<Gallery> galleries = galleryService.getAllGalleries();
        showExhibitionDialog(null, galleries).ifPresent(exhibition ->
                runDbActionAsync("Add Exhibition", () -> {
                    exhibitionService.createExhibition(exhibition);
                    return exhibitionService.getAllExhibitions();
                }, exhibitions -> {
                    exhibitionTable.setItems(FXCollections.observableArrayList(exhibitions));
                    exhibitions.stream()
                            .filter(e -> e.getTitle().equals(exhibition.getTitle()))
                            .findFirst()
                            .ifPresent(e -> exhibitionTable.getSelectionModel().select(e));
                })
        );
    }

    @FXML
    private void handleEditExhibition() {
        Exhibition selected = exhibitionTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            toastInfo("Veuillez sélectionner une exposition à modifier.");
            return;
        }
        List<Gallery> galleries = galleryService.getAllGalleries();
        showExhibitionDialog(selected, galleries).ifPresent(updated ->
                runDbActionAsync("Edit Exhibition", () -> {
                    exhibitionService.updateExhibition(updated);
                    return exhibitionService.getAllExhibitions();
                }, exhibitions -> {
                    exhibitionTable.setItems(FXCollections.observableArrayList(exhibitions));
                    exhibitions.stream()
                            .filter(e -> e.getTitle().equals(updated.getTitle()))
                            .findFirst()
                            .ifPresent(e -> exhibitionTable.getSelectionModel().select(e));
                })
        );
    }

    @FXML
    private void handleDeleteExhibition() {
        Exhibition selected = exhibitionTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            toastInfo("Veuillez sélectionner une exposition à supprimer.");
            return;
        }
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Delete Exhibition");
        confirmation.setHeaderText("Delete \"" + selected.getTitle() + "\"?");
        confirmation.setContentText("This will delete the exhibition and all its artwork links.");
        confirmation.showAndWait()
                .filter(ButtonType.OK::equals)
                .ifPresent(bt ->
                        runDbActionAsync("Delete Exhibition", () -> {
                            exhibitionService.deleteExhibition(selected.getTitle());
                            return exhibitionService.getAllExhibitions();
                        }, exhibitions -> {
                            exhibitionTable.setItems(FXCollections.observableArrayList(exhibitions));
                            clearDetails();
                        })
                );
    }

    // ------------------------------------------------------------------
    // Dialog
    // ------------------------------------------------------------------

    private Optional<Exhibition> showExhibitionDialog(Exhibition existing, List<Gallery> galleries) {
        Dialog<Exhibition> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Add Exhibition" : "Edit Exhibition");
        dialog.setHeaderText(existing == null
                ? "Create a new exhibition in the database."
                : "Update the selected exhibition.");

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        dialog.getDialogPane().setPrefWidth(540);
        dialog.getDialogPane().setPrefHeight(480);
        dialog.setResizable(true);

        // Champs
        TextField titleField = new TextField(existing == null ? "" : safe(existing.getTitle()));
        TextField themeField = new TextField(existing == null ? "" : safe(existing.getTheme()));
        TextField curatorField = new TextField(existing == null ? "" : safe(existing.getCuratorName()));
        TextField startField = new TextField(existing == null ? "" :
                existing.getStartDate() == null ? "" : existing.getStartDate().toString());
        TextField endField = new TextField(existing == null ? "" :
                existing.getEndDate() == null ? "" : existing.getEndDate().toString());
        TextField artworksField = new TextField(existing == null ? "" :
                existing.getArtworks().stream().map(Artwork::getTitle).collect(Collectors.joining(", ")));
        TextArea descArea = new TextArea(existing == null ? "" : safe(existing.getDescription()));
        descArea.setPrefRowCount(3);

        // ComboBox galeries
        ComboBox<Gallery> galleryCombo = new ComboBox<>(FXCollections.observableArrayList(galleries));
        galleryCombo.setPromptText("Select a gallery");
        if (existing != null && existing.getGallery() != null) {
            galleries.stream()
                    .filter(g -> g.getName().equals(existing.getGallery().getName()))
                    .findFirst()
                    .ifPresent(galleryCombo::setValue);
        }

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPrefWidth(480);
        // Colonne 0 : labels fixes, colonne 1 : champs extensibles
        ColumnConstraints col0 = new ColumnConstraints(100);
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setHgrow(javafx.scene.layout.Priority.ALWAYS);
        grid.getColumnConstraints().addAll(col0, col1);
        grid.addRow(0, new Label("Title"),        titleField);
        grid.addRow(1, new Label("Gallery"),      galleryCombo);
        grid.addRow(2, new Label("Theme"),        themeField);
        grid.addRow(3, new Label("Curator"),      curatorField);
        grid.addRow(4, new Label("Start (yyyy-MM-dd)"), startField);
        grid.addRow(5, new Label("End (yyyy-MM-dd)"),   endField);
        grid.addRow(6, new Label("Artworks (titles, séparés par virgule)"), artworksField);
        grid.addRow(7, new Label("Description"),  descArea);
        dialog.getDialogPane().setContent(grid);

        // Désactiver Save si titre ou galerie manquants
        Node saveButton = dialog.getDialogPane().lookupButton(saveButtonType);
        Runnable updateSaveState = () -> saveButton.setDisable(
                titleField.getText().isBlank() || galleryCombo.getValue() == null);
        updateSaveState.run();
        titleField.textProperty().addListener((obs, o, n) -> updateSaveState.run());
        galleryCombo.valueProperty().addListener((obs, o, n) -> updateSaveState.run());

        // Validation avant fermeture
        saveButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            String error = validateDates(startField.getText(), endField.getText());
            if (error != null) {
                event.consume();
                showValidationError(dialog, error);
            }
        });

        dialog.setResultConverter(bt -> {
            if (bt != saveButtonType) return null;
            Exhibition ex = new Exhibition();
            ex.setTitle(titleField.getText().trim());
            ex.setTheme(blankToNull(themeField.getText()));
            ex.setCuratorName(blankToNull(curatorField.getText()));
            ex.setDescription(blankToNull(descArea.getText()));
            ex.setGallery(galleryCombo.getValue());
            ex.setStartDate(parseDate(startField.getText()));
            ex.setEndDate(parseDate(endField.getText()));
            // Convertir les titres d'œuvres en objets Artwork légers
            if (!artworksField.getText().isBlank()) {
                List<Artwork> artworks = Arrays.stream(artworksField.getText().split(","))
                        .map(String::trim)
                        .filter(t -> !t.isEmpty())
                        .map(t -> { Artwork a = new Artwork(); a.setTitle(t); return a; })
                        .collect(Collectors.toList());
                ex.setArtworks(artworks);
            }
            return ex;
        });

        return dialog.showAndWait();
    }

    // ------------------------------------------------------------------
    // Thread de fond
    // ------------------------------------------------------------------

    private <T> void runDbActionAsync(String title, Callable<T> dbAction, Consumer<T> onSuccess) {
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return dbAction.call();
            }
        };
        task.setOnSucceeded(e -> onSuccess.accept(task.getValue()));
        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            showError(title, buildFriendlyMessage(ex));
        });
        Thread thread = new Thread(task, "artconnect-db-" + title.toLowerCase().replace(" ", "-"));
        thread.setDaemon(true);
        thread.start();
    }

    // ------------------------------------------------------------------
    // Affichage détails
    // ------------------------------------------------------------------

    private void refreshTable() {
        runDbActionAsync("Load Exhibitions",
                exhibitionService::getAllExhibitions,
                exhibitions -> exhibitionTable.setItems(
                        FXCollections.observableArrayList(exhibitions)));
    }

    private void showExhibitionDetails(Exhibition exhibition) {
        if (exhibition == null) { clearDetails(); return; }
        detailsTitleLabel.setText(orDash(exhibition.getTitle()) + " | " + orDash(exhibition.getTheme()));
        detailsGalleryLabel.setText(exhibition.getGallery() == null ? "-"
                : orDash(exhibition.getGallery().getName()) + " — " + orDash(exhibition.getGallery().getAddress()));
        detailsScheduleLabel.setText(String.format("%s → %s | Curator: %s",
                exhibition.getStartDate() == null ? "-" : exhibition.getStartDate().toString(),
                exhibition.getEndDate() == null ? "-" : exhibition.getEndDate().toString(),
                orDash(exhibition.getCuratorName())));
        detailsArtworksArea.setText(exhibition.getArtworks().isEmpty()
                ? "No artworks linked."
                : exhibition.getArtworks().stream()
                .map(a -> a.getTitle() + " — "
                        + (a.getArtist() == null ? "-" : orDash(a.getArtist().getName())))
                .collect(Collectors.joining("\n")));
    }

    private void clearDetails() {
        detailsTitleLabel.setText("-");
        detailsGalleryLabel.setText("-");
        detailsScheduleLabel.setText("-");
        detailsArtworksArea.setText("-");
    }

    // ------------------------------------------------------------------
    // Validation
    // ------------------------------------------------------------------

    private String validateDates(String startText, String endText) {
        LocalDate start = parseDate(startText);
        LocalDate end = parseDate(endText);
        if (startText != null && !startText.isBlank() && start == null)
            return "Date de début invalide. Format attendu : yyyy-MM-dd (ex: 2025-06-15).";
        if (endText != null && !endText.isBlank() && end == null)
            return "Date de fin invalide. Format attendu : yyyy-MM-dd (ex: 2025-09-30).";
        if (start != null && end != null && end.isBefore(start))
            return "La date de fin doit être après la date de début.";
        return null;
    }

    private LocalDate parseDate(String text) {
        if (text == null || text.isBlank()) return null;
        try { return LocalDate.parse(text.trim()); }
        catch (DateTimeParseException e) { return null; }
    }

    // ------------------------------------------------------------------
    // Helpers UI
    // ------------------------------------------------------------------

    private void showValidationError(Dialog<?> parent, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Validation");
        alert.setHeaderText("Valeur invalide");
        alert.setContentText(message);
        alert.initOwner(parent.getDialogPane().getScene().getWindow());
        alert.showAndWait();
    }

    private void showInfo(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title); alert.setHeaderText(null);
        alert.setContentText(content); alert.showAndWait();
    }

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title); alert.setHeaderText(null);
        alert.setContentText(content); alert.showAndWait();
    }

    private String buildFriendlyMessage(Throwable t) {
        String msg = collectMessages(t).toLowerCase();
        if (msg.contains("duplicate entry"))
            return "Une exposition avec ce titre existe déjà en base.";
        if (msg.contains("unable to resolve required id"))
            return "La galerie sélectionnée est introuvable en base.";
        if (msg.contains("end_datetime"))
            return "La date de fin doit être postérieure à la date de début.";
        return t.getMessage() == null ? "Erreur lors de l'opération sur la base." : t.getMessage();
    }

    private String collectMessages(Throwable t) {
        StringBuilder sb = new StringBuilder();
        while (t != null) { if (t.getMessage() != null) sb.append(t.getMessage()).append(' '); t = t.getCause(); }
        return sb.toString();
    }

    private String safe(String v)        { return v == null ? "" : v; }
    private String orDash(String v)      { return v == null || v.isBlank() ? "-" : v; }
    private String blankToNull(String v) { return v == null || v.isBlank() ? null : v.trim(); }

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
            if (exhibitionTable != null && exhibitionTable.getScene() != null)
                return exhibitionTable.getScene().getWindow();
        } catch (Exception ignored) {}
        return null;
    }

}