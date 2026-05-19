package com.project.artconnect.ui;

import com.project.artconnect.model.Artist;
import com.project.artconnect.model.Discipline;
import com.project.artconnect.service.ArtistService;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ArtistController {
    @FXML private TextField searchField;
    @FXML private ComboBox<Discipline> disciplineFilter;
    @FXML private TableView<Artist> artistTable;
    @FXML private TableColumn<Artist, String> nameColumn;
    @FXML private TableColumn<Artist, String> cityColumn;
    @FXML private TableColumn<Artist, String> emailColumn;
    @FXML private TableColumn<Artist, Integer> yearColumn;
    @FXML private Label detailsNameLabel;
    @FXML private Label detailsCityLabel;
    @FXML private Label detailsEmailLabel;
    @FXML private Label detailsDisciplinesLabel;
    @FXML private TextArea detailsBioArea;
    @FXML private TextArea detailsEventsArea;
    @FXML private Button btnAdd;
    @FXML private Button btnEdit;
    @FXML private Button btnDelete;

    private final ArtistService artistService = ServiceProvider.getArtistService();

    @FXML
    public void initialize() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        cityColumn.setCellValueFactory(new PropertyValueFactory<>("city"));
        yearColumn.setCellValueFactory(new PropertyValueFactory<>("birthYear"));

        applyRoleRestrictions();

        disciplineFilter.setItems(FXCollections.observableArrayList(artistService.getAllDisciplines()));
        artistTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldValue, newValue) -> showArtistDetails(newValue));
        detailsBioArea.setEditable(false);
        detailsBioArea.setWrapText(true);
        refreshTable();
        clearArtistDetails();
    }

    private void applyRoleRestrictions() {
        SessionManager session = SessionManager.getInstance();
        boolean isAdmin = session.isAdmin();

        // Colonne email — masquée pour les non-admins
        emailColumn.setCellValueFactory(cellData -> {
            if (isAdmin) {
                return new SimpleStringProperty(
                        cellData.getValue().getContactEmail() != null
                                ? cellData.getValue().getContactEmail() : "-");
            }
            return new SimpleStringProperty("••••••••");
        });

        // Bouton Add et Delete — admin uniquement
        if (btnAdd != null) {
            btnAdd.setVisible(isAdmin);
            btnAdd.setManaged(isAdmin);
        }
        if (btnDelete != null) {
            btnDelete.setVisible(isAdmin);
            btnDelete.setManaged(isAdmin);
        }

        // Bouton Edit — admin ou artiste (sur son propre profil)
        if (btnEdit != null) {
            boolean canEdit = isAdmin || session.isArtist();
            btnEdit.setVisible(canEdit);
            btnEdit.setManaged(canEdit);
        }

        // Label email dans le panneau de détails — admin uniquement
        if (detailsEmailLabel != null) {
            artistTable.getSelectionModel().selectedItemProperty()
                    .addListener((obs, oldVal, newVal) -> {
                        if (newVal == null) return;
                        if (isAdmin) {
                            detailsEmailLabel.setText(orDash(newVal.getContactEmail()));
                        } else {
                            detailsEmailLabel.setText("••••••••");
                        }
                        // Artiste : Edit uniquement sur son propre profil
                        if (btnEdit != null && session.isArtist()) {
                            btnEdit.setDisable(!newVal.getName().equals(session.getFullName()));
                        }
                        // Charger les événements de l'artiste
                        loadArtistEvents(newVal.getName());
                    });
        }
    }

    private void loadArtistEvents(String artistName) {
        if (detailsEventsArea == null) return;
        detailsEventsArea.setText("Chargement...");

        Task<List<String>> task = new Task<>() {
            @Override
            protected List<String> call() throws Exception {
                String sql = """
                        SELECT e.title, e.start_datetime,
                               ae.role_in_event,
                               CASE
                                   WHEN ex.event_id IS NOT NULL THEN 'Exposition'
                                   WHEN w.event_id  IS NOT NULL THEN 'Atelier'
                                   ELSE 'Evenement'
                               END AS event_type
                        FROM artist_event ae
                        JOIN artist a  ON ae.artist_id = a.artist_id
                        JOIN event  e  ON ae.event_id  = e.event_id
                        LEFT JOIN exhibition ex ON e.event_id = ex.event_id
                        LEFT JOIN workshop   w  ON e.event_id = w.event_id
                        WHERE a.stage_name = ?
                        ORDER BY e.start_datetime DESC
                        """;
                List<String> lines = new ArrayList<>();
                try (var conn = com.project.artconnect.util.ConnectionManager.getConnection();
                     var stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, artistName);
                    try (var rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            String date = rs.getTimestamp("start_datetime") != null
                                    ? rs.getTimestamp("start_datetime").toLocalDateTime()
                                    .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                                    : "-";
                            String role = rs.getString("role_in_event");
                            lines.add(String.format("[%s] %s  (%s)%s",
                                    rs.getString("event_type"),
                                    rs.getString("title"),
                                    date,
                                    role != null ? "  — " + role : ""));
                        }
                    }
                }
                return lines;
            }
        };

        task.setOnSucceeded(e -> {
            List<String> lines = task.getValue();
            if (detailsEventsArea != null)
                detailsEventsArea.setText(lines.isEmpty()
                        ? "Aucun evenement lie."
                        : String.join("\n", lines));
        });

        task.setOnFailed(e -> {
            if (detailsEventsArea != null)
                detailsEventsArea.setText("Erreur de chargement.");
        });

        Thread t = new Thread(task, "artconnect-artist-events");
        t.setDaemon(true);
        t.start();
    }

    @FXML
    private void handleSearch() {
        String query = searchField.getText();
        Discipline d = disciplineFilter.getValue();
        String dName = (d != null) ? d.getName() : null;
        artistTable.setItems(FXCollections.observableArrayList(
                artistService.searchArtists(query, dName, null)));
    }

    @FXML
    private void handleReset() {
        searchField.clear();
        disciplineFilter.setValue(null);
        refreshTable();
    }

    private void refreshTable() {
        artistTable.setItems(FXCollections.observableArrayList(artistService.getAllArtists()));
    }

    // ------------------------------------------------------------------
    // Actions CRUD — exécutées sur un thread de fond pour ne pas figer l'UI
    // ------------------------------------------------------------------

    @FXML
    private void handleAddArtist() {
        showArtistDialog(null).ifPresent(artist -> {
            runDbActionAsync("Add Artist", () -> {
                artistService.createArtist(artist);
                // Les trois requêtes suivantes s'exécutent en fond,
                // l'UI reste réactive pendant ce temps.
                List<Discipline> disciplines = artistService.getAllDisciplines();
                List<Artist> artists         = artistService.getAllArtists();
                Optional<Artist> created     = artistService.getArtistByName(artist.getName());
                return new Object[]{disciplines, artists, created};
            }, result -> {
                // Retour sur le thread JavaFX pour mettre à jour l'UI
                Object[] data = (Object[]) result;
                disciplineFilter.setItems(FXCollections.observableArrayList((List<Discipline>) data[0]));
                artistTable.setItems(FXCollections.observableArrayList((List<Artist>) data[1]));
                ((Optional<Artist>) data[2]).ifPresent(a -> artistTable.getSelectionModel().select(a));
            });
        });
    }

    @FXML
    private void handleEditArtist() {
        Artist selectedArtist = artistTable.getSelectionModel().getSelectedItem();
        if (selectedArtist == null) {
            toastInfo("Veuillez sélectionner un artiste à modifier.");
            return;
        }
        showArtistDialog(selectedArtist).ifPresent(updatedArtist -> {
            runDbActionAsync("Edit Artist", () -> {
                artistService.updateArtist(updatedArtist);
                List<Discipline> disciplines = artistService.getAllDisciplines();
                List<Artist> artists         = artistService.getAllArtists();
                Optional<Artist> updated     = artistService.getArtistByName(updatedArtist.getName());
                return new Object[]{disciplines, artists, updated};
            }, result -> {
                Object[] data = (Object[]) result;
                disciplineFilter.setItems(FXCollections.observableArrayList((List<Discipline>) data[0]));
                artistTable.setItems(FXCollections.observableArrayList((List<Artist>) data[1]));
                ((Optional<Artist>) data[2]).ifPresent(a -> artistTable.getSelectionModel().select(a));
            });
        });
    }

    @FXML
    private void handleDeleteArtist() {
        Artist selectedArtist = artistTable.getSelectionModel().getSelectedItem();
        if (selectedArtist == null) {
            toastInfo("Veuillez sélectionner un artiste à supprimer.");
            return;
        }
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Delete Artist");
        confirmation.setHeaderText("Delete " + selectedArtist.getName() + "?");
        confirmation.setContentText("This action will persist in the database.");
        confirmation.showAndWait()
                .filter(ButtonType.OK::equals)
                .ifPresent(buttonType -> {
                    runDbActionAsync("Delete Artist", () -> {
                        artistService.deleteArtist(selectedArtist.getName());
                        List<Discipline> disciplines = artistService.getAllDisciplines();
                        List<Artist> artists         = artistService.getAllArtists();
                        return new Object[]{disciplines, artists};
                    }, result -> {
                        Object[] data = (Object[]) result;
                        disciplineFilter.setItems(FXCollections.observableArrayList((List<Discipline>) data[0]));
                        artistTable.setItems(FXCollections.observableArrayList((List<Artist>) data[1]));
                        clearArtistDetails();
                    });
                });
    }

    // ------------------------------------------------------------------
    // Thread de fond — JavaFX Task
    // ------------------------------------------------------------------

    /**
     * Exécute une opération BD sur un thread de fond (Task JavaFX).
     * L'UI reste réactive pendant la requête.
     * Le callback onSuccess est exécuté sur le thread JavaFX après succès.
     *
     * @param title     Nom de l'opération (affiché dans les messages d'erreur)
     * @param dbAction  Callable exécuté en fond — peut retourner des données
     * @param onSuccess Consumer exécuté sur le thread UI avec le résultat
     */
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
            showError(title, buildFriendlyMessage(
                    ex instanceof Exception ? (Exception) ex : new Exception(ex)));
        });

        Thread thread = new Thread(task, "artconnect-db-" + title.toLowerCase().replace(" ", "-"));
        thread.setDaemon(true); // le thread se termine avec l'application
        thread.start();
    }

    // ------------------------------------------------------------------
    // Dialog artiste
    // ------------------------------------------------------------------

    private Optional<Artist> showArtistDialog(Artist existingArtist) {
        Dialog<Artist> dialog = new Dialog<>();
        dialog.setTitle(existingArtist == null ? "Add Artist" : "Edit Artist");
        dialog.setHeaderText(existingArtist == null
                ? "Create a new artist stored in the database."
                : "Update the selected artist in the database.");

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // Taille fixe et centrage du dialog — évite le fond noir en plein écran
        dialog.getDialogPane().setPrefWidth(520);
        dialog.getDialogPane().setPrefHeight(420);
        dialog.setResizable(true);

        TextField nameField = new TextField(existingArtist == null ? "" : safe(existingArtist.getName()));
        TextField cityField = new TextField(existingArtist == null ? "" : safe(existingArtist.getCity()));
        TextField emailField = new TextField(existingArtist == null ? "" : safe(existingArtist.getContactEmail()));
        TextField yearField = new TextField(existingArtist == null || existingArtist.getBirthYear() == null
                ? "" : existingArtist.getBirthYear().toString());
        TextField disciplinesField = new TextField(existingArtist == null
                ? "" : existingArtist.getDisciplines().stream()
                .map(Discipline::getName).reduce((a, b) -> a + ", " + b).orElse(""));
        TextArea bioArea = new TextArea(existingArtist == null ? "" : safe(existingArtist.getBio()));
        bioArea.setPrefRowCount(3);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPrefWidth(480);
        // Colonne 0 : labels fixes, colonne 1 : champs extensibles
        ColumnConstraints col0 = new ColumnConstraints(100);
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setHgrow(javafx.scene.layout.Priority.ALWAYS);
        grid.getColumnConstraints().addAll(col0, col1);
        grid.addRow(0, new Label("Name"),        nameField);
        grid.addRow(1, new Label("City"),        cityField);
        grid.addRow(2, new Label("Email"),       emailField);
        grid.addRow(3, new Label("Birth Year"),  yearField);
        grid.addRow(4, new Label("Disciplines"), disciplinesField);
        grid.addRow(5, new Label("Bio"),         bioArea);
        dialog.getDialogPane().setContent(grid);

        Node saveButton = dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.setDisable(nameField.getText().isBlank());
        nameField.textProperty().addListener((obs, oldValue, newValue) ->
                saveButton.setDisable(newValue == null || newValue.isBlank()));

        dialog.setResultConverter(buttonType -> {
            if (buttonType != saveButtonType) return null;

            Integer birthYear = parseInteger(yearField.getText());
            if (birthYear != null && (birthYear < 1000 || birthYear > 2100)) {
                throw new IllegalArgumentException("Birth year must be between 1000 and 2100.");
            }

            String email = blankToNull(emailField.getText());
            if (email != null && !email.contains("@")) {
                throw new IllegalArgumentException("Email must contain '@'.");
            }

            Artist artist = new Artist();
            artist.setName(nameField.getText().trim());
            artist.setCity(blankToNull(cityField.getText()));
            artist.setContactEmail(email);
            artist.setBirthYear(birthYear);
            artist.setBio(blankToNull(bioArea.getText()));
            artist.setActive(existingArtist == null || existingArtist.isActive());
            artist.setDisciplines(parseDisciplines(disciplinesField.getText()));
            return artist;
        });

        try {
            return dialog.showAndWait();
        } catch (IllegalArgumentException ex) {
            showError(dialog.getTitle(), ex.getMessage());
            return Optional.empty();
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private List<Discipline> parseDisciplines(String rawDisciplines) {
        List<Discipline> disciplines = new ArrayList<>();
        if (rawDisciplines == null || rawDisciplines.isBlank()) return disciplines;
        for (String token : rawDisciplines.split(",")) {
            String trimmed = token.trim();
            if (!trimmed.isEmpty()) disciplines.add(new Discipline(trimmed));
        }
        return disciplines;
    }

    private Integer parseInteger(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) return null;
        return Integer.parseInt(rawValue.trim());
    }

    private void refreshDisciplines() {
        disciplineFilter.setItems(FXCollections.observableArrayList(artistService.getAllDisciplines()));
    }

    private void showArtistDetails(Artist artist) {
        if (artist == null) { clearArtistDetails(); return; }
        detailsNameLabel.setText(safe(artist.getName()));
        detailsCityLabel.setText(orDash(artist.getCity()));
        detailsEmailLabel.setText(orDash(artist.getContactEmail()));
        detailsDisciplinesLabel.setText(artist.getDisciplines().isEmpty() ? "-"
                : artist.getDisciplines().stream().map(Discipline::getName).collect(Collectors.joining(", ")));
        detailsBioArea.setText(orDash(artist.getBio()));
    }

    private void clearArtistDetails() {
        detailsNameLabel.setText("-");
        detailsCityLabel.setText("-");
        detailsEmailLabel.setText("-");
        detailsDisciplinesLabel.setText("-");
        detailsBioArea.setText("-");
        if (detailsEventsArea != null) detailsEventsArea.setText("-");
    }

    private void showInfo(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private String buildFriendlyMessage(Throwable throwable) {
        String combinedMessage = collectMessages(throwable).toLowerCase();
        if (combinedMessage.contains("duplicate entry"))
            return "Impossible d'enregistrer cet artiste : le nom ou l'adresse email existe deja dans la base.";
        if (combinedMessage.contains("fk_workshop_artist"))
            return "Impossible de supprimer cet artiste car il est encore reference dans un workshop.";
        if (combinedMessage.contains("birth year must be between"))
            return "L'annee de naissance saisie est invalide.";
        if (combinedMessage.contains("check constraint"))
            return "Une contrainte de la base est violee. Verifie les valeurs saisies.";
        return throwable.getMessage() == null || throwable.getMessage().isBlank()
                ? "Une erreur est survenue pendant l'operation sur la base."
                : throwable.getMessage();
    }

    private String collectMessages(Throwable throwable) {
        StringBuilder builder = new StringBuilder();
        Throwable current = throwable;
        while (current != null) {
            if (current.getMessage() != null) builder.append(current.getMessage()).append(' ');
            current = current.getCause();
        }
        return builder.toString();
    }

    private String safe(String value)   { return value == null ? "" : value; }
    private String orDash(String value) { return value == null || value.isBlank() ? "-" : value; }
    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }

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
            if (artistTable != null && artistTable.getScene() != null)
                return artistTable.getScene().getWindow();
        } catch (Exception ignored) {}
        return null;
    }

}