package com.project.artconnect.persistence;

import com.project.artconnect.dao.GalleryDao;
import com.project.artconnect.model.Gallery;
import com.project.artconnect.util.ConnectionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcGalleryDao implements GalleryDao {

    @Override
    public Optional<Gallery> findById(Long id) {
        String sql = """
                SELECT gallery_id, name, address, owner_name, opening_hours, contact_phone, rating, website
                FROM gallery
                WHERE gallery_id = ?
                """;

        try (Connection conn = ConnectionManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapGallery(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw databaseError("Unable to load gallery by id.", e);
        }
    }

    @Override
    public List<Gallery> findAll() {
        String sql = """
                SELECT gallery_id, name, address, owner_name, opening_hours, contact_phone, rating, website
                FROM gallery
                ORDER BY gallery_id
                """;

        try (Connection conn = ConnectionManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {
            List<Gallery> galleries = new ArrayList<>();
            while (rs.next()) {
                galleries.add(mapGallery(rs));
            }
            return galleries;
        } catch (SQLException e) {
            throw databaseError("Unable to load galleries from MySQL.", e);
        }
    }

    public Optional<Gallery> findByName(String name) {
        String sql = """
                SELECT gallery_id, name, address, owner_name, opening_hours, contact_phone, rating, website
                FROM gallery
                WHERE name = ?
                """;

        try (Connection conn = ConnectionManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapGallery(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw databaseError("Unable to load gallery by name.", e);
        }
    }

    private Gallery mapGallery(ResultSet rs) throws SQLException {
        Gallery gallery = new Gallery();
        gallery.setName(rs.getString("name"));
        gallery.setAddress(rs.getString("address"));
        gallery.setOwnerName(rs.getString("owner_name"));
        gallery.setOpeningHours(rs.getString("opening_hours"));
        gallery.setContactPhone(rs.getString("contact_phone"));
        gallery.setRating(rs.getDouble("rating"));
        gallery.setWebsite(rs.getString("website"));
        return gallery;
    }

    private IllegalStateException databaseError(String message, Exception cause) {
        return new IllegalStateException(message, cause);
    }
}
