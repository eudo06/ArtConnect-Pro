package com.project.artconnect.util;

import com.project.artconnect.service.*;
import com.project.artconnect.service.impl.*;

public class ServiceProvider {
    private static final boolean inMemoryMode;
    private static final ArtistService artistService;
    private static final ArtworkService artworkService;
    private static final GalleryService galleryService;
    private static final WorkshopService workshopService;
    private static final CommunityService communityService;
    private static final ExhibitionService exhibitionService;

    static {
        inMemoryMode = useInMemoryServices();
        if (inMemoryMode) {
            artistService    = buildInMemoryArtistService();
            artworkService   = buildInMemoryArtworkService(artistService);
            galleryService   = buildInMemoryGalleryService(artworkService);
            workshopService  = buildInMemoryWorkshopService(artistService);
            communityService = buildInMemoryCommunityService(artworkService);
            exhibitionService = new JdbcExhibitionService(); // pas de version InMemory — utilise JDBC directement
            System.out.println("ArtConnect is using in-memory services (test mode).");
        } else {
            artistService    = new JdbcArtistService();
            artworkService   = new JdbcArtworkService();
            galleryService   = new JdbcGalleryService();
            workshopService  = new JdbcWorkshopService();
            communityService = new JdbcCommunityService();
            exhibitionService = new JdbcExhibitionService();
            System.out.println("ArtConnect is using MySQL-backed services.");
        }
    }

    public static ArtistService    getArtistService()    { return artistService; }
    public static ArtworkService   getArtworkService()   { return artworkService; }
    public static GalleryService   getGalleryService()   { return galleryService; }
    public static WorkshopService  getWorkshopService()  { return workshopService; }
    public static CommunityService getCommunityService() { return communityService; }
    public static ExhibitionService getExhibitionService() { return exhibitionService; }

    public static String getPersistenceModeLabel() {
        return inMemoryMode ? "In-Memory" : "MySQL";
    }

    private static boolean useInMemoryServices() {
        return Boolean.parseBoolean(System.getProperty("artconnect.useInMemory", "false"));
    }

    private static ArtistService buildInMemoryArtistService() {
        return new InMemoryArtistService();
    }

    private static ArtworkService buildInMemoryArtworkService(ArtistService artistService) {
        InMemoryArtworkService artworkService = new InMemoryArtworkService();
        artworkService.initData(artistService);
        return artworkService;
    }

    private static GalleryService buildInMemoryGalleryService(ArtworkService artworkService) {
        InMemoryGalleryService galleryService = new InMemoryGalleryService();
        galleryService.initData(artworkService);
        return galleryService;
    }

    private static WorkshopService buildInMemoryWorkshopService(ArtistService artistService) {
        InMemoryWorkshopService workshopService = new InMemoryWorkshopService();
        workshopService.initData(artistService);
        return workshopService;
    }

    private static CommunityService buildInMemoryCommunityService(ArtworkService artworkService) {
        InMemoryCommunityService communityService = new InMemoryCommunityService();
        communityService.initData(artworkService);
        return communityService;
    }
}