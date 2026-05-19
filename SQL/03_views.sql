USE artconnect;
 
-- =========================================================
-- VUE 1 : Catalogue public des œuvres
-- Objectif : simplifier les requêtes de listing et masquer
--   les colonnes techniques (artwork_id interne, artist_id).
--   Exposée en lecture à art_user (voir 06_autorisations.sql).
-- =========================================================
CREATE OR REPLACE VIEW vw_artwork_catalog AS
SELECT
    aw.artwork_id,
    aw.title AS artwork_title,
    aw.type,
    aw.medium,
    aw.price,
    aw.status,
    a.artist_id,
    a.stage_name AS artist_name,
    a.city AS artist_city
FROM artwork aw
JOIN artist a ON aw.artist_id = a.artist_id;
 
 
-- =========================================================
-- VUE 2 : Détails des expositions
-- Objectif : centraliser la jointure event / exhibition /
--   gallery en une seule surface de requête. Évite de
--   dupliquer cette jointure complexe dans chaque couche.
--   Exposée en lecture à art_user.
-- =========================================================
CREATE OR REPLACE VIEW vw_exhibition_details AS
SELECT
    e.event_id,
    e.title,
    e.description,
    e.start_datetime,
    e.end_datetime,
    e.status,
    e.capacity,
    e.location,
    ex.theme,
    ex.curator_name,
    g.gallery_id,
    g.name    AS gallery_name,
    g.address AS gallery_address,
    g.rating  AS gallery_rating
FROM exhibition ex
JOIN event   e ON ex.event_id   = e.event_id
JOIN gallery g ON ex.gallery_id = g.gallery_id;
 
 
-- =========================================================
-- VUE 3 : Détails des ateliers
-- Objectif : centraliser la jointure event / workshop /
--   artist (instructeur) pour l'affichage de la liste des
--   ateliers. Exposée en lecture à art_user.
-- =========================================================
CREATE OR REPLACE VIEW vw_workshop_details AS
SELECT
    e.event_id,
    e.title,
    e.description,
    e.start_datetime,
    e.end_datetime,
    e.status,
    e.capacity,
    e.location,
    w.duration_minutes,
    w.price,
    w.level,
    a.artist_id   AS instructor_id,
    a.stage_name  AS instructor_name
FROM workshop w
JOIN event  e ON w.event_id             = e.event_id
JOIN artist a ON w.instructor_artist_id = a.artist_id;
 
 
-- =========================================================
-- VUE 4 : Comptes publics (version sécurisée)
-- Objectif : masquer password_hash et limiter les colonnes
--   visibles à ce qui est strictement nécessaire pour les
--   opérations d'administration légère.
--
-- Choix de sécurité :
--   - password_hash    → masqué (données sensibles)
--   - email            → masqué (données personnelles RGPD)
--   - account_id       → exposé (clé de référence)
--   - account_status   → exposé (utile pour l'admin)
--   - created_at       → exposé (utile pour l'audit)
--
-- Accès : réservé à art_admin uniquement.
--   art_user n'a pas accès aux données de compte,
--   même partielles (voir 06_autorisations.sql).
-- =========================================================
CREATE OR REPLACE VIEW vw_public_accounts AS
SELECT
    account_id,
    account_status,
    created_at
FROM account;
 