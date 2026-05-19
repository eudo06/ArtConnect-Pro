package com.project.artconnect.persistence;

import com.project.artconnect.dao.ExhibitionDao;
import com.project.artconnect.model.Artist;
import com.project.artconnect.model.Artwork;
import com.project.artconnect.model.Exhibition;
import com.project.artconnect.model.Gallery;
import com.project.artconnect.util.ConnectionManager;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class JdbcExhibitionDao implements ExhibitionDao {

    private static final String SELECT_EXHIBITIONS = """
            SELECT e.event_id,
                   e.title,
                   e.description,
                   e.start_datetime,
                   e.end_datetime,
                   ex.theme,
                   ex.curator_name,
                   g.name AS gallery_name,
                   g.address,
                   g.owner_name,
                   g.opening_hours,
                   g.contact_phone,
                   g.rating,
                   g.website,
                   aw.title AS artwork_title,
                   aw.creation_year,
                   aw.type,
                   aw.medium,
                   aw.dimensions,
                   aw.description AS artwork_description,
                   aw.price,
                   aw.status,
                   ar.stage_name AS artist_name
            FROM exhibition ex
            JOIN event e ON ex.event_id = e.event_id
            JOIN gallery g ON ex.gallery_id = g.gallery_id
            LEFT JOIN exhibition_artwork ea ON ex.event_id = ea.exhibition_event_id
            LEFT JOIN artwork aw ON ea.artwork_id = aw.artwork_id
            LEFT JOIN artist ar ON aw.artist_id = ar.artist_id
            """;

    @Override
    public List<Exhibition> findAll() {
        String sql = SELECT_EXHIBITIONS + " ORDER BY e.start_datetime";

        try (Connection conn = ConnectionManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {
            return extractExhibitions(rs);
        } catch (SQLException e) {
            throw databaseError("Unable to load exhibitions from MySQL.", e);
        }
    }

    @Override
    public void save(Exhibition exhibition) {
        String selectPromoterId = "SELECT promoter_id FROM promoter ORDER BY promoter_id LIMIT 1";
        String selectGalleryId = "SELECT gallery_id FROM gallery WHERE name = ?";
        String insertEvent = """
                INSERT INTO event (promoter_id, title, description, start_datetime, end_datetime, status, capacity, location)
                VALUES (?, ?, ?, ?, ?, 'PLANNED', NULL, ?)
                """;
        String insertExhibition = """
                INSERT INTO exhibition (event_id, gallery_id, theme, curator_name)
                VALUES (?, ?, ?, ?)
                """;

        try (Connection conn = ConnectionManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                long promoterId = lookupRequiredId(conn, selectPromoterId, null, "promoter_id");
                long galleryId = lookupRequiredId(conn, selectGalleryId, exhibition.getGallery() == null ? null : exhibition.getGallery().getName(), "gallery_id");

                long eventId;
                try (PreparedStatement eventStmt = conn.prepareStatement(insertEvent, Statement.RETURN_GENERATED_KEYS)) {
                    eventStmt.setLong(1, promoterId);
                    eventStmt.setString(2, exhibition.getTitle());
                    eventStmt.setString(3, exhibition.getDescription());
                    eventStmt.setTimestamp(4, exhibition.getStartDate() == null ? null : Timestamp.valueOf(exhibition.getStartDate().atStartOfDay()));
                    eventStmt.setTimestamp(5, exhibition.getEndDate() == null ? null : Timestamp.valueOf(exhibition.getEndDate().atStartOfDay()));
                    eventStmt.setString(6, exhibition.getGallery() == null ? null : exhibition.getGallery().getAddress());
                    eventStmt.executeUpdate();
                    try (ResultSet keys = eventStmt.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException("No event id generated for exhibition insert.");
                        }
                        eventId = keys.getLong(1);
                    }
                }

                try (PreparedStatement exhibitionStmt = conn.prepareStatement(insertExhibition)) {
                    exhibitionStmt.setLong(1, eventId);
                    exhibitionStmt.setLong(2, galleryId);
                    exhibitionStmt.setString(3, exhibition.getTheme());
                    exhibitionStmt.setString(4, exhibition.getCuratorName());
                    exhibitionStmt.executeUpdate();
                }

                syncExhibitionArtworks(conn, eventId, exhibition.getArtworks());
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw databaseError("Unable to save exhibition in MySQL.", e);
        }
    }

    @Override
    public void update(Exhibition exhibition) {
        String selectIds = """
                SELECT ex.event_id, ex.gallery_id
                FROM exhibition ex
                JOIN event e ON ex.event_id = e.event_id
                WHERE e.title = ?
                """;
        String selectGalleryId = "SELECT gallery_id FROM gallery WHERE name = ?";
        String updateEvent = """
                UPDATE event
                SET description = ?, start_datetime = ?, end_datetime = ?, location = ?
                WHERE event_id = ?
                """;
        String updateExhibition = """
                UPDATE exhibition
                SET gallery_id = ?, theme = ?, curator_name = ?
                WHERE event_id = ?
                """;

        try (Connection conn = ConnectionManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Long eventId = null;
                try (PreparedStatement selectStmt = conn.prepareStatement(selectIds)) {
                    selectStmt.setString(1, exhibition.getTitle());
                    try (ResultSet rs = selectStmt.executeQuery()) {
                        if (rs.next()) {
                            eventId = rs.getLong("event_id");
                        }
                    }
                }

                if (eventId == null) {
                    throw new SQLException("Exhibition not found for update: " + exhibition.getTitle());
                }

                long galleryId = lookupRequiredId(conn, selectGalleryId, exhibition.getGallery() == null ? null : exhibition.getGallery().getName(), "gallery_id");

                try (PreparedStatement eventStmt = conn.prepareStatement(updateEvent)) {
                    eventStmt.setString(1, exhibition.getDescription());
                    eventStmt.setTimestamp(2, exhibition.getStartDate() == null ? null : Timestamp.valueOf(exhibition.getStartDate().atStartOfDay()));
                    eventStmt.setTimestamp(3, exhibition.getEndDate() == null ? null : Timestamp.valueOf(exhibition.getEndDate().atStartOfDay()));
                    eventStmt.setString(4, exhibition.getGallery() == null ? null : exhibition.getGallery().getAddress());
                    eventStmt.setLong(5, eventId);
                    eventStmt.executeUpdate();
                }

                try (PreparedStatement exhibitionStmt = conn.prepareStatement(updateExhibition)) {
                    exhibitionStmt.setLong(1, galleryId);
                    exhibitionStmt.setString(2, exhibition.getTheme());
                    exhibitionStmt.setString(3, exhibition.getCuratorName());
                    exhibitionStmt.setLong(4, eventId);
                    exhibitionStmt.executeUpdate();
                }

                syncExhibitionArtworks(conn, eventId, exhibition.getArtworks());
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw databaseError("Unable to update exhibition in MySQL.", e);
        }
    }

    @Override
    public void delete(String title) {
        String sql = """
                DELETE e, ex
                FROM event e
                JOIN exhibition ex ON e.event_id = ex.event_id
                WHERE e.title = ?
                """;

        try (Connection conn = ConnectionManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, title);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw databaseError("Unable to delete exhibition from MySQL.", e);
        }
    }

    private List<Exhibition> extractExhibitions(ResultSet rs) throws SQLException {
        Map<String, Exhibition> exhibitionsByTitle = new LinkedHashMap<>();

        while (rs.next()) {
            String title = rs.getString("title");
            Exhibition exhibition = exhibitionsByTitle.computeIfAbsent(title, ignored -> mapExhibition(rs));

            String artworkTitle = rs.getString("artwork_title");
            if (artworkTitle != null && exhibition.getArtworks().stream().noneMatch(a -> artworkTitle.equalsIgnoreCase(a.getTitle()))) {
                exhibition.getArtworks().add(mapArtwork(rs));
            }
        }

        return new ArrayList<>(exhibitionsByTitle.values());
    }

    private Exhibition mapExhibition(ResultSet rs) {
        try {
            Exhibition exhibition = new Exhibition();
            exhibition.setTitle(rs.getString("title"));
            Timestamp start = rs.getTimestamp("start_datetime");
            exhibition.setStartDate(start == null ? null : start.toLocalDateTime().toLocalDate());
            Timestamp end = rs.getTimestamp("end_datetime");
            exhibition.setEndDate(end == null ? null : end.toLocalDateTime().toLocalDate());
            exhibition.setDescription(rs.getString("description"));
            exhibition.setTheme(rs.getString("theme"));
            exhibition.setCuratorName(rs.getString("curator_name"));
            exhibition.setGallery(mapGallery(rs));
            return exhibition;
        } catch (SQLException e) {
            throw databaseError("Unable to map exhibition row.", e);
        }
    }

    private Gallery mapGallery(ResultSet rs) throws SQLException {
        Gallery gallery = new Gallery();
        gallery.setName(rs.getString("gallery_name"));
        gallery.setAddress(rs.getString("address"));
        gallery.setOwnerName(rs.getString("owner_name"));
        gallery.setOpeningHours(rs.getString("opening_hours"));
        gallery.setContactPhone(rs.getString("contact_phone"));
        gallery.setRating(rs.getDouble("rating"));
        gallery.setWebsite(rs.getString("website"));
        return gallery;
    }

    private Artwork mapArtwork(ResultSet rs) throws SQLException {
        Artwork artwork = new Artwork();
        artwork.setTitle(rs.getString("artwork_title"));
        int creationYear = rs.getInt("creation_year");
        artwork.setCreationYear(rs.wasNull() ? null : creationYear);
        artwork.setType(rs.getString("type"));
        artwork.setMedium(rs.getString("medium"));
        artwork.setDimensions(rs.getString("dimensions"));
        artwork.setDescription(rs.getString("artwork_description"));
        artwork.setPrice(rs.getDouble("price"));
        artwork.setStatus(parseStatus(rs.getString("status")));

        Artist artist = new Artist();
        artist.setName(rs.getString("artist_name"));
        artwork.setArtist(artist);
        return artwork;
    }

    private Artwork.Status parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return Artwork.Status.FOR_SALE;
        }
        try {
            return Artwork.Status.valueOf(status);
        } catch (IllegalArgumentException e) {
            return Artwork.Status.FOR_SALE;
        }
    }

    private long lookupRequiredId(Connection conn, String sql, String parameter, String columnName) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (parameter != null) {
                stmt.setString(1, parameter);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(columnName);
                }
            }
        }
        throw new SQLException("Unable to resolve required id for column " + columnName + ".");
    }

    private void syncExhibitionArtworks(Connection conn, long eventId, List<Artwork> artworks) throws SQLException {
        try (PreparedStatement deleteStmt = conn.prepareStatement(
                "DELETE FROM exhibition_artwork WHERE exhibition_event_id = ?")) {
            deleteStmt.setLong(1, eventId);
            deleteStmt.executeUpdate();
        }

        if (artworks == null) {
            return;
        }

        String findArtworkId = "SELECT artwork_id FROM artwork WHERE title = ?";
        String insertArtworkLink = """
                INSERT INTO exhibition_artwork (exhibition_event_id, artwork_id)
                VALUES (?, ?)
                """;

        for (Artwork artwork : artworks) {
            if (artwork == null || artwork.getTitle() == null || artwork.getTitle().isBlank()) {
                continue;
            }

            Long artworkId = null;
            try (PreparedStatement findStmt = conn.prepareStatement(findArtworkId)) {
                findStmt.setString(1, artwork.getTitle());
                try (ResultSet rs = findStmt.executeQuery()) {
                    if (rs.next()) {
                        artworkId = rs.getLong("artwork_id");
                    }
                }
            }

            if (artworkId == null) {
                continue;
            }

            try (PreparedStatement insertStmt = conn.prepareStatement(insertArtworkLink)) {
                insertStmt.setLong(1, eventId);
                insertStmt.setLong(2, artworkId);
                insertStmt.executeUpdate();
            }
        }
    }

    private IllegalStateException databaseError(String message, Exception cause) {
        return new IllegalStateException(message, cause);
    }
}
