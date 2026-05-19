USE artconnect;

DELETE FROM booking WHERE member_id = 9;
 
-- =========================================================
-- SCÉNARIOS TRANSACTIONNELS — ArtConnect
--
-- Principe : une transaction garantit l'atomicité.
-- Soit TOUTES les opérations réussissent (COMMIT),
-- soit AUCUNE n'est enregistrée (ROLLBACK).
-- =========================================================
 
 
-- =========================================================
-- SCÉNARIO 1 (SUCCÈS) : Inscription atomique d'un membre
--                        à plusieurs workshops
-- =========================================================
-- Objectif : inscrire le membre 9 à trois workshops
-- différents en une seule transaction. Si l'une échoue,
-- aucune inscription n'est enregistrée.
-- =========================================================
 
START TRANSACTION;
 
CALL sp_book_member_to_workshop(6,  9, 'PENDING');
CALL sp_book_member_to_workshop(8,  9, 'PENDING');
CALL sp_book_member_to_workshop(14, 9, 'PENDING');
 
COMMIT;
 
-- Vérification après COMMIT :
-- SELECT booking_id, workshop_event_id, member_id, payment_status
-- FROM booking
-- WHERE member_id = 9
-- ORDER BY booking_id DESC;
 
 
-- =========================================================
-- SCÉNARIO 2 (ÉCHEC — DOUBLON) : Démonstration du ROLLBACK
--                                  sur violation de contrainte
-- =========================================================
-- Objectif : tenter d'inscrire le membre 9 à un workshop
-- auquel il est déjà inscrit (scénario 1 doit être exécuté
-- avant). La contrainte UNIQUE uq_booking(workshop_event_id,
-- member_id) bloque la deuxième inscription au workshop 6.
-- Le ROLLBACK annule toutes les opérations de la transaction.
-- =========================================================
 
START TRANSACTION;
 
-- Première inscription : workshop 10 → devrait réussir
CALL sp_book_member_to_workshop(10, 9, 'PENDING');
 
-- Deuxième inscription : workshop 6 → ÉCHEC car membre 9
-- est déjà inscrit (contrainte uq_booking) ; la transaction
-- entière doit être annulée.
CALL sp_book_member_to_workshop(6, 9, 'PENDING');
 
-- Cette ligne ne doit pas être atteinte en cas d'erreur.
-- En production Java/JDBC, le ROLLBACK est déclenché dans
-- le bloc catch. En SQL pur, exécuter manuellement :
ROLLBACK;
 
-- Vérification : le membre 9 NE doit PAS avoir de nouvelle
-- inscription au workshop 10 (rollback effectif).
-- SELECT booking_id, workshop_event_id, member_id
-- FROM booking
-- WHERE member_id = 9
-- ORDER BY booking_id DESC;

 
 
-- =========================================================
-- SCÉNARIO 3 (ÉCHEC — CAPACITÉ ATTEINTE) : ROLLBACK
--             déclenché par le trigger trg_booking_check_capacity
-- =========================================================
-- Objectif : tenter d'inscrire un membre à un workshop
-- complet. Le trigger trg_booking_check_capacity_before_insert
-- lève une erreur SQLSTATE 45000 qui provoque le ROLLBACK.
--
-- Pré-requis : le workshop d'event_id 6 a une capacité
-- définie. Remplir d'abord toutes les places disponibles,
-- puis tenter une inscription supplémentaire.
-- =========================================================
 
START TRANSACTION;
 
-- Inscription normale au workshop 12 → réussit
CALL sp_book_member_to_workshop(12, 2, 'PENDING');
 
-- Inscription à un workshop dont la capacité est atteinte
-- → le trigger lève : "nombre maximal de participants
--   atteint pour ce workshop" → ROLLBACK nécessaire.
CALL sp_book_member_to_workshop(6, 10, 'PENDING');
 
ROLLBACK;
 
-- Vérification : aucune des deux inscriptions ci-dessus
-- ne doit apparaître en base.
-- SELECT booking_id, workshop_event_id, member_id
-- FROM booking
-- WHERE member_id IN (2, 10)
-- ORDER BY booking_id DESC;
 
 
-- =========================================================
-- NOTE SUR L'UTILISATION EN JAVA (JDBC)
-- =========================================================
-- Dans le code Java (JdbcCommunityMemberDao.createBooking),
-- la gestion transactionnelle est :
--
--   conn.setAutoCommit(false);
--   try {
--       // opérations...
--       conn.commit();
--   } catch (SQLException e) {
--       conn.rollback();   // ← déclenché automatiquement
--       throw e;
--   } finally {
--       conn.setAutoCommit(true);
--   }
--
-- Le ROLLBACK est donc automatique en cas d'erreur,
-- qu'elle vienne d'un trigger, d'une contrainte UNIQUE,
-- ou de toute autre violation d'intégrité.
-- =========================================================
 