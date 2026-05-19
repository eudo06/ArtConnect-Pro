package com.project.artconnect.ui;

import com.project.artconnect.util.ServiceProvider;
import com.project.artconnect.util.SessionManager;
import com.project.artconnect.util.SessionManager.Role;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainController {

    @FXML private Label     sidebarUserName;
    @FXML private Label     sidebarUserRole;
    @FXML private Label     sidebarModeLabel;
    @FXML private Label     modeLabel;

    @FXML private Button navDiscover;
    @FXML private Button navArtists;
    @FXML private Button navArtworks;
    @FXML private Button navGalleries;
    @FXML private Button navExhibitions;
    @FXML private Button navWorkshops;
    @FXML private Button navCommunity;
    @FXML private Button navInscriptions;
    @FXML private Button navAdmin;

    @FXML private StackPane contentStack;
    @FXML private Node discoverView;
    @FXML private Node artistsView;
    @FXML private Node artworksView;
    @FXML private Node galleriesView;
    @FXML private Node exhibitionsView;
    @FXML private Node workshopsView;
    @FXML private Node communityView;
    @FXML private Node inscriptionsView;
    @FXML private Node adminView;

    private List<Button> allNavButtons;

    @FXML
    public void initialize() {
        SessionManager session = SessionManager.getInstance();

        sidebarUserName.setText(session.getFullName() != null ? session.getFullName() : "—");
        sidebarUserRole.setText(session.getRoleLabel());
        sidebarModeLabel.setText("Mode : " + ServiceProvider.getPersistenceModeLabel());
        modeLabel.setText("ArtConnect Pro v1.0  |  " + ServiceProvider.getPersistenceModeLabel()
                + "  |  " + session.getFullName());

        allNavButtons = new ArrayList<>();
        addIfNotNull(allNavButtons, navDiscover, navArtists, navArtworks, navGalleries,
                navExhibitions, navWorkshops, navCommunity, navInscriptions, navAdmin);

        // Masquer tout sauf Discover au départ
        contentStack.getChildren().forEach(n -> n.setVisible(false));
        if (discoverView != null) discoverView.setVisible(true);

        applyRoleRestrictions(session.getRole());
        setActive(navDiscover);
    }

    // ------------------------------------------------------------------
    // Navigation — chaque bouton affiche sa vue
    // ------------------------------------------------------------------

    @FXML private void showDiscover()     { showView(discoverView,     navDiscover); }
    @FXML private void showArtists()      { showView(artistsView,      navArtists); }
    @FXML private void showArtworks()     { showView(artworksView,     navArtworks); }
    @FXML private void showGalleries()    { showView(galleriesView,    navGalleries); }
    @FXML private void showExhibitions()  { showView(exhibitionsView,  navExhibitions); }
    @FXML private void showWorkshops()    { showView(workshopsView,    navWorkshops); }
    @FXML private void showCommunity()    { showView(communityView,    navCommunity); }
    @FXML private void showInscriptions() { showView(inscriptionsView, navInscriptions); }
    @FXML private void showAdmin()        { showView(adminView,        navAdmin); }

    private void showView(Node view, Button btn) {
        contentStack.getChildren().forEach(n -> n.setVisible(false));
        if (view != null) view.setVisible(true);
        setActive(btn);
    }

    private void setActive(Button active) {
        allNavButtons.forEach(b -> b.getStyleClass().remove("nav-button-active"));
        if (active != null && !active.getStyleClass().contains("nav-button-active"))
            active.getStyleClass().add("nav-button-active");
    }

    // ------------------------------------------------------------------
    // Restrictions par rôle
    // ------------------------------------------------------------------

    private void applyRoleRestrictions(Role role) {
        // Masquer Inscriptions et Admin par défaut
        hideNav(navInscriptions);
        hideNav(navAdmin);

        if (role == null) { hideAllNav(); return; }

        if (role == Role.ADMIN) {
            showNav(navAdmin);
        } else if (role == Role.PROMOTER) {
            hideNav(navCommunity);
        } else if (role == Role.ARTIST) {
            hideNav(navCommunity);
        } else if (role == Role.MEMBER) {
            showNav(navInscriptions);
        } else if (role == Role.GUEST_MEMBER) {
            hideNav(navWorkshops);
            hideNav(navCommunity);
        }
    }

    private void hideNav(Button btn) {
        if (btn == null) return;
        btn.setVisible(false);
        btn.setManaged(false);
        allNavButtons.remove(btn);
    }

    private void showNav(Button btn) {
        if (btn == null) return;
        btn.setVisible(true);
        btn.setManaged(true);
        if (!allNavButtons.contains(btn)) allNavButtons.add(btn);
    }

    private void hideAllNav() {
        new ArrayList<>(allNavButtons).forEach(this::hideNav);
    }

    private void addIfNotNull(List<Button> list, Button... btns) {
        for (Button b : btns) if (b != null) list.add(b);
    }

    // ------------------------------------------------------------------
    // Déconnexion
    // ------------------------------------------------------------------

    @FXML
    private void handleLogout() {
        SessionManager.getInstance().closeSession();
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/project/artconnect/ui/LoginView.fxml"));
            Scene scene = new Scene(loader.load(), 600, 500);
            Stage stage = (Stage) contentStack.getScene().getWindow();
            stage.setTitle("ArtConnect Pro — Connexion");
            stage.setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleExit() { Platform.exit(); }
}