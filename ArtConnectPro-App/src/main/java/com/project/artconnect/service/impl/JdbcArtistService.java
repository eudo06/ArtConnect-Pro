package com.project.artconnect.service.impl;

import com.project.artconnect.dao.ArtistDao;
import com.project.artconnect.model.Artist;
import com.project.artconnect.model.Discipline;
import com.project.artconnect.persistence.JdbcArtistDao;
import com.project.artconnect.service.ArtistService;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class JdbcArtistService implements ArtistService {
    private final ArtistDao artistDao;

    public JdbcArtistService() {
        this(new JdbcArtistDao());
    }

    public JdbcArtistService(ArtistDao artistDao) {
        this.artistDao = artistDao;
    }

    @Override
    public List<Artist> getAllArtists() {
        return artistDao.findAll();
    }

    /**
     * Recherche un artiste par nom via une requête ciblée (findByName),
     * au lieu de charger tous les artistes avec findAll() puis de filtrer
     * en mémoire. Cela élimine le délai observé après un save() qui
     * appelait trois fois findAll() en séquence.
     */
    @Override
    public Optional<Artist> getArtistByName(String name) {
        if (name == null) return Optional.empty();
        return artistDao.findByName(name);
    }

    @Override
    public void createArtist(Artist artist) {
        artistDao.save(artist);
    }

    @Override
    public void updateArtist(Artist artist) {
        artistDao.update(artist);
    }

    @Override
    public void deleteArtist(String name) {
        artistDao.delete(name);
    }

    @Override
    public List<Discipline> getAllDisciplines() {
        Map<String, Discipline> disciplinesByName = new LinkedHashMap<>();
        for (Artist artist : artistDao.findAll()) {
            for (Discipline discipline : artist.getDisciplines()) {
                if (discipline == null || discipline.getName() == null || discipline.getName().isBlank()) continue;
                disciplinesByName.putIfAbsent(discipline.getName().toLowerCase(),
                        new Discipline(discipline.getName()));
            }
        }
        return disciplinesByName.values().stream()
                .sorted(Comparator.comparing(Discipline::getName, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    public List<Artist> searchArtists(String query, String disciplineName, String city) {
        String normalizedQuery      = query == null ? null : query.trim().toLowerCase();
        String normalizedDiscipline = disciplineName == null ? null : disciplineName.trim();
        String normalizedCity       = city == null ? null : city.trim();

        return artistDao.findAll().stream()
                .filter(a -> normalizedQuery == null || normalizedQuery.isBlank()
                        || (a.getName() != null && a.getName().toLowerCase().contains(normalizedQuery)))
                .filter(a -> normalizedCity == null || normalizedCity.isBlank()
                        || (a.getCity() != null && a.getCity().equalsIgnoreCase(normalizedCity)))
                .filter(a -> normalizedDiscipline == null || normalizedDiscipline.isBlank()
                        || a.getDisciplines().stream()
                        .anyMatch(d -> normalizedDiscipline.equalsIgnoreCase(d.getName())))
                .collect(Collectors.toList());
    }
}