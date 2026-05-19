package com.project.artconnect.persistence;

import com.project.artconnect.dao.ArtistDao;
import com.project.artconnect.model.Artist;
import com.project.artconnect.model.Discipline;
import com.project.artconnect.util.ConnectionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class JdbcArtistDao implements ArtistDao {

    private static final String SELECT_ARTISTS_WITH_DISCIPLINES = """
            SELECT a.artist_id,
                   a.account_id,
                   a.stage_name,
                   a.bio,
                   a.birth_year,
                   a.contact_email,
                   a.phone,
                   a.city,
                   a.website,
                   a.social_media,
                   a.is_active,
                   d.name AS discipline_name
            FROM artist a
            LEFT JOIN artist_discipline ad ON a.artist_id = ad.artist_id
            LEFT JOIN discipline d ON ad.discipline_id = d.discipline_id
            """;

    @Override
    public List<Artist> findAll() {
        String sql = SELECT_ARTISTS_WITH_DISCIPLINES + " ORDER BY a.artist_id";
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            return extractArtists(rs);
        } catch (SQLException e) {
            throw databaseError("Unable to load artists from MySQL.", e);
        }
    }

    /**
     * Recherche ciblée par nom — évite un findAll() complet.
     * Utilisé par getArtistByName() dans le service après un save()
     * pour sélectionner l'artiste dans la TableView sans recharger toute la liste.
     */
    @Override
    public Optional<Artist> findByName(String name) {
        if (name == null || name.isBlank()) return Optional.empty();
        String sql = SELECT_ARTISTS_WITH_DISCIPLINES + " WHERE a.stage_name = ?";
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            try (ResultSet rs = stmt.executeQuery()) {
                List<Artist> results = extractArtists(rs);
                return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
            }
        } catch (SQLException e) {
            throw databaseError("Unable to load artist by name from MySQL.", e);
        }
    }

    @Override
    public List<Artist> findByCity(String city) {
        String sql = SELECT_ARTISTS_WITH_DISCIPLINES + " WHERE a.city = ? ORDER BY a.artist_id";
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, city);
            try (ResultSet rs = stmt.executeQuery()) {
                return extractArtists(rs);
            }
        } catch (SQLException e) {
            throw databaseError("Unable to load artists by city from MySQL.", e);
        }
    }

    @Override
    public void save(Artist artist) {
        String insertAccount = """
                INSERT INTO account (email, password_hash, account_status)
                VALUES (?, ?, ?)
                """;
        String insertArtist = """
                INSERT INTO artist (
                    account_id, stage_name, bio, birth_year,
                    contact_email, phone, city, website, social_media, is_active
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = ConnectionManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                long accountId = insertAccount(conn, insertAccount, resolveEmail(artist));
                long artistId  = insertArtist(conn, insertArtist, artist, accountId);
                syncDisciplines(conn, artistId, artist.getDisciplines());
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw databaseError("Unable to save artist in MySQL.", e);
        }
    }

    @Override
    public void update(Artist artist) {
        String selectIds    = "SELECT artist_id, account_id FROM artist WHERE stage_name = ?";
        String updateArtist = """
                UPDATE artist
                SET bio = ?, birth_year = ?, contact_email = ?,
                    phone = ?, city = ?, website = ?, social_media = ?, is_active = ?
                WHERE artist_id = ?
                """;
        String updateAccount = "UPDATE account SET email = ? WHERE account_id = ?";
        try (Connection conn = ConnectionManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                long[] ids = resolveArtistIds(conn, selectIds, artist.getName());
                try (PreparedStatement stmt = conn.prepareStatement(updateAccount)) {
                    stmt.setString(1, resolveEmail(artist));
                    stmt.setLong(2, ids[1]);
                    stmt.executeUpdate();
                }
                try (PreparedStatement stmt = conn.prepareStatement(updateArtist)) {
                    stmt.setString(1, artist.getBio());
                    setNullableInteger(stmt, 2, artist.getBirthYear());
                    stmt.setString(3, artist.getContactEmail());
                    stmt.setString(4, artist.getPhone());
                    stmt.setString(5, artist.getCity());
                    stmt.setString(6, artist.getWebsite());
                    stmt.setString(7, artist.getSocialMedia());
                    stmt.setBoolean(8, artist.isActive());
                    stmt.setLong(9, ids[0]);
                    stmt.executeUpdate();
                }
                syncDisciplines(conn, ids[0], artist.getDisciplines());
                conn.commit();
            } catch (SQLException | IllegalArgumentException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw databaseError("Unable to update artist in MySQL.", e);
        }
    }

    @Override
    public void delete(String artistName) {
        String selectIds     = "SELECT artist_id, account_id FROM artist WHERE stage_name = ?";
        String deleteArtist  = "DELETE FROM artist WHERE artist_id = ?";
        String deleteAccount = "DELETE FROM account WHERE account_id = ?";
        try (Connection conn = ConnectionManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                long[] ids;
                try {
                    ids = resolveArtistIds(conn, selectIds, artistName);
                } catch (IllegalArgumentException e) {
                    conn.rollback();
                    return;
                }
                try (PreparedStatement stmt = conn.prepareStatement(deleteArtist)) {
                    stmt.setLong(1, ids[0]);
                    stmt.executeUpdate();
                }
                try (PreparedStatement stmt = conn.prepareStatement(deleteAccount)) {
                    stmt.setLong(1, ids[1]);
                    stmt.executeUpdate();
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw databaseError("Unable to delete artist from MySQL.", e);
        }
    }

    private long[] resolveArtistIds(Connection conn, String sql, String artistName) throws SQLException {
        if (artistName == null || artistName.isBlank())
            throw new IllegalArgumentException("Le nom de l'artiste ne peut pas être vide.");
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, artistName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return new long[]{rs.getLong("artist_id"), rs.getLong("account_id")};
            }
        }
        throw new IllegalArgumentException(
                "Artiste introuvable en base : \"" + artistName + "\". "
                        + "Il a peut-être été supprimé entre-temps.");
    }

    private List<Artist> extractArtists(ResultSet rs) throws SQLException {
        Map<Long, Artist> artistsById = new LinkedHashMap<>();
        while (rs.next()) {
            long artistId = rs.getLong("artist_id");
            Artist artist = artistsById.computeIfAbsent(artistId, ignored -> mapArtist(rs));
            String disciplineName = rs.getString("discipline_name");
            if (disciplineName != null
                    && artist.getDisciplines().stream()
                    .noneMatch(d -> disciplineName.equalsIgnoreCase(d.getName()))) {
                artist.getDisciplines().add(new Discipline(disciplineName));
            }
        }
        return new ArrayList<>(artistsById.values());
    }

    private Artist mapArtist(ResultSet rs) {
        try {
            Artist artist = new Artist();
            artist.setName(rs.getString("stage_name"));
            artist.setBio(rs.getString("bio"));
            int birthYear = rs.getInt("birth_year");
            artist.setBirthYear(rs.wasNull() ? null : birthYear);
            artist.setContactEmail(rs.getString("contact_email"));
            artist.setPhone(rs.getString("phone"));
            artist.setCity(rs.getString("city"));
            artist.setWebsite(rs.getString("website"));
            artist.setSocialMedia(rs.getString("social_media"));
            artist.setActive(rs.getBoolean("is_active"));
            return artist;
        } catch (SQLException e) {
            throw databaseError("Unable to map artist row.", e);
        }
    }

    private long insertAccount(Connection conn, String sql, String email) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, email);
            stmt.setString(2, "jdbc-placeholder");
            stmt.setString(3, "ACTIVE");
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("No account id generated.");
                return keys.getLong(1);
            }
        }
    }

    private long insertArtist(Connection conn, String sql, Artist artist, long accountId) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindArtistFields(stmt, artist, accountId);
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("No artist id generated.");
                return keys.getLong(1);
            }
        }
    }

    private void bindArtistFields(PreparedStatement stmt, Artist artist, long accountId) throws SQLException {
        stmt.setLong(1, accountId);
        stmt.setString(2, artist.getName());
        stmt.setString(3, artist.getBio());
        setNullableInteger(stmt, 4, artist.getBirthYear());
        stmt.setString(5, artist.getContactEmail());
        stmt.setString(6, artist.getPhone());
        stmt.setString(7, artist.getCity());
        stmt.setString(8, artist.getWebsite());
        stmt.setString(9, artist.getSocialMedia());
        stmt.setBoolean(10, artist.isActive());
    }

    private void syncDisciplines(Connection conn, long artistId, List<Discipline> disciplines) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
                "DELETE FROM artist_discipline WHERE artist_id = ?")) {
            stmt.setLong(1, artistId);
            stmt.executeUpdate();
        }
        if (disciplines == null) return;
        String insertLink = "INSERT INTO artist_discipline (artist_id, discipline_id) VALUES (?, ?)";
        for (Discipline d : disciplines) {
            if (d == null || d.getName() == null || d.getName().isBlank()) continue;
            long disciplineId = getOrCreateDisciplineId(conn, d.getName().trim());
            try (PreparedStatement stmt = conn.prepareStatement(insertLink)) {
                stmt.setLong(1, artistId);
                stmt.setLong(2, disciplineId);
                stmt.executeUpdate();
            }
        }
    }

    private long getOrCreateDisciplineId(Connection conn, String name) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT discipline_id FROM discipline WHERE name = ?")) {
            stmt.setString(1, name);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getLong("discipline_id");
            }
        }
        try (PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO discipline (name) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, name);
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
            }
        }
        throw new SQLException("Unable to create discipline: " + name);
    }

    private void setNullableInteger(PreparedStatement stmt, int index, Integer value) throws SQLException {
        if (value == null) stmt.setNull(index, java.sql.Types.INTEGER);
        else stmt.setInt(index, value);
    }

    private String resolveEmail(Artist artist) {
        if (artist.getContactEmail() != null && !artist.getContactEmail().isBlank())
            return artist.getContactEmail();
        String base = artist.getName() == null ? "artist"
                : artist.getName().trim().toLowerCase().replaceAll("[^a-z0-9]+", ".");
        base = base.replaceAll("^\\.+|\\.+$", "");
        if (base.isBlank()) base = "artist";
        return base + "@artconnect.local";
    }

    private IllegalStateException databaseError(String message, Exception cause) {
        return new IllegalStateException(message, cause);
    }
}