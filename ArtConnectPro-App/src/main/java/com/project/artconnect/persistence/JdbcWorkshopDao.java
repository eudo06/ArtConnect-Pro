package com.project.artconnect.persistence;

import com.project.artconnect.dao.WorkshopDao;
import com.project.artconnect.model.Artist;
import com.project.artconnect.model.Workshop;
import com.project.artconnect.util.ConnectionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcWorkshopDao implements WorkshopDao {

    private static final String SELECT_WORKSHOPS = """
            SELECT w.event_id,
                   e.title,
                   e.start_datetime,
                   e.capacity,
                   e.location,
                   e.description,
                   w.duration_minutes,
                   w.price,
                   w.level,
                   a.stage_name
            FROM workshop w
            JOIN event e ON w.event_id = e.event_id
            JOIN artist a ON w.instructor_artist_id = a.artist_id
            """;

    @Override
    public Optional<Workshop> findById(Long id) {
        String sql = SELECT_WORKSHOPS + " WHERE w.event_id = ?";
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(mapWorkshop(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw databaseError("Unable to load workshop by id.", e);
        }
    }

    @Override
    public List<Workshop> findAll() {
        String sql = SELECT_WORKSHOPS + " ORDER BY e.start_datetime";
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            List<Workshop> workshops = new ArrayList<>();
            while (rs.next()) workshops.add(mapWorkshop(rs));
            return workshops;
        } catch (SQLException e) {
            throw databaseError("Unable to load workshops from MySQL.", e);
        }
    }

    public Optional<Workshop> findByTitle(String title) {
        String sql = SELECT_WORKSHOPS + " WHERE e.title = ?";
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, title);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(mapWorkshop(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw databaseError("Unable to load workshop by title.", e);
        }
    }

    /**
     * Appelle fn_workshop_participant_count(event_id) en base.
     * Retourne le nombre d'inscrits à un workshop.
     */
    public int getParticipantCount(long eventId) {
        String sql = "SELECT fn_workshop_participant_count(?) AS cnt";
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, eventId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt("cnt") : 0;
            }
        } catch (SQLException e) {
            return 0; // En cas d'erreur, on retourne 0 sans crasher
        }
    }

    private Workshop mapWorkshop(ResultSet rs) throws SQLException {
        Workshop workshop = new Workshop();
        workshop.setEventId(rs.getLong("event_id")); // ← eventId mappé
        workshop.setTitle(rs.getString("title"));
        Timestamp start = rs.getTimestamp("start_datetime");
        workshop.setDate(start == null ? null : start.toLocalDateTime());
        workshop.setMaxParticipants(rs.getInt("capacity"));
        workshop.setLocation(rs.getString("location"));
        workshop.setDescription(rs.getString("description"));
        workshop.setDurationMinutes(rs.getInt("duration_minutes"));
        workshop.setPrice(rs.getDouble("price"));
        workshop.setLevel(rs.getString("level"));

        Artist artist = new Artist();
        artist.setName(rs.getString("stage_name"));
        workshop.setInstructor(artist);
        return workshop;
    }

    private IllegalStateException databaseError(String message, Exception cause) {
        return new IllegalStateException(message, cause);
    }
}