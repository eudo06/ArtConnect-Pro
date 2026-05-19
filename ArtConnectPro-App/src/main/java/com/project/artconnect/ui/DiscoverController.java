package com.project.artconnect.ui;

import com.project.artconnect.model.Artist;
import com.project.artconnect.model.Exhibition;
import com.project.artconnect.model.Workshop;
import com.project.artconnect.persistence.JdbcWorkshopDao;
import com.project.artconnect.service.ArtistService;
import com.project.artconnect.service.ExhibitionService;
import com.project.artconnect.service.WorkshopService;
import com.project.artconnect.util.ServiceProvider;
import com.project.artconnect.util.SessionManager;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class DiscoverController {

    @FXML private FlowPane discoverPane;
    @FXML private Label    welcomeLabel;

    private final ExhibitionService exhibitionService = ServiceProvider.getExhibitionService();
    private final WorkshopService   workshopService   = ServiceProvider.getWorkshopService();
    private final ArtistService     artistService     = ServiceProvider.getArtistService();
    private final JdbcWorkshopDao   workshopDao       = new JdbcWorkshopDao();

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    @FXML
    public void initialize() {
        // Message de bienvenue personnalisé selon la session
        SessionManager session = SessionManager.getInstance();
        if (welcomeLabel != null && session.isLoggedIn()) {
            welcomeLabel.setText("Bienvenue, " + session.getFullName()
                    + " \u2014 " + session.getRoleLabel());
        }

        // Chargement des données en arrière-plan
        Task<Void> loadTask = new Task<>() {
            @Override
            protected Void call() {
                List<Exhibition> exhibitions = exhibitionService.getAllExhibitions();
                List<Workshop>   workshops   = workshopService.getAllWorkshops();
                List<Artist>     artists     = artistService.getAllArtists();
                return null;
            }
        };

        // On charge directement — le Discover est le premier onglet
        loadDiscover();
    }

    private void loadDiscover() {
        Task<Object[]> task = new Task<>() {
            @Override
            protected Object[] call() {
                List<Exhibition> exhibitions = exhibitionService.getAllExhibitions();
                List<Workshop>   workshops   = workshopService.getAllWorkshops();
                List<Artist>     artists     = artistService.getAllArtists();
                return new Object[]{exhibitions, workshops, artists};
            }
        };

        task.setOnSucceeded(e -> {
            Object[] data = (Object[]) task.getValue();
            List<Exhibition> exhibitions = (List<Exhibition>) data[0];
            List<Workshop>   workshops   = (List<Workshop>)   data[1];
            List<Artist>     artists     = (List<Artist>)     data[2];

            discoverPane.getChildren().clear();

            // Section titre expositions
            discoverPane.getChildren().add(sectionLabel("Expositions en cours"));
            exhibitions.stream().limit(3).forEach(ex -> discoverPane.getChildren().add(buildExhibitionCard(ex)));

            // Section titre workshops
            discoverPane.getChildren().add(sectionLabel("Prochains ateliers"));
            workshops.stream().limit(3).forEach(w -> discoverPane.getChildren().add(buildWorkshopCard(w)));

            // Section artistes mis en avant
            discoverPane.getChildren().add(sectionLabel("Artistes a decouvrir"));
            artists.stream().limit(3).forEach(a -> discoverPane.getChildren().add(buildArtistCard(a)));
        });

        task.setOnFailed(e -> System.err.println("Erreur chargement Discover : " + task.getException().getMessage()));

        Thread t = new Thread(task, "artconnect-discover");
        t.setDaemon(true);
        t.start();
    }

    // ------------------------------------------------------------------
    // Cartes
    // ------------------------------------------------------------------

    private VBox buildExhibitionCard(Exhibition ex) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(14));
        card.setPrefWidth(260);
        card.setStyle(
                "-fx-background-color: #FFFFFF;" +
                        "-fx-border-color: #6D2E46;" +
                        "-fx-border-width: 0 0 0 4;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.10), 8, 0, 0, 2);" +
                        "-fx-background-radius: 6;" +
                        "-fx-border-radius: 6;"
        );

        Label badge = new Label("EXPOSITION");
        badge.setStyle("-fx-background-color: #6D2E46; -fx-text-fill: white; -fx-padding: 2 8; -fx-font-size: 10; -fx-background-radius: 3;");

        Label title = new Label(orDash(ex.getTitle()));
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 14; -fx-wrap-text: true;");
        title.setWrapText(true);

        Label gallery = new Label(ex.getGallery() != null ? ex.getGallery().getName() : "Galerie inconnue");
        gallery.setStyle("-fx-text-fill: #666666; -fx-font-size: 12;");

        Label theme = new Label(ex.getTheme() != null ? "Theme : " + ex.getTheme() : "");
        theme.setStyle("-fx-text-fill: #888888; -fx-font-size: 11; -fx-font-style: italic;");

        String dates = "";
        if (ex.getStartDate() != null) dates += "Du " + ex.getStartDate().format(FMT);
        if (ex.getEndDate() != null)   dates += " au " + ex.getEndDate().format(FMT);
        Label dateLabel = new Label(dates);
        dateLabel.setStyle("-fx-text-fill: #6D2E46; -fx-font-size: 11;");

        card.getChildren().addAll(badge, title, gallery, theme, dateLabel);
        return card;
    }

    private VBox buildWorkshopCard(Workshop w) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(14));
        card.setPrefWidth(260);
        card.setStyle(
                "-fx-background-color: #FFFFFF;" +
                        "-fx-border-color: #2D6A4F;" +
                        "-fx-border-width: 0 0 0 4;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.10), 8, 0, 0, 2);" +
                        "-fx-background-radius: 6;" +
                        "-fx-border-radius: 6;"
        );

        Label badge = new Label("ATELIER");
        badge.setStyle("-fx-background-color: #2D6A4F; -fx-text-fill: white; -fx-padding: 2 8; -fx-font-size: 10; -fx-background-radius: 3;");

        Label title = new Label(orDash(w.getTitle()));
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 14; -fx-wrap-text: true;");
        title.setWrapText(true);

        Label instructor = new Label(w.getInstructor() != null ? "Par " + w.getInstructor().getName() : "");
        instructor.setStyle("-fx-text-fill: #666666; -fx-font-size: 12;");

        // Nombre de participants via fn_workshop_participant_count
        int count    = workshopDao.getParticipantCount(w.getEventId());
        int capacity = w.getMaxParticipants();
        Label participants = new Label(count + " / " + capacity + " inscrits");
        participants.setStyle("-fx-text-fill: " + (count >= capacity ? "#B85042" : "#2D6A4F") + "; -fx-font-size: 11; -fx-font-weight: bold;");

        Label price = new Label(String.format("%.2f EUR | %s", w.getPrice(), orDash(w.getLevel())));
        price.setStyle("-fx-text-fill: #888888; -fx-font-size: 11;");

        String dateStr = w.getDate() != null ? w.getDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")) : "-";
        Label dateLabel = new Label(dateStr);
        dateLabel.setStyle("-fx-text-fill: #2D6A4F; -fx-font-size: 11;");

        card.getChildren().addAll(badge, title, instructor, participants, price, dateLabel);
        return card;
    }

    private VBox buildArtistCard(Artist a) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(14));
        card.setPrefWidth(260);
        card.setStyle(
                "-fx-background-color: #FFFFFF;" +
                        "-fx-border-color: #3D5A80;" +
                        "-fx-border-width: 0 0 0 4;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.10), 8, 0, 0, 2);" +
                        "-fx-background-radius: 6;" +
                        "-fx-border-radius: 6;"
        );

        Label badge = new Label("ARTISTE");
        badge.setStyle("-fx-background-color: #3D5A80; -fx-text-fill: white; -fx-padding: 2 8; -fx-font-size: 10; -fx-background-radius: 3;");

        Label name = new Label(orDash(a.getName()));
        name.setStyle("-fx-font-weight: bold; -fx-font-size: 14;");

        Label city = new Label(a.getCity() != null ? a.getCity() : "");
        city.setStyle("-fx-text-fill: #666666; -fx-font-size: 12;");

        String disciplines = a.getDisciplines().isEmpty() ? "Artiste polyvalent"
                : a.getDisciplines().stream()
                .map(d -> d.getName())
                .reduce((x, y) -> x + ", " + y).orElse("");
        Label disciplinesLabel = new Label(disciplines);
        disciplinesLabel.setStyle("-fx-text-fill: #888888; -fx-font-size: 11; -fx-font-style: italic;");
        disciplinesLabel.setWrapText(true);

        String bio = a.getBio() != null && !a.getBio().isBlank()
                ? (a.getBio().length() > 80 ? a.getBio().substring(0, 80) + "..." : a.getBio())
                : "";
        Label bioLabel = new Label(bio);
        bioLabel.setStyle("-fx-text-fill: #555555; -fx-font-size: 11;");
        bioLabel.setWrapText(true);

        card.getChildren().addAll(badge, name, city, disciplinesLabel, bioLabel);
        return card;
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private Label sectionLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #1A1A2E; -fx-padding: 10 0 4 0;");
        label.prefWidthProperty().bind(discoverPane.prefWrapLengthProperty());
        return label;
    }

    private String orDash(String v) { return v == null || v.isBlank() ? "-" : v; }
}