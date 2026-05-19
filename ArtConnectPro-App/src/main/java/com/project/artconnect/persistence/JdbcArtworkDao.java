package com.project.artconnect.persistence;

import com.project.artconnect.dao.ArtworkDao;
import com.project.artconnect.model.Artist;
import com.project.artconnect.model.Artwork;
import com.project.artconnect.model.ArtworkTag;
import com.project.artconnect.util.ConnectionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * Implémentation JDBC du DAO Artwork.
 *
 * <p><strong>Note sur la résolution par titre :</strong> le modèle Java {@link Artwork}
 * ne porte pas d'ID (choix OOP du projet). Les opérations {@code update} et
 * {@code delete} identifient l'œuvre par son {@code title}, qui joue le rôle
 * de clé métier dans l'application. L'UI garantit que le titre provient
 * toujours d'un objet sélectionné dans la TableView (donc existant en base).
 * En cas d'œuvre introuvable, une {@link IllegalArgumentException} est levée
 * avec un message explicite plutôt qu'un échec silencieux.</p>
 */
public class JdbcArtworkDao implements ArtworkDao {

    private static final String SELECT_ARTWORKS = """
            SELECT aw.artwork_id,
                   aw.artist_id,
                   aw.title,
                   aw.creation_year,
                   aw.type,
                   aw.medium,
                   aw.dimensions,
                   aw.description,
                   aw.price,
                   aw.status,
                   a.stage_name,
                   a.city,
                   a.contact_email
            FROM artwork aw
            JOIN artist a ON aw.artist_id = a.artist_id
            """;

    // ------------------------------------------------------------------
    // Lecture
    // ------------------------------------------------------------------

    @Override
    public List<Artwork> findAll() {
        String sql = SELECT_ARTWORKS + " ORDER BY aw.artwork_id";

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            return extractArtworks(rs);
        } catch (SQLException e) {
            throw databaseError("Unable to load artworks from MySQL.", e);
        }
    }

    @Override
    public Artwork findById(int id) {
        String sql = SELECT_ARTWORKS + " WHERE aw.artwork_id = ?";

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                List<Artwork> artworks = extractArtworks(rs);
                return artworks.isEmpty() ? null : artworks.get(0);
            }
        } catch (SQLException e) {
            throw databaseError("Unable to load artwork by id from MySQL.", e);
        }
    }

    @Override
    public List<Artwork> findByArtistName(String artistName) {
        String sql = SELECT_ARTWORKS + " WHERE a.stage_name = ? ORDER BY aw.artwork_id";

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, artistName);
            try (ResultSet rs = stmt.executeQuery()) {
                return extractArtworks(rs);
            }
        } catch (SQLException e) {
            throw databaseError("Unable to load artworks by artist from MySQL.", e);
        }
    }

    // ------------------------------------------------------------------
    // Écriture
    // ------------------------------------------------------------------

    @Override
    public void save(Artwork artwork) {
        String sql = """
                INSERT INTO artwork (
                    artist_id, title, creation_year, type,
                    medium, dimensions, description, price, status
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, resolveArtistId(conn, artwork));
            stmt.setString(2, artwork.getTitle());
            setNullableInteger(stmt, 3, artwork.getCreationYear());
            stmt.setString(4, artwork.getType());
            stmt.setString(5, artwork.getMedium());
            stmt.setString(6, artwork.getDimensions());
            stmt.setString(7, artwork.getDescription());
            stmt.setDouble(8, artwork.getPrice());
            stmt.setString(9, statusName(artwork));
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw databaseError("Unable to save artwork in MySQL.", e);
        }
    }

    @Override
    public void update(Artwork artwork) {
        // Le modèle Java n'a pas d'ID : on résout l'œuvre par title.
        // resolveArtworkId() lève IllegalArgumentException si introuvable,
        // évitant toute mise à jour silencieuse sur un mauvais enregistrement.
        String sql = """
                UPDATE artwork
                SET artist_id = ?, creation_year = ?, type = ?, medium = ?,
                    dimensions = ?, description = ?, price = ?, status = ?
                WHERE artwork_id = ?
                """;

        try (Connection conn = ConnectionManager.getConnection()) {
            long artworkId = resolveArtworkId(conn, artwork.getTitle());

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, resolveArtistId(conn, artwork));
                setNullableInteger(stmt, 2, artwork.getCreationYear());
                stmt.setString(3, artwork.getType());
                stmt.setString(4, artwork.getMedium());
                stmt.setString(5, artwork.getDimensions());
                stmt.setString(6, artwork.getDescription());
                stmt.setDouble(7, artwork.getPrice());
                stmt.setString(8, statusName(artwork));
                stmt.setLong(9, artworkId);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            throw databaseError("Unable to update artwork in MySQL.", e);
        }
    }

    @Override
    public void delete(int artworkId) {
        String sql = "DELETE FROM artwork WHERE artwork_id = ?";

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, artworkId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw databaseError("Unable to delete artwork from MySQL.", e);
        }
    }

    public void deleteByTitle(String title) {
        // Méthode appelée par l'UI (qui passe un titre depuis la sélection).
        // resolveArtworkId() vérifie l'existence avant de supprimer.
        try (Connection conn = ConnectionManager.getConnection()) {
            long artworkId;
            try {
                artworkId = resolveArtworkId(conn, title);
            } catch (IllegalArgumentException e) {
                // L'œuvre n'existe plus en base : rien à supprimer.
                return;
            }
            try (PreparedStatement stmt = conn.prepareStatement(
                    "DELETE FROM artwork WHERE artwork_id = ?")) {
                stmt.setLong(1, artworkId);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            throw databaseError("Unable to delete artwork from MySQL by title.", e);
        }
    }

    // ------------------------------------------------------------------
    // Helpers — résolution d'IDs
    // ------------------------------------------------------------------

    /**
     * Résout l'artwork_id depuis le titre de l'œuvre.
     *
     * @throws IllegalArgumentException si aucune œuvre ne correspond au titre,
     *         avec un message explicite pour l'UI.
     */
    private long resolveArtworkId(Connection conn, String title) throws SQLException {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Le titre de l'œuvre ne peut pas être vide.");
        }
        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT artwork_id FROM artwork WHERE title = ?")) {
            stmt.setString(1, title);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getLong("artwork_id");
            }
        }
        throw new IllegalArgumentException(
                "Œuvre introuvable en base : \"" + title + "\". "
                        + "Elle a peut-être été supprimée entre-temps.");
    }

    /**
     * Résout l'artist_id depuis le stage_name de l'artiste associé à l'œuvre.
     *
     * @throws SQLException si l'artiste référencé n'existe pas en base.
     */
    private long resolveArtistId(Connection conn, Artwork artwork) throws SQLException {
        if (artwork.getArtist() == null
                || artwork.getArtist().getName() == null
                || artwork.getArtist().getName().isBlank()) {
            throw new SQLException("L'œuvre doit référencer un artiste valide.");
        }
        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT artist_id FROM artist WHERE stage_name = ?")) {
            stmt.setString(1, artwork.getArtist().getName());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getLong("artist_id");
            }
        }
        throw new SQLException("Artiste inconnu pour cette œuvre : " + artwork.getArtist().getName());
    }

    // ------------------------------------------------------------------
    // Helpers — mapping ResultSet
    // ------------------------------------------------------------------

    private List<Artwork> extractArtworks(ResultSet rs) throws SQLException {
        List<Artwork> artworks = new ArrayList<>();
        while (rs.next()) {
            artworks.add(mapArtwork(rs));
        }
        return artworks;
    }

    private Artwork mapArtwork(ResultSet rs) throws SQLException {
        Artwork artwork = new Artwork();
        artwork.setTitle(rs.getString("title"));
        int creationYear = rs.getInt("creation_year");
        artwork.setCreationYear(rs.wasNull() ? null : creationYear);
        artwork.setType(rs.getString("type"));
        artwork.setMedium(rs.getString("medium"));
        artwork.setDimensions(rs.getString("dimensions"));
        artwork.setDescription(rs.getString("description"));
        artwork.setPrice(rs.getDouble("price"));
        artwork.setStatus(parseStatus(rs.getString("status")));
        artwork.setArtist(mapArtist(rs));
        return artwork;
    }

    private Artist mapArtist(ResultSet rs) throws SQLException {
        Artist artist = new Artist();
        artist.setName(rs.getString("stage_name"));
        artist.setCity(rs.getString("city"));
        artist.setContactEmail(rs.getString("contact_email"));
        return artist;
    }

    // ------------------------------------------------------------------
    // Helpers — utilitaires
    // ------------------------------------------------------------------

    private Artwork.Status parseStatus(String rawStatus) {
        if (rawStatus == null || rawStatus.isBlank()) return Artwork.Status.FOR_SALE;
        try {
            return Artwork.Status.valueOf(rawStatus);
        } catch (IllegalArgumentException ex) {
            return Artwork.Status.FOR_SALE;
        }
    }

    private String statusName(Artwork artwork) {
        return artwork.getStatus() == null
                ? Artwork.Status.FOR_SALE.name()
                : artwork.getStatus().name();
    }

    private void setNullableInteger(PreparedStatement stmt, int index, Integer value) throws SQLException {
        if (value == null) stmt.setNull(index, Types.INTEGER);
        else stmt.setInt(index, value);
    }

    /**
     * Appelle fn_artwork_average_rating(artwork_id) en base via le titre.
     * Retourne la note moyenne (0 si aucun avis).
     */
    public double getAverageRating(String title) {
        String sql = """
                SELECT fn_artwork_average_rating(artwork_id) AS avg_rating
                FROM artwork
                WHERE title = ?
                """;
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, title);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getDouble("avg_rating") : 0.0;
            }
        } catch (SQLException e) {
            return 0.0;
        }
    }

    // ------------------------------------------------------------------
    // Tags
    // ------------------------------------------------------------------

    /**
     * Retourne tous les tags distincts de la base.
     */
    public List<ArtworkTag> findAllTags() {
        String sql = "SELECT name FROM artwork_tag ORDER BY name";
        List<ArtworkTag> tags = new ArrayList<>();
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) tags.add(new ArtworkTag(rs.getString("name")));
        } catch (SQLException e) {
            throw databaseError("Unable to load tags.", e);
        }
        return tags;
    }

    /**
     * Retourne les oeuvres qui ont le tag donné.
     */
    public List<Artwork> findByTag(String tagName) {
        String sql = SELECT_ARTWORKS + """
                JOIN artwork_tag_map atm ON aw.artwork_id = atm.artwork_id
                JOIN artwork_tag at ON atm.tag_id = at.tag_id
                WHERE at.name = ?
                ORDER BY aw.artwork_id
                """;
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, tagName);
            try (ResultSet rs = stmt.executeQuery()) {
                return extractArtworks(rs);
            }
        } catch (SQLException e) {
            throw databaseError("Unable to load artworks by tag.", e);
        }
    }

    /**
     * Retourne les tags d'une oeuvre identifiée par son titre.
     */
    public List<ArtworkTag> findTagsByArtworkTitle(String title) {
        String sql = """
                SELECT at.name
                FROM artwork_tag at
                JOIN artwork_tag_map atm ON at.tag_id = atm.tag_id
                JOIN artwork aw ON atm.artwork_id = aw.artwork_id
                WHERE aw.title = ?
                ORDER BY at.name
                """;
        List<ArtworkTag> tags = new ArrayList<>();
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, title);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) tags.add(new ArtworkTag(rs.getString("name")));
            }
        } catch (SQLException e) {
            throw databaseError("Unable to load tags for artwork.", e);
        }
        return tags;
    }

    private IllegalStateException databaseError(String message, Exception cause) {
        return new IllegalStateException(message, cause);
    }
}