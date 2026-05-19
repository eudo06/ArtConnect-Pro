USE artconnect;
 
-- =========================================================
-- TRIGGERS
-- =========================================================
 
DELIMITER $$
 
CREATE TRIGGER trg_event_check_dates_before_insert
BEFORE INSERT ON event
FOR EACH ROW
BEGIN
    IF NEW.end_datetime IS NOT NULL AND NEW.end_datetime < NEW.start_datetime THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Erreur : end_datetime doit être supérieur ou égal à start_datetime';
    END IF;
END$$
 
DELIMITER ;
 
 
DELIMITER $$
 
CREATE TRIGGER trg_event_check_dates_before_update
BEFORE UPDATE ON event
FOR EACH ROW
BEGIN
    IF NEW.end_datetime IS NOT NULL AND NEW.end_datetime < NEW.start_datetime THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Erreur : end_datetime doit être supérieur ou égal à start_datetime';
    END IF;
END$$
 
DELIMITER ;
 
 
DELIMITER $$
 
CREATE TRIGGER trg_booking_check_capacity_before_insert
BEFORE INSERT ON booking
FOR EACH ROW
BEGIN
    DECLARE v_capacity INT;
    DECLARE v_count INT;
 
    SELECT e.capacity
    INTO v_capacity
    FROM workshop w
    JOIN event e ON w.event_id = e.event_id
    WHERE w.event_id = NEW.workshop_event_id;
 
    SELECT COUNT(*)
    INTO v_count
    FROM booking b
    WHERE b.workshop_event_id = NEW.workshop_event_id;
 
    IF v_capacity IS NOT NULL AND v_count >= v_capacity THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Erreur : nombre maximal de participants atteint pour ce workshop';
    END IF;
END$$
 
DELIMITER ;
 
 
DELIMITER $$
 
CREATE TRIGGER trg_event_audit_after_update
AFTER UPDATE ON event
FOR EACH ROW
BEGIN
    INSERT INTO event_audit (
        event_id,
        action_type,
        old_title,
        new_title,
        old_start_datetime,
        new_start_datetime,
        old_end_datetime,
        new_end_datetime,
        changed_at
    )
    VALUES (
        OLD.event_id,
        'UPDATE',
        OLD.title,
        NEW.title,
        OLD.start_datetime,
        NEW.start_datetime,
        OLD.end_datetime,
        NEW.end_datetime,
        NOW()
    );
END$$
 
DELIMITER ;
 
 
-- =========================================================
-- PROCÉDURES STOCKÉES
-- =========================================================
 
DELIMITER $$
 
CREATE PROCEDURE sp_create_workshop (
    IN p_promoter_id INT,
    IN p_title VARCHAR(160),
    IN p_description TEXT,
    IN p_start_datetime DATETIME,
    IN p_end_datetime DATETIME,
    IN p_capacity INT,
    IN p_location VARCHAR(160),
    IN p_instructor_artist_id INT,
    IN p_duration_minutes INT,
    IN p_price DECIMAL(10,2),
    IN p_level VARCHAR(30)
)
BEGIN
    DECLARE v_event_id INT;
 
    INSERT INTO event (
        promoter_id, title, description,
        start_datetime, end_datetime,
        status, capacity, location
    )
    VALUES (
        p_promoter_id, p_title, p_description,
        p_start_datetime, p_end_datetime,
        'PLANNED', p_capacity, p_location
    );
 
    SET v_event_id = LAST_INSERT_ID();
 
    INSERT INTO workshop (
        event_id, instructor_artist_id,
        duration_minutes, price, level
    )
    VALUES (
        v_event_id, p_instructor_artist_id,
        p_duration_minutes, p_price, p_level
    );
END$$
 
DELIMITER ;
 
 
DELIMITER $$
 
CREATE PROCEDURE sp_create_exhibition (
    IN p_promoter_id INT,
    IN p_gallery_id INT,
    IN p_title VARCHAR(160),
    IN p_description TEXT,
    IN p_start_datetime DATETIME,
    IN p_end_datetime DATETIME,
    IN p_location VARCHAR(160),
    IN p_theme VARCHAR(120),
    IN p_curator_name VARCHAR(120)
)
BEGIN
    DECLARE v_event_id INT;
 
    INSERT INTO event (
        promoter_id, title, description,
        start_datetime, end_datetime,
        status, capacity, location
    )
    VALUES (
        p_promoter_id, p_title, p_description,
        p_start_datetime, p_end_datetime,
        'PLANNED', NULL, p_location
    );
 
    SET v_event_id = LAST_INSERT_ID();
 
    INSERT INTO exhibition (
        event_id, gallery_id, theme, curator_name
    )
    VALUES (
        v_event_id, p_gallery_id, p_theme, p_curator_name
    );
END$$
 
DELIMITER ;
 
 
DELIMITER $$
 
CREATE PROCEDURE sp_book_member_to_workshop (
    IN p_workshop_event_id INT,
    IN p_member_id INT,
    IN p_payment_status VARCHAR(20)
)
BEGIN
    INSERT INTO booking (
        workshop_event_id, member_id,
        booking_date, payment_status
    )
    VALUES (
        p_workshop_event_id, p_member_id,
        NOW(), p_payment_status
    );
END$$
 
DELIMITER ;
 
 
-- =========================================================
-- FONCTIONS STOCKÉES
-- =========================================================
 
-- ---------------------------------------------------------
-- fn_workshop_participant_count
--
-- Retourne le nombre de participants inscrits à un workshop.
--
-- NOT DETERMINISTIC : le résultat dépend de l'état courant
--   de la table booking, qui évolue à chaque inscription.
--   Marquer DETERMINISTIC serait incorrect et pousserait
--   MySQL à mettre en cache un résultat obsolète.
-- READS SQL DATA : la fonction lit des données en base
--   sans les modifier.
-- ---------------------------------------------------------
DELIMITER $$
 
CREATE FUNCTION fn_workshop_participant_count (
    p_workshop_event_id INT
)
RETURNS INT
NOT DETERMINISTIC
READS SQL DATA
BEGIN
    DECLARE v_count INT;
 
    SELECT COUNT(*)
    INTO v_count
    FROM booking
    WHERE workshop_event_id = p_workshop_event_id;
 
    RETURN v_count;
END$$
 
DELIMITER ;
 
 
-- ---------------------------------------------------------
-- fn_artwork_average_rating
--
-- Retourne la note moyenne d'une œuvre (0 si aucun avis).
--
-- NOT DETERMINISTIC : le résultat dépend des lignes
--   présentes dans review, qui changent à chaque critique
--   ajoutée ou supprimée.
-- READS SQL DATA : lecture seule, aucune écriture en base.
-- ---------------------------------------------------------
DELIMITER $$
 
CREATE FUNCTION fn_artwork_average_rating (
    p_artwork_id INT
)
RETURNS DECIMAL(4,2)
NOT DETERMINISTIC
READS SQL DATA
BEGIN
    DECLARE v_avg DECIMAL(4,2);
 
    SELECT AVG(rating)
    INTO v_avg
    FROM review
    WHERE artwork_id = p_artwork_id;
 
    RETURN IFNULL(v_avg, 0);
END$$
 
DELIMITER ;
 