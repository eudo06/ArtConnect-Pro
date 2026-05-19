package com.project.artconnect.service.impl;

import com.project.artconnect.dao.ArtworkDao;
import com.project.artconnect.model.Artist;
import com.project.artconnect.model.Artwork;
import com.project.artconnect.persistence.JdbcArtworkDao;
import com.project.artconnect.service.ArtworkService;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class JdbcArtworkService implements ArtworkService {
    private final ArtworkDao artworkDao;

    public JdbcArtworkService() {
        this(new JdbcArtworkDao());
    }

    public JdbcArtworkService(ArtworkDao artworkDao) {
        this.artworkDao = artworkDao;
    }

    @Override
    public List<Artwork> getAllArtworks() {
        return artworkDao.findAll();
    }

    @Override
    public Optional<Artwork> getArtworkByTitle(String title) {
        if (title == null) {
            return Optional.empty();
        }

        return artworkDao.findAll().stream()
                .filter(artwork -> title.equalsIgnoreCase(artwork.getTitle()))
                .findFirst();
    }

    @Override
    public List<Artwork> getArtworksByArtist(Artist artist) {
        if (artist == null || artist.getName() == null || artist.getName().isBlank()) {
            return Collections.emptyList();
        }
        return artworkDao.findByArtistName(artist.getName());
    }

    @Override
    public void createArtwork(Artwork artwork) {
        artworkDao.save(artwork);
    }

    @Override
    public void updateArtwork(Artwork artwork) {
        artworkDao.update(artwork);
    }

    @Override
    public void deleteArtwork(String title) {
        if (title == null || title.isBlank()) {
            return;
        }

        if (artworkDao instanceof JdbcArtworkDao jdbcArtworkDao) {
            jdbcArtworkDao.deleteByTitle(title);
            return;
        }

        artworkDao.findAll().stream()
                .filter(artwork -> title.equalsIgnoreCase(artwork.getTitle()))
                .findFirst()
                .ifPresent(artwork -> {
                    throw new IllegalStateException("Artwork deletion requires a JDBC-backed DAO.");
                });
    }
}
