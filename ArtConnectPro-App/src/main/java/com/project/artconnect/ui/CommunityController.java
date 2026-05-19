package com.project.artconnect.ui;

import com.project.artconnect.model.CommunityMember;
import com.project.artconnect.model.Discipline;
import com.project.artconnect.model.Review;
import com.project.artconnect.service.CommunityService;
import com.project.artconnect.service.WorkshopService;
import com.project.artconnect.util.ConnectionManager;
import com.project.artconnect.util.ServiceProvider;
import com.project.artconnect.util.SessionManager;
import com.project.artconnect.util.ToastManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CommunityController {

    // --- Tableau membres ---
    @FXML private TableView<CommunityMember>           memberTable;
    @FXML private TableColumn<CommunityMember, String> nameColumn;
    @FXML private TableColumn<CommunityMember, String> emailColumn;
    @FXML private TableColumn<CommunityMember, String> cityColumn;
    @FXML private TableColumn<CommunityMember, String> membershipColumn;
    @FXML private Label    detailsMemberLabel;
    @FXML private Label    detailsMembershipLabel;
    @FXML private Label    detailsPhoneLabel;
    @FXML private Label    detailsDisciplinesLabel;
    @FXML private TextArea detailsActivityArea;
    @FXML private Label    labelPhone;
    @FXML private Label    labelEmail;

    // --- Forum ---
    @FXML private ListView<String[]> communityList;
    @FXML private VBox               forumMessagesBox;
    @FXML private ScrollPane         forumScrollPane;
    @FXML private TextArea           forumInput;
    @FXML private Button             btnPublier;
    @FXML private Label              forumCommunityLabel;

    private final CommunityService communityService = ServiceProvider.getCommunityService();
    private final WorkshopService  workshopService  = ServiceProvider.getWorkshopService();

    private int selectedCommunityId = -1;

    @FXML
    public void initialize() {
        setupMemberTable();
        applyRoleRestrictions();
        setupForumList();
        loadMembers();
        loadCommunities();
        clearDetails();
    }

    // ------------------------------------------------------------------
    // Membres
    // ------------------------------------------------------------------

    private void setupMemberTable() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        cityColumn.setCellValueFactory(new PropertyValueFactory<>("city"));
        membershipColumn.setCellValueFactory(new PropertyValueFactory<>("membershipType"));
        memberTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, o, n) -> showMemberDetails(n));
        detailsActivityArea.setEditable(false);
        detailsActivityArea.setWrapText(true);
    }

    private void applyRoleRestrictions() {
        boolean isAdmin = SessionManager.getInstance().isAdmin();

        emailColumn.setCellValueFactory(cellData -> {
            if (isAdmin) {
                return new SimpleStringProperty(
                        cellData.getValue().getEmail() != null
                                ? cellData.getValue().getEmail() : "-");
            }
            return new SimpleStringProperty("••••••••");
        });

        if (labelEmail != null)       { labelEmail.setVisible(isAdmin);       labelEmail.setManaged(isAdmin); }
        if (labelPhone != null)       { labelPhone.setVisible(isAdmin);       labelPhone.setManaged(isAdmin); }
        if (detailsPhoneLabel != null){ detailsPhoneLabel.setVisible(isAdmin); detailsPhoneLabel.setManaged(isAdmin); }

        // Champ de saisie forum — Membre Premium + Admin
        boolean canPost = SessionManager.getInstance().isPremiumMember() || isAdmin;
        if (forumInput != null) {
            forumInput.setDisable(!canPost);
            forumInput.setPromptText(canPost
                    ? "Écrivez votre message ici..."
                    : "Seuls les membres Premium peuvent publier.");
        }
        if (btnPublier != null) btnPublier.setDisable(!canPost);
    }

    private void loadMembers() {
        Task<List<CommunityMember>> task = new Task<>() {
            @Override protected List<CommunityMember> call() {
                return communityService.getAllMembers();
            }
        };
        task.setOnSucceeded(e ->
                memberTable.setItems(FXCollections.observableArrayList(task.getValue())));
        Thread t = new Thread(task, "artconnect-members"); t.setDaemon(true); t.start();
    }

    private void showMemberDetails(CommunityMember member) {
        if (member == null) { clearDetails(); return; }
        boolean isAdmin = SessionManager.getInstance().isAdmin();

        List<Review> reviews   = communityService.getReviewsByMember(member);
        int bookingsCount      = workshopService.getBookingsByMember(member).size();

        detailsMemberLabel.setText(orDash(member.getName()));
        detailsMembershipLabel.setText(orDash(member.getMembershipType()));
        if (detailsPhoneLabel != null && detailsPhoneLabel.isVisible())
            detailsPhoneLabel.setText(isAdmin ? orDash(member.getPhone()) : "••••••••");

        detailsDisciplinesLabel.setText(member.getFavoriteDisciplines().isEmpty() ? "-"
                : member.getFavoriteDisciplines().stream()
                .map(Discipline::getName).collect(Collectors.joining(", ")));

        detailsActivityArea.setText(String.format(
                "Réservations : %d\nAvis : %d\nOeuvres évaluées : %s",
                bookingsCount, reviews.size(),
                reviews.stream().limit(3)
                        .map(r -> r.getArtwork() == null ? null : r.getArtwork().getTitle())
                        .filter(t -> t != null && !t.isBlank())
                        .collect(Collectors.joining(", "))));
    }

    private void clearDetails() {
        detailsMemberLabel.setText("-");
        detailsMembershipLabel.setText("-");
        if (detailsPhoneLabel != null) detailsPhoneLabel.setText("-");
        detailsDisciplinesLabel.setText("-");
        detailsActivityArea.setText("-");
    }

    // ------------------------------------------------------------------
    // Forum
    // ------------------------------------------------------------------

    private void setupForumList() {
        if (communityList == null) return;
        communityList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String[] item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); return; }
                setText(item[1]); // nom de la communauté
            }
        });
        communityList.getSelectionModel().selectedItemProperty()
                .addListener((obs, o, n) -> {
                    if (n == null) return;
                    selectedCommunityId = Integer.parseInt(n[0]);
                    if (forumCommunityLabel != null)
                        forumCommunityLabel.setText(n[1] + "  —  " + n[2]);
                    loadForumMessages(selectedCommunityId);
                });
    }

    private void loadCommunities() {
        if (communityList == null) return;
        Task<List<String[]>> task = new Task<>() {
            @Override
            protected List<String[]> call() throws Exception {
                List<String[]> rows = new ArrayList<>();
                String sql = "SELECT community_id, name, IFNULL(description,'') FROM community ORDER BY community_id";
                try (Connection conn = ConnectionManager.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql);
                     ResultSet rs = stmt.executeQuery()) {
                    while (rs.next())
                        rows.add(new String[]{
                                String.valueOf(rs.getInt(1)),
                                rs.getString(2),
                                rs.getString(3)
                        });
                }
                return rows;
            }
        };
        task.setOnSucceeded(e -> {
            communityList.setItems(FXCollections.observableArrayList(task.getValue()));
            if (!task.getValue().isEmpty())
                communityList.getSelectionModel().selectFirst();
        });
        Thread t = new Thread(task, "artconnect-communities"); t.setDaemon(true); t.start();
    }

    private void loadForumMessages(int communityId) {
        if (forumMessagesBox == null) return;
        Task<List<String[]>> task = new Task<>() {
            @Override
            protected List<String[]> call() throws Exception {
                List<String[]> posts = new ArrayList<>();
                String sql = """
                        SELECT cp.post_id, cm.full_name, cp.content,
                               DATE_FORMAT(cp.posted_at, '%d/%m/%Y %H:%i') AS posted_at
                        FROM community_post cp
                        JOIN community_member cm ON cp.member_id = cm.member_id
                        WHERE cp.community_id = ?
                        ORDER BY cp.posted_at ASC
                        """;
                try (Connection conn = ConnectionManager.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setInt(1, communityId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next())
                            posts.add(new String[]{
                                    rs.getString("post_id"),
                                    rs.getString("full_name"),
                                    rs.getString("content"),
                                    rs.getString("posted_at")
                            });
                    }
                }
                return posts;
            }
        };

        task.setOnSucceeded(e -> {
            List<String[]> posts = task.getValue();
            forumMessagesBox.getChildren().clear();

            if (posts.isEmpty()) {
                Label empty = new Label("Aucun message. Soyez le premier à publier !");
                empty.setStyle("-fx-text-fill: #888888; -fx-font-style: italic; -fx-padding: 20;");
                forumMessagesBox.getChildren().add(empty);
                return;
            }

            SessionManager session = SessionManager.getInstance();
            for (String[] post : posts)
                forumMessagesBox.getChildren().add(buildMessageCard(post, session));

            if (forumScrollPane != null) forumScrollPane.setVvalue(1.0);
        });

        Thread t = new Thread(task, "artconnect-forum-load"); t.setDaemon(true); t.start();
    }

    private VBox buildMessageCard(String[] post, SessionManager session) {
        boolean isOwnPost = post[1].equals(session.getFullName());
        boolean isAdmin   = session.isAdmin();

        VBox card = new VBox(5);
        card.setPadding(new Insets(10, 14, 10, 14));
        card.setMaxWidth(Double.MAX_VALUE);
        card.setStyle(
                "-fx-background-color: " + (isOwnPost ? "#EAF4EE" : "#FFFFFF") + ";" +
                        "-fx-border-color: "     + (isOwnPost ? "#2D6A4F" : "#E0D4C8") + ";" +
                        "-fx-border-width: 0 0 0 3;" +
                        "-fx-background-radius: 6; -fx-border-radius: 6;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 4, 0, 0, 1);"
        );

        Label header = new Label(post[1] + "   •   " + post[3]
                + (isOwnPost ? "   (vous)" : ""));
        header.setStyle("-fx-font-size: 11; -fx-text-fill: #888888; -fx-font-weight: bold;");

        Label content = new Label(post[2]);
        content.setWrapText(true);
        content.setMaxWidth(Double.MAX_VALUE);
        content.setStyle("-fx-font-size: 13; -fx-text-fill: #1A1A2E;");

        card.getChildren().addAll(header, content);

        if (isAdmin || isOwnPost) {
            Button btnDel = new Button("Supprimer");
            btnDel.setStyle("-fx-font-size: 10; -fx-text-fill: #B85042; -fx-background-color: transparent; -fx-cursor: hand;");
            btnDel.setOnAction(e -> deletePost(Integer.parseInt(post[0])));
            card.getChildren().add(btnDel);
        }

        return card;
    }

    @FXML
    private void handlePublier() {
        if (selectedCommunityId < 0) {
            ToastManager.info(getWindow(), "Sélectionnez une communauté avant de publier.");
            return;
        }
        if (forumInput == null || forumInput.getText().isBlank()) {
            ToastManager.info(getWindow(), "Le message ne peut pas être vide.");
            return;
        }

        String text   = forumInput.getText().trim();
        int    commId = selectedCommunityId;
        String name   = SessionManager.getInstance().getFullName();

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                try (Connection conn = ConnectionManager.getConnection()) {
                    int memberId;
                    try (PreparedStatement stmt = conn.prepareStatement(
                            "SELECT member_id FROM community_member WHERE full_name = ?")) {
                        stmt.setString(1, name);
                        try (ResultSet rs = stmt.executeQuery()) {
                            if (!rs.next()) throw new SQLException("Membre introuvable : " + name);
                            memberId = rs.getInt("member_id");
                        }
                    }
                    try (PreparedStatement stmt = conn.prepareStatement(
                            "INSERT INTO community_post (community_id, member_id, content) VALUES (?, ?, ?)")) {
                        stmt.setInt(1, commId);
                        stmt.setInt(2, memberId);
                        stmt.setString(3, text);
                        stmt.executeUpdate();
                    }
                }
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            forumInput.clear();
            loadForumMessages(commId);
            ToastManager.success(getWindow(), "Message publié !");
        });

        task.setOnFailed(e ->
                ToastManager.error(getWindow(), "Erreur lors de la publication."));

        Thread t = new Thread(task, "artconnect-forum-post"); t.setDaemon(true); t.start();
    }

    private void deletePost(int postId) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                try (Connection conn = ConnectionManager.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(
                             "DELETE FROM community_post WHERE post_id = ?")) {
                    stmt.setInt(1, postId);
                    stmt.executeUpdate();
                }
                return null;
            }
        };
        task.setOnSucceeded(e -> {
            loadForumMessages(selectedCommunityId);
            ToastManager.success(getWindow(), "Message supprimé.");
        });
        task.setOnFailed(e ->
                ToastManager.error(getWindow(), "Erreur lors de la suppression."));
        Thread t = new Thread(task, "artconnect-forum-delete"); t.setDaemon(true); t.start();
    }

    private javafx.stage.Window getWindow() {
        try {
            if (memberTable != null && memberTable.getScene() != null)
                return memberTable.getScene().getWindow();
        } catch (Exception ignored) {}
        return null;
    }

    private String orDash(String v) { return v == null || v.isBlank() ? "-" : v; }
}