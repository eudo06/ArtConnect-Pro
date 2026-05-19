package com.project.artconnect.ui;

import com.project.artconnect.model.Booking;
import com.project.artconnect.model.CommunityMember;
import com.project.artconnect.service.WorkshopService;
import com.project.artconnect.util.ServiceProvider;
import com.project.artconnect.util.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.LocalDateTime;
import java.util.List;

public class MesInscriptionsController {

    @FXML private TableView<Booking>                  bookingTable;
    @FXML private TableColumn<Booking, String>        workshopColumn;
    @FXML private TableColumn<Booking, String>        instructorColumn;
    @FXML private TableColumn<Booking, LocalDateTime> dateColumn;
    @FXML private TableColumn<Booking, String>        locationColumn;
    @FXML private TableColumn<Booking, String>        statusColumn;
    @FXML private Label labelMembre;
    @FXML private Label labelTotal;

    private final WorkshopService workshopService = ServiceProvider.getWorkshopService();

    @FXML
    public void initialize() {
        // Colonne atelier
        workshopColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getWorkshop() != null
                        ? cellData.getValue().getWorkshop().getTitle() : "-"));

        // Colonne instructeur
        instructorColumn.setCellValueFactory(cellData -> {
            if (cellData.getValue().getWorkshop() == null) return new SimpleStringProperty("-");
            if (cellData.getValue().getWorkshop().getInstructor() == null) return new SimpleStringProperty("-");
            return new SimpleStringProperty(cellData.getValue().getWorkshop().getInstructor().getName());
        });

        // Colonne date de réservation
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("bookingDate"));

        // Colonne lieu
        locationColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getWorkshop() != null
                        ? orDash(cellData.getValue().getWorkshop().getLocation()) : "-"));

        // Colonne statut paiement
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("paymentStatus"));

        // Afficher le nom du membre connecté
        SessionManager session = SessionManager.getInstance();
        labelMembre.setText("Inscriptions de : " + session.getFullName()
                + " (" + session.getRoleLabel() + ")");

        loadBookings();
    }

    private void loadBookings() {
        SessionManager session = SessionManager.getInstance();

        // Construire un CommunityMember léger depuis la session
        CommunityMember member = new CommunityMember();
        member.setName(session.getFullName());

        Task<List<Booking>> task = new Task<>() {
            @Override
            protected List<Booking> call() {
                return workshopService.getBookingsByMember(member);
            }
        };

        task.setOnSucceeded(e -> {
            List<Booking> bookings = task.getValue();
            bookingTable.setItems(FXCollections.observableArrayList(bookings));
            labelTotal.setText("Total : " + bookings.size() + " inscription(s)");
        });

        task.setOnFailed(e ->
                labelTotal.setText("Erreur lors du chargement des inscriptions."));

        Thread t = new Thread(task, "artconnect-mes-inscriptions");
        t.setDaemon(true);
        t.start();
    }

    @FXML
    private void handleRefresh() {
        loadBookings();
    }

    private String orDash(String v) { return v == null || v.isBlank() ? "-" : v; }
}
