package com.project.artconnect.service.impl;

import com.project.artconnect.model.Exhibition;
import com.project.artconnect.model.Gallery;
import com.project.artconnect.persistence.JdbcExhibitionDao;
import com.project.artconnect.persistence.JdbcGalleryDao;
import com.project.artconnect.service.GalleryService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcGalleryService implements GalleryService {
    private final JdbcGalleryDao galleryDao;
    private final JdbcExhibitionDao exhibitionDao;

    public JdbcGalleryService() {
        this(new JdbcGalleryDao(), new JdbcExhibitionDao());
    }

    public JdbcGalleryService(JdbcGalleryDao galleryDao, JdbcExhibitionDao exhibitionDao) {
        this.galleryDao = galleryDao;
        this.exhibitionDao = exhibitionDao;
    }

    @Override
    public List<Gallery> getAllGalleries() {
        List<Gallery> galleries = galleryDao.findAll();
        List<Exhibition> exhibitions = exhibitionDao.findAll();

        for (Gallery gallery : galleries) {
            List<Exhibition> attachedExhibitions = new ArrayList<>();
            for (Exhibition exhibition : exhibitions) {
                if (exhibition.getGallery() != null
                        && gallery.getName() != null
                        && gallery.getName().equalsIgnoreCase(exhibition.getGallery().getName())) {
                    exhibition.setGallery(gallery);
                    attachedExhibitions.add(exhibition);
                }
            }
            gallery.setExhibitions(attachedExhibitions);
        }

        return galleries;
    }

    @Override
    public Optional<Gallery> getGalleryByName(String name) {
        if (name == null) {
            return Optional.empty();
        }
        return getAllGalleries().stream()
                .filter(gallery -> name.equalsIgnoreCase(gallery.getName()))
                .findFirst();
    }

    @Override
    public List<Exhibition> getExhibitionsByGallery(Gallery gallery) {
        if (gallery == null || gallery.getName() == null || gallery.getName().isBlank()) {
            return List.of();
        }
        return getGalleryByName(gallery.getName())
                .map(Gallery::getExhibitions)
                .orElseGet(List::of);
    }
}
