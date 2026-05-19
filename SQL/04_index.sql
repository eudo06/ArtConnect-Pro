USE artconnect;

-- =========================================================
-- INDEX ARTCONNECT
--
-- Rappel InnoDB : toute FOREIGN KEY crée automatiquement
-- un index sur la colonne référençante. Inutile de les
-- recréer manuellement (cela génère un doublon invisible
-- qui occupe de l'espace et ralentit les écritures).
--
-- Les index ci-dessous ciblent uniquement des colonnes
-- NON couvertes par une PK ou une FK existante.
-- =========================================================


-- ---------------------------------------------------------
-- INDEX 1 : Recherche d'artiste par nom de scène
-- Colonne : artist.stage_name (pas de FK, pas de UNIQUE)
-- Utilisation : JdbcArtistDao.update() / delete() font un
--   WHERE stage_name = ? ; l'UI filtre aussi par nom.
-- ---------------------------------------------------------
CREATE INDEX idx_artist_stage_name
ON artist(stage_name);


-- ---------------------------------------------------------
-- INDEX 2 : Tri / filtre des événements par date
-- Colonne : event.start_datetime (pas de FK)
-- Utilisation : JdbcWorkshopDao.findAll() trie par
--   start_datetime ; les vues et l'UI filtrent les
--   événements à venir.
-- ---------------------------------------------------------
CREATE INDEX idx_event_start_datetime
ON event(start_datetime);


-- ---------------------------------------------------------
-- INDEX 3 : Recherche d'une œuvre par titre
-- Colonne : artwork.title (pas de FK, pas de UNIQUE)
-- Utilisation : JdbcArtworkDao.update() / deleteByTitle()
--   et JdbcExhibitionDao.syncExhibitionArtworks() font un
--   WHERE title = ? sans index — scan complet sinon.
-- ---------------------------------------------------------
CREATE INDEX idx_artwork_title
ON artwork(title);


-- ---------------------------------------------------------
-- INDEX 4 : Recherche d'un membre par nom complet
-- Colonne : community_member.full_name (pas de FK)
-- Utilisation : JdbcCommunityMemberDao.findByName(),
--   findReviewsByMemberName() et findBookingsByMemberName()
--   font tous un WHERE full_name = ?.
-- ---------------------------------------------------------
CREATE INDEX idx_community_member_full_name
ON community_member(full_name);


-- ---------------------------------------------------------
-- INDEX 5 : Statut et date pour filtrer les réservations
-- Colonne : booking.payment_status (pas de FK)
-- Utilisation : requêtes de reporting sur les paiements
--   en attente (PENDING) ; utile pour les triggers et
--   procédures qui vérifient les inscriptions actives.
-- ---------------------------------------------------------
CREATE INDEX idx_booking_payment_status
ON booking(payment_status);


-- ---------------------------------------------------------
-- INDEX 6 : Statut des œuvres (filtrage catalogue)
-- Colonne : artwork.status (pas de FK)
-- Utilisation : la vue vw_artwork_catalog et l'UI filtrent
--   souvent par status = 'FOR_SALE' ou 'SOLD'.
-- ---------------------------------------------------------
CREATE INDEX idx_artwork_status
ON artwork(status);


-- =========================================================
-- INDEX NON CRÉÉS (redondants avec FK InnoDB)
-- =========================================================
-- Les colonnes suivantes sont déjà indexées automatiquement
-- par InnoDB via leur contrainte FOREIGN KEY :
--
--   artwork.artist_id          → fk_artwork_artist
--   workshop.instructor_artist_id → fk_workshop_artist
--   booking.member_id          → fk_booking_member
--   review.artwork_id          → fk_review_artwork
--   review.member_id           → fk_review_member
--   event.promoter_id          → fk_event_promoter
--   exhibition.gallery_id      → fk_exhibition_gallery
--
-- Créer ces index manuellement doublerait l'espace disque
-- et alourdirait chaque INSERT / UPDATE / DELETE.
-- =========================================================