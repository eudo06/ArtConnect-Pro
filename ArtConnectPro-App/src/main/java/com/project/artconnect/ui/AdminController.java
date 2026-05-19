package com.project.artconnect.ui;

import com.project.artconnect.util.ConnectionManager;
import com.project.artconnect.util.SessionManager;
import com.project.artconnect.util.ToastManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AdminController {

    // --- Statistiques ---
    @FXML private Label statArtistesLabel;
    @FXML private Label statMembresLabel;
    @FXML private Label statWorkshopsLabel;
    @FXML private Label statExpositionsLabel;
    @FXML private Label statReservationsLabel;
    @FXML private Label statAvisLabel;

    // --- Journal d'audit ---
    @FXML private TableView<String[]>               auditTable;
    @FXML private TableColumn<String[], String>     auditEventCol;
    @FXML private TableColumn<String[], String>     auditActionCol;
    @FXML private TableColumn<String[], String>     auditOldTitleCol;
    @FXML private TableColumn<String[], String>     auditNewTitleCol;
    @FXML private TableColumn<String[], String>     auditDateCol;

    // --- Demandes artistes ---
    @FXML private TableView<String[]>               artistAppTable;
    @FXML private TableColumn<String[], String>     artAppEmailCol;
    @FXML private TableColumn<String[], String>     artAppDisciplineCol;
    @FXML private TableColumn<String[], String>     artAppDateCol;
    @FXML private TableColumn<String[], String>     artAppStatusCol;
    @FXML private Button                            btnApproveArtist;
    @FXML private Button                            btnRejectArtist;

    // --- Demandes promoteurs ---
    @FXML private TableView<String[]>               promoterAppTable;
    @FXML private TableColumn<String[], String>     proAppEmailCol;
    @FXML private TableColumn<String[], String>     proAppOrgCol;
    @FXML private TableColumn<String[], String>     proAppDateCol;
    @FXML private TableColumn<String[], String>     proAppStatusCol;
    @FXML private Button                            btnApprovePromoter;
    @FXML private Button                            btnRejectPromoter;

    @FXML
    public void initialize() {
        setupAuditTable();
        setupArtistAppTable();
        setupPromoterAppTable();
        loadAll();
    }

    // ------------------------------------------------------------------
    // Configuration des tables
    // ------------------------------------------------------------------

    private void setupAuditTable() {
        auditEventCol.setCellValueFactory(cd    -> new SimpleStringProperty(cd.getValue()[0]));
        auditActionCol.setCellValueFactory(cd   -> new SimpleStringProperty(cd.getValue()[1]));
        auditOldTitleCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue()[2]));
        auditNewTitleCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue()[3]));
        auditDateCol.setCellValueFactory(cd     -> new SimpleStringProperty(cd.getValue()[4]));

        // Colorier les lignes selon le type d'action
        auditTable.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(String[] item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setStyle(""); return; }
                if ("UPDATE".equals(item[1])) {
                    setStyle("-fx-background-color: #FFF8E1;");
                } else if ("DELETE".equals(item[1])) {
                    setStyle("-fx-background-color: #FDECEA;");
                } else {
                    setStyle("");
                }
            }
        });
    }

    private void setupArtistAppTable() {
        artAppEmailCol.setCellValueFactory(cd      -> new SimpleStringProperty(cd.getValue()[0]));
        artAppDisciplineCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue()[1]));
        artAppDateCol.setCellValueFactory(cd       -> new SimpleStringProperty(cd.getValue()[2]));
        artAppStatusCol.setCellValueFactory(cd     -> new SimpleStringProperty(cd.getValue()[3]));

        artistAppTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, o, n) -> {
                    boolean selected = n != null && "SUBMITTED".equals(n[3]);
                    btnApproveArtist.setDisable(!selected);
                    btnRejectArtist.setDisable(!selected);
                });
        btnApproveArtist.setDisable(true);
        btnRejectArtist.setDisable(true);
    }

    private void setupPromoterAppTable() {
        proAppEmailCol.setCellValueFactory(cd  -> new SimpleStringProperty(cd.getValue()[0]));
        proAppOrgCol.setCellValueFactory(cd    -> new SimpleStringProperty(cd.getValue()[1]));
        proAppDateCol.setCellValueFactory(cd   -> new SimpleStringProperty(cd.getValue()[2]));
        proAppStatusCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue()[3]));

        promoterAppTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, o, n) -> {
                    boolean selected = n != null && "SUBMITTED".equals(n[3]);
                    btnApprovePromoter.setDisable(!selected);
                    btnRejectPromoter.setDisable(!selected);
                });
        btnApprovePromoter.setDisable(true);
        btnRejectPromoter.setDisable(true);
    }

    // ------------------------------------------------------------------
    // Chargement de toutes les données
    // ------------------------------------------------------------------

    private void loadAll() {
        Task<Object[]> task = new Task<>() {
            @Override
            protected Object[] call() throws Exception {
                try (Connection conn = ConnectionManager.getConnection()) {
                    String[] stats       = loadStats(conn);
                    List<String[]> audit = loadAudit(conn);
                    List<String[]> artApps  = loadArtistApplications(conn);
                    List<String[]> proApps  = loadPromoterApplications(conn);
                    return new Object[]{stats, audit, artApps, proApps};
                }
            }
        };

        task.setOnSucceeded(e -> {
            Object[] data = (Object[]) task.getValue();
            String[]       stats    = (String[])       data[0];
            List<String[]> audit    = (List<String[]>) data[1];
            List<String[]> artApps  = (List<String[]>) data[2];
            List<String[]> proApps  = (List<String[]>) data[3];

            // Stats
            statArtistesLabel.setText(stats[0]);
            statMembresLabel.setText(stats[1]);
            statWorkshopsLabel.setText(stats[2]);
            statExpositionsLabel.setText(stats[3]);
            statReservationsLabel.setText(stats[4]);
            statAvisLabel.setText(stats[5]);

            // Tables
            auditTable.setItems(FXCollections.observableArrayList(audit));
            artistAppTable.setItems(FXCollections.observableArrayList(artApps));
            promoterAppTable.setItems(FXCollections.observableArrayList(proApps));
        });

        task.setOnFailed(e ->
                System.err.println("Erreur chargement Admin : " + task.getException().getMessage()));

        Thread t = new Thread(task, "artconnect-admin-load");
        t.setDaemon(true);
        t.start();
    }

    // ------------------------------------------------------------------
    // Requêtes SQL
    // ------------------------------------------------------------------

    private String[] loadStats(Connection conn) throws SQLException {
        String[] stats = new String[6];
        String[][] queries = {
                {"SELECT COUNT(*) FROM artist",           "0"},
                {"SELECT COUNT(*) FROM community_member", "0"},
                {"SELECT COUNT(*) FROM workshop",         "0"},
                {"SELECT COUNT(*) FROM exhibition",       "0"},
                {"SELECT COUNT(*) FROM booking",          "0"},
                {"SELECT COUNT(*) FROM review",           "0"},
        };
        for (int i = 0; i < queries.length; i++) {
            try (PreparedStatement stmt = conn.prepareStatement(queries[i][0]);
                 ResultSet rs = stmt.executeQuery()) {
                stats[i] = rs.next() ? String.valueOf(rs.getInt(1)) : "0";
            }
        }
        return stats;
    }

    private List<String[]> loadAudit(Connection conn) throws SQLException {
        List<String[]> rows = new ArrayList<>();
        String sql = """
                SELECT event_id, action_type,
                       IFNULL(old_title, '-') AS old_title,
                       IFNULL(new_title, '-') AS new_title,
                       DATE_FORMAT(changed_at, '%d/%m/%Y %H:%i') AS changed_at
                FROM event_audit
                ORDER BY changed_at DESC
                LIMIT 50
                """;
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                rows.add(new String[]{
                        "Event #" + rs.getString("event_id"),
                        rs.getString("action_type"),
                        rs.getString("old_title"),
                        rs.getString("new_title"),
                        rs.getString("changed_at")
                });
            }
        }
        return rows;
    }

    private List<String[]> loadArtistApplications(Connection conn) throws SQLException {
        List<String[]> rows = new ArrayList<>();
        String sql = """
                SELECT a.email,
                       IFNULL(aa.discipline_requested, '-') AS discipline,
                       DATE_FORMAT(aa.submission_date, '%d/%m/%Y') AS submission_date,
                       aa.status,
                       aa.artist_application_id
                FROM artist_application aa
                JOIN account a ON aa.account_id = a.account_id
                ORDER BY aa.submission_date DESC
                """;
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                rows.add(new String[]{
                        rs.getString("email"),
                        rs.getString("discipline"),
                        rs.getString("submission_date"),
                        rs.getString("status"),
                        rs.getString("artist_application_id")
                });
            }
        }
        return rows;
    }

    private List<String[]> loadPromoterApplications(Connection conn) throws SQLException {
        List<String[]> rows = new ArrayList<>();
        String sql = """
                SELECT a.email,
                       IFNULL(pa.organization_name, '-') AS org,
                       DATE_FORMAT(pa.submission_date, '%d/%m/%Y') AS submission_date,
                       pa.status,
                       pa.promoter_application_id
                FROM promoter_application pa
                JOIN account a ON pa.account_id = a.account_id
                ORDER BY pa.submission_date DESC
                """;
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                rows.add(new String[]{
                        rs.getString("email"),
                        rs.getString("org"),
                        rs.getString("submission_date"),
                        rs.getString("status"),
                        rs.getString("promoter_application_id")
                });
            }
        }
        return rows;
    }

    // ------------------------------------------------------------------
    // Actions sur les demandes
    // ------------------------------------------------------------------

    @FXML
    private void handleApproveArtist() {
        updateApplicationStatus(artistAppTable, "artist_application",
                "artist_application_id", "APPROVED");
    }

    @FXML
    private void handleRejectArtist() {
        updateApplicationStatus(artistAppTable, "artist_application",
                "artist_application_id", "REJECTED");
    }

    @FXML
    private void handleApprovePromoter() {
        updateApplicationStatus(promoterAppTable, "promoter_application",
                "promoter_application_id", "APPROVED");
    }

    @FXML
    private void handleRejectPromoter() {
        updateApplicationStatus(promoterAppTable, "promoter_application",
                "promoter_application_id", "REJECTED");
    }

    private void updateApplicationStatus(TableView<String[]> table,
                                         String tableName, String idCol, String newStatus) {
        String[] selected = table.getSelectionModel().getSelectedItem();
        if (selected == null || selected.length < 5) return;

        String appId = selected[4];

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                String sql = "UPDATE " + tableName + " SET status = ? WHERE " + idCol + " = ?";
                try (Connection conn = ConnectionManager.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, newStatus);
                    stmt.setInt(2, Integer.parseInt(appId));
                    stmt.executeUpdate();
                }
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            selected[3] = newStatus;
            table.refresh();
            table.getSelectionModel().clearSelection();
            toastSuccess("Demande mise à jour — statut : " + newStatus);
        });

        task.setOnFailed(e ->
                toastError("Impossible de mettre à jour la demande."));

        Thread t = new Thread(task, "artconnect-admin-update");
        t.setDaemon(true);
        t.start();
    }

    @FXML
    private void handleRefresh() {
        loadAll();
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private void showInfo(String title, String content) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(content); a.showAndWait();
    }

    private void showError(String title, String content) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(content); a.showAndWait();
    }

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
            if (auditTable != null && auditTable.getScene() != null)
                return auditTable.getScene().getWindow();
        } catch (Exception ignored) {}
        return null;
    }

}