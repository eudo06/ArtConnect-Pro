package com.project.artconnect.dao;

import com.project.artconnect.model.Artwork;
import java.util.List;

public interface ArtworkDao {

    List<Artwork> findAll();

    Artwork findById(int id);

    void save(Artwork artwork);

    void update(Artwork artwork);

    void delete(int artworkId);

    List<Artwork> findByArtistName(String artistName);
}