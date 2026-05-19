package com.project.artconnect.persistence;

import com.project.artconnect.dao.CommunityMemberDao;
import com.project.artconnect.model.Artist;
import com.project.artconnect.model.Artwork;
import com.project.artconnect.model.Booking;
import com.project.artconnect.model.CommunityMember;
import com.project.artconnect.model.Discipline;
import com.project.artconnect.model.Review;
import com.project.artconnect.model.Workshop;
import com.project.artconnect.util.ConnectionManager;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class JdbcCommunityMemberDao implements CommunityMemberDao {

    private static final String SELECT_MEMBERS = """
        SELECT cm.member_id,
               cm.full_name,
               acc.email AS email,
               cm.birth_year,
               cm.phone,
               cm.city,
               cm.membership_type,
               d.name AS discipline_name
        FROM community_member cm
        JOIN account acc ON cm.account_id = acc.account_id
        LEFT JOIN member_discipline md ON cm.member_id = md.member_id
        LEFT JOIN discipline d ON md.discipline_id = d.discipline_id
        """;


    @Override
    public Optional<CommunityMember> findById(Long id) {
        String sql = SELECT_MEMBERS + " WHERE cm.member_id = ?";

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                List<CommunityMember> members = extractMembers(rs);
                return members.stream().findFirst();
            }
        } catch (SQLException e) {
            throw databaseError("Unable to load community member by id.", e);
        }
    }

    @Override
    public List<CommunityMember> findAll() {
        String sql = SELECT_MEMBERS + " ORDER BY cm.member_id";

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            return extractMembers(rs);
        } catch (SQLException e) {
            throw databaseError("Unable to load community members from MySQL.", e);
        }
    }

    public Optional<CommunityMember> findByName(String name) {
        String sql = SELECT_MEMBERS + " WHERE cm.full_name = ?";

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            try (ResultSet rs = stmt.executeQuery()) {
                List<CommunityMember> members = extractMembers(rs);
                return members.stream().findFirst();
            }
        } catch (SQLException e) {
            throw databaseError("Unable to load community member by name.", e);
        }
    }

    public List<Review> findReviewsByMemberName(String memberName) {
        String sql = """
                SELECT r.rating,
                       r.comment,
                       r.review_date,
                       aw.title,
                       aw.creation_year,
                       aw.type,
                       aw.medium,
                       aw.dimensions,
                       aw.description,
                       aw.price,
                       aw.status,
                       a.stage_name
                FROM review r
                JOIN community_member cm ON r.member_id = cm.member_id
                JOIN artwork aw ON r.artwork_id = aw.artwork_id
                JOIN artist a ON aw.artist_id = a.artist_id
                WHERE cm.full_name = ?
                ORDER BY r.review_date DESC
                """;

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, memberName);
            try (ResultSet rs = stmt.executeQuery()) {
                List<Review> reviews = new ArrayList<>();
                while (rs.next()) {
                    Review review = new Review();
                    review.setRating(rs.getInt("rating"));
                    review.setComment(rs.getString("comment"));
                    Date reviewDate = rs.getDate("review_date");
                    review.setReviewDate(reviewDate == null ? null : reviewDate.toLocalDate());
                    review.setArtwork(mapArtwork(rs));
                    reviews.add(review);
                }
                return reviews;
            }
        } catch (SQLException e) {
            throw databaseError("Unable to load reviews for member.", e);
        }
    }

    public List<Booking> findBookingsByMemberName(String memberName) {
        String sql = """
                SELECT b.booking_date,
                       b.payment_status,
                       e.title,
                       e.start_datetime,
                       e.location,
                       e.description,
                       e.capacity,
                       w.duration_minutes,
                       w.price,
                       w.level,
                       a.stage_name
                FROM booking b
                JOIN community_member cm ON b.member_id = cm.member_id
                JOIN workshop w ON b.workshop_event_id = w.event_id
                JOIN event e ON w.event_id = e.event_id
                JOIN artist a ON w.instructor_artist_id = a.artist_id
                WHERE cm.full_name = ?
                ORDER BY b.booking_date DESC
                """;

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, memberName);
            try (ResultSet rs = stmt.executeQuery()) {
                List<Booking> bookings = new ArrayList<>();
                while (rs.next()) {
                    Booking booking = new Booking();
                    booking.setBookingDate(toLocalDateTime(rs.getTimestamp("booking_date")));
                    booking.setPaymentStatus(rs.getString("payment_status"));
                    booking.setWorkshop(mapWorkshop(rs));
                    bookings.add(booking);
                }
                return bookings;
            }
        } catch (SQLException e) {
            throw databaseError("Unable to load bookings for member.", e);
        }
    }

    public void createBooking(String workshopTitle, String memberName) {
        String lookup = """
                SELECT w.event_id AS workshop_event_id, cm.member_id
                FROM workshop w
                JOIN event e ON w.event_id = e.event_id
                CROSS JOIN community_member cm
                WHERE e.title = ? AND cm.full_name = ?
                LIMIT 1
                """;
        String insert = """
                INSERT INTO booking (workshop_event_id, member_id, booking_date, payment_status)
                VALUES (?, ?, ?, ?)
                """;

        try (Connection conn = ConnectionManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Long workshopEventId = null;
                Long memberId = null;
                try (PreparedStatement lookupStmt = conn.prepareStatement(lookup)) {
                    lookupStmt.setString(1, workshopTitle);
                    lookupStmt.setString(2, memberName);
                    try (ResultSet rs = lookupStmt.executeQuery()) {
                        if (rs.next()) {
                            workshopEventId = rs.getLong("workshop_event_id");
                            memberId = rs.getLong("member_id");
                        }
                    }
                }

                if (workshopEventId == null || memberId == null) {
                    throw new SQLException("Unable to resolve workshop or member for booking.");
                }

                try (PreparedStatement insertStmt = conn.prepareStatement(insert)) {
                    insertStmt.setLong(1, workshopEventId);
                    insertStmt.setLong(2, memberId);
                    insertStmt.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
                    insertStmt.setString(4, "PENDING");
                    insertStmt.executeUpdate();
                }

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw databaseError("Unable to create booking in MySQL.", e);
        }
    }

    private List<CommunityMember> extractMembers(ResultSet rs) throws SQLException {
        Map<Long, CommunityMember> membersById = new LinkedHashMap<>();

        while (rs.next()) {
            long memberId = rs.getLong("member_id");
            CommunityMember member = membersById.computeIfAbsent(memberId, ignored -> mapMember(rs));

            String disciplineName = rs.getString("discipline_name");
            if (disciplineName != null
                    && member.getFavoriteDisciplines().stream().noneMatch(d -> disciplineName.equalsIgnoreCase(d.getName()))) {
                member.getFavoriteDisciplines().add(new Discipline(disciplineName));
            }
        }

        return new ArrayList<>(membersById.values());
    }

    private CommunityMember mapMember(ResultSet rs) {
        try {
            CommunityMember member = new CommunityMember();
            member.setName(rs.getString("full_name"));
            member.setEmail(rs.getString("email"));
            int birthYear = rs.getInt("birth_year");
            member.setBirthYear(rs.wasNull() ? null : birthYear);
            member.setPhone(rs.getString("phone"));
            member.setCity(rs.getString("city"));
            member.setMembershipType(rs.getString("membership_type"));
            return member;
        } catch (SQLException e) {
            throw databaseError("Unable to map community member row.", e);
        }
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

        Artist artist = new Artist();
        artist.setName(rs.getString("stage_name"));
        artwork.setArtist(artist);
        return artwork;
    }

    private Workshop mapWorkshop(ResultSet rs) throws SQLException {
        Workshop workshop = new Workshop();
        workshop.setTitle(rs.getString("title"));
        workshop.setDate(toLocalDateTime(rs.getTimestamp("start_datetime")));
        int capacity = rs.getInt("capacity");
        workshop.setMaxParticipants(rs.wasNull() ? 0 : capacity);
        workshop.setDurationMinutes(rs.getInt("duration_minutes"));
        workshop.setPrice(rs.getDouble("price"));
        workshop.setLevel(rs.getString("level"));
        workshop.setLocation(rs.getString("location"));
        workshop.setDescription(rs.getString("description"));

        Artist instructor = new Artist();
        instructor.setName(rs.getString("stage_name"));
        workshop.setInstructor(instructor);
        return workshop;
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

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    // ------------------------------------------------------------------
    // Reviews
    // ------------------------------------------------------------------

    /**
     * Retourne tous les avis sur une oeuvre identifiée par son titre.
     */
    public List<Review> findReviewsByArtworkTitle(String artworkTitle) {
        String sql = """
                SELECT r.rating, r.comment, r.review_date,
                       cm.full_name AS reviewer_name,
                       aw.title AS artwork_title
                FROM review r
                JOIN community_member cm ON r.member_id = cm.member_id
                JOIN artwork aw ON r.artwork_id = aw.artwork_id
                WHERE aw.title = ?
                ORDER BY r.review_date DESC
                """;

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, artworkTitle);
            try (ResultSet rs = stmt.executeQuery()) {
                List<Review> reviews = new ArrayList<>();
                while (rs.next()) {
                    Review review = new Review();
                    review.setRating(rs.getInt("rating"));
                    review.setComment(rs.getString("comment"));
                    java.sql.Date d = rs.getDate("review_date");
                    review.setReviewDate(d == null ? null : d.toLocalDate());
                    CommunityMember reviewer = new CommunityMember();
                    reviewer.setName(rs.getString("reviewer_name"));
                    review.setReviewer(reviewer);
                    Artwork artwork = new Artwork();
                    artwork.setTitle(rs.getString("artwork_title"));
                    review.setArtwork(artwork);
                    reviews.add(review);
                }
                return reviews;
            }
        } catch (SQLException e) {
            throw databaseError("Unable to load reviews for artwork.", e);
        }
    }

    /**
     * Crée un avis sur une oeuvre par un membre identifié par son nom.
     * Vérifie que le membre n'a pas déjà laissé un avis sur cette oeuvre.
     */
    public void createReview(String artworkTitle, String memberName, int rating, String comment) {
        String sqlGetIds = """
                SELECT aw.artwork_id, cm.member_id
                FROM artwork aw, community_member cm
                WHERE aw.title = ? AND cm.full_name = ?
                """;
        String sqlInsert = """
                INSERT INTO review (artwork_id, member_id, rating, comment, review_date)
                VALUES (?, ?, ?, ?, CURDATE())
                """;

        try (Connection conn = ConnectionManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                long artworkId, memberId;

                try (PreparedStatement stmt = conn.prepareStatement(sqlGetIds)) {
                    stmt.setString(1, artworkTitle);
                    stmt.setString(2, memberName);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (!rs.next()) throw new IllegalArgumentException(
                                "Oeuvre ou membre introuvable : " + artworkTitle + " / " + memberName);
                        artworkId = rs.getLong("artwork_id");
                        memberId  = rs.getLong("member_id");
                    }
                }

                try (PreparedStatement stmt = conn.prepareStatement(sqlInsert)) {
                    stmt.setLong(1, artworkId);
                    stmt.setLong(2, memberId);
                    stmt.setInt(3, Math.max(1, Math.min(5, rating)));
                    stmt.setString(4, comment);
                    stmt.executeUpdate();
                }

                conn.commit();
            } catch (SQLException | IllegalArgumentException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw databaseError("Unable to create review.", e);
        }
    }

    private IllegalStateException databaseError(String message, Exception cause) {
        return new IllegalStateException(message, cause);
    }
}