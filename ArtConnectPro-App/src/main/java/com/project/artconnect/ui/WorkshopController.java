package com.project.artconnect.ui;

import com.project.artconnect.model.CommunityMember;
import com.project.artconnect.model.Workshop;
import com.project.artconnect.service.CommunityService;
import com.project.artconnect.persistence.JdbcWorkshopDao;
import com.project.artconnect.service.WorkshopService;
import com.project.artconnect.util.ServiceProvider;
import com.project.artconnect.util.SessionManager;
import com.project.artconnect.util.ToastManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.LocalDateTime;

public class WorkshopController {

    @FXML private TableView<Workshop>               workshopTable;
    @FXML private TableColumn<Workshop, String>     titleColumn;
    @FXML private TableColumn<Workshop, LocalDateTime> dateColumn;
    @FXML private TableColumn<Workshop, String>     instructorColumn;
    @FXML private TableColumn<Workshop, Double>     priceColumn;
    @FXML private TableColumn<Workshop, String>     levelColumn;
    @FXML private TableColumn<Workshop, Number>     capacityColumn;
    @FXML private TableColumn<Workshop, String>     locationColumn;
    @FXML private Label    detailsInstructorLabel;
    @FXML private Label    detailsScheduleLabel;
    @FXML private Label    detailsCapacityLabel;
    @FXML private TextArea detailsDescriptionArea;
    @FXML private Button   btnInscription;

    private final WorkshopService  workshopService  = ServiceProvider.getWorkshopService();
    private final JdbcWorkshopDao   workshopDao      = new JdbcWorkshopDao();
    private final CommunityService communityService = ServiceProvider.getCommunityService();

    @FXML
    public void initialize() {
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));
        levelColumn.setCellValueFactory(new PropertyValueFactory<>("level"));
        capacityColumn.setCellValueFactory(new PropertyValueFactory<>("maxParticipants"));
        locationColumn.setCellValueFactory(new PropertyValueFactory<>("location"));
        instructorColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getInstructor() != null
                        ? cellData.getValue().getInstructor().getName() : "Unknown"));

        workshopTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldVal, newVal) -> {
                    showWorkshopDetails(newVal);
                    // Activer le bouton seulement si un workshop est sélectionné
                    if (btnInscription != null)
                        btnInscription.setDisable(newVal == null);
                });

        detailsDescriptionArea.setEditable(false);
        detailsDescriptionArea.setWrapText(true);

        applyRoleRestrictions();
        refreshTable();
        clearDetails();
    }

    // ------------------------------------------------------------------
    // Restrictions par rôle
    // ------------------------------------------------------------------

    private void applyRoleRestrictions() {
        if (btnInscription == null) return;
        SessionManager session = SessionManager.getInstance();

        // Le bouton S'inscrire est visible uniquement pour les membres Premium
        boolean canBook = session.isPremiumMember();
        btnInscription.setVisible(canBook);
        btnInscription.setManaged(canBook);
        btnInscription.setDisable(true); // activé à la sélection
    }

    // ------------------------------------------------------------------
    // Inscription
    // ------------------------------------------------------------------

    @FXML
    private void handleInscription() {
        Workshop selected = workshopTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        SessionManager session = SessionManager.getInstance();

        // Récupérer le membre connecté depuis son nom dans la session
        CommunityMember member = communityService.getAllMembers().stream()
                .filter(m -> m.getName().equals(session.getFullName()))
                .findFirst()
                .orElse(null);

        if (member == null) {
            showError("Inscription", "Votre profil membre est introuvable en base.");
            return;
        }

        // Confirmation avant inscription
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("S'inscrire");
        confirm.setHeaderText("S'inscrire à \"" + selected.getTitle() + "\" ?");
        confirm.setContentText(String.format(
                "Prix : %.2f EUR%nNiveau : %s%nLieu : %s",
                selected.getPrice(),
                orDash(selected.getLevel()),
                orDash(selected.getLocation())));

        confirm.showAndWait()
                .filter(ButtonType.OK::equals)
                .ifPresent(bt -> bookWorkshop(selected, member));
    }

    private void bookWorkshop(Workshop workshop, CommunityMember member) {
        btnInscription.setDisable(true);

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                workshopService.bookWorkshop(workshop, member);
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            showSuccess("Inscription confirmée",
                    "Vous êtes inscrit à \"" + workshop.getTitle() + "\" !\n"
                            + "Votre réservation est enregistrée en base.");
            refreshTable(); // Recharger pour mettre à jour les places
        });

        task.setOnFailed(e -> {
            btnInscription.setDisable(false);
            String msg = buildFriendlyMessage(task.getException());
            showError("Inscription impossible", msg);
        });

        Thread thread = new Thread(task, "artconnect-booking");
        thread.setDaemon(true);
        thread.start();
    }

    // ------------------------------------------------------------------
    // Affichage
    // ------------------------------------------------------------------

    private void refreshTable() {
        Task<java.util.List<Workshop>> task = new Task<>() {
            @Override
            protected java.util.List<Workshop> call() {
                return workshopService.getAllWorkshops();
            }
        };
        task.setOnSucceeded(e ->
                workshopTable.setItems(FXCollections.observableArrayList(task.getValue())));
        Thread t = new Thread(task, "artconnect-workshops-load");
        t.setDaemon(true);
        t.start();
    }

    private void showWorkshopDetails(Workshop workshop) {
        if (workshop == null) { clearDetails(); return; }
        detailsInstructorLabel.setText(workshop.getInstructor() == null
                ? "-" : orDash(workshop.getInstructor().getName()));
        // Prix atelier formaté
        String workshopPrice = workshop.getPrice() == 0.0 ? "Gratuit"
                : String.format("%.0f EUR", workshop.getPrice());
        detailsScheduleLabel.setText(String.format("%s | %s | %s",
                workshop.getDate() == null ? "-" : workshop.getDate().toString(),
                orDash(workshop.getLocation()),
                workshopPrice));

        // Appel à fn_workshop_participant_count en base
        int participants = workshopDao.getParticipantCount(workshop.getEventId());
        int capacity     = workshop.getMaxParticipants();
        String capacityText = String.format("%d / %d inscrits | %d min | %s",
                participants, capacity,
                workshop.getDurationMinutes(),
                orDash(workshop.getLevel()));
        detailsCapacityLabel.setText(capacityText);
        detailsDescriptionArea.setText(orDash(workshop.getDescription()));
    }

    private void clearDetails() {
        detailsInstructorLabel.setText("-");
        detailsScheduleLabel.setText("-");
        detailsCapacityLabel.setText("-");
        detailsDescriptionArea.setText("-");
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private String buildFriendlyMessage(Throwable t) {
        String msg = "";
        Throwable current = t;
        while (current != null) {
            if (current.getMessage() != null) msg += current.getMessage() + " ";
            current = current.getCause();
        }
        msg = msg.toLowerCase();

        if (msg.contains("nombre maximal de participants"))
            return "Cet atelier est complet — plus de places disponibles.";
        if (msg.contains("duplicate entry") || msg.contains("uq_booking"))
            return "Vous êtes déjà inscrit à cet atelier.";
        if (msg.contains("introuvable") || msg.contains("not found"))
            return "Atelier introuvable en base. Veuillez rafraîchir la liste.";
        return t.getMessage() == null
                ? "Erreur lors de l'inscription." : t.getMessage();
    }

    private void showSuccess(String title, String content) {
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

    private String orDash(String v) { return v == null || v.isBlank() ? "-" : v; }

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
            if (workshopTable != null && workshopTable.getScene() != null)
                return workshopTable.getScene().getWindow();
        } catch (Exception ignored) {}
        return null;
    }

}