USE artconnect;
 
 
DROP USER IF EXISTS 'art_user'@'localhost';
DROP USER IF EXISTS 'art_admin'@'localhost';
FLUSH PRIVILEGES;
 
-- =========================================================
-- GESTION DES DROITS D'ACCÈS — ArtConnect
--
-- Ce script est à exécuter une seule fois après la création
-- des vues (03_views.sql).
-- Si les utilisateurs existent déjà, les supprimer d'abord :
--   DROP USER IF EXISTS 'art_user'@'localhost';
--   DROP USER IF EXISTS 'art_admin'@'localhost';
-- =========================================================
 
 
-- ---------------------------------------------------------
-- Utilisateur : art_user
-- Profil : visiteur / membre de la communauté
-- Droits : lecture seule sur les vues publiques uniquement.
--   - Pas d'accès aux tables directement (pas de SELECT
--     sur account, artist, artwork, etc.).
--   - Pas d'accès à vw_public_accounts : les données de
--     compte (même sans email ni mot de passe) ne concernent
--     pas un visiteur.
-- ---------------------------------------------------------
CREATE USER 'art_user'@'localhost' IDENTIFIED BY 'user123';
 
GRANT SELECT ON artconnect.vw_artwork_catalog    TO 'art_user'@'localhost';
GRANT SELECT ON artconnect.vw_exhibition_details TO 'art_user'@'localhost';
GRANT SELECT ON artconnect.vw_workshop_details   TO 'art_user'@'localhost';
 
FLUSH PRIVILEGES;
 
 
-- ---------------------------------------------------------
-- Utilisateur : art_admin
-- Profil : administrateur de la plateforme
-- Droits : tous les privilèges sur la base artconnect,
--   y compris l'accès à vw_public_accounts pour les
--   opérations de modération et d'audit des comptes.
--
-- Note : ALL PRIVILEGES inclut SELECT sur toutes les vues,
--   dont vw_public_accounts. Pas besoin de GRANT séparé.
-- ---------------------------------------------------------
CREATE USER 'art_admin'@'localhost' IDENTIFIED BY 'admin123';
 
GRANT ALL PRIVILEGES ON artconnect.* TO 'art_admin'@'localhost';
 
FLUSH PRIVILEGES;
 
 
-- =========================================================
-- RÉCAPITULATIF DES ACCÈS PAR VUE
-- =========================================================
--
-- Vue                    | art_user | art_admin
-- -----------------------|----------|----------
-- vw_artwork_catalog     |  SELECT  |  ALL
-- vw_exhibition_details  |  SELECT  |  ALL
-- vw_workshop_details    |  SELECT  |  ALL
-- vw_public_accounts     |  AUCUN   |  ALL
-- Tables directes        |  AUCUN   |  ALL
--
-- =========================================================
 