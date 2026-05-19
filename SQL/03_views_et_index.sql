use artconnect;



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
    g.name AS gallery_name,
    g.address AS gallery_address,
    g.rating AS gallery_rating
FROM exhibition ex
JOIN event e ON ex.event_id = e.event_id
JOIN gallery g ON ex.gallery_id = g.gallery_id;

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
    a.artist_id AS instructor_id,
    a.stage_name AS instructor_name
FROM workshop w
JOIN event e ON w.event_id = e.event_id
JOIN artist a ON w.instructor_artist_id = a.artist_id;

CREATE OR REPLACE VIEW vw_public_accounts AS
SELECT
    account_id,
    email,
    account_status,
    created_at
FROM account;



CREATE INDEX idx_artist_stage_name
ON artist(stage_name);

CREATE INDEX idx_artwork_artist_id
ON artwork(artist_id);

CREATE INDEX idx_event_start_datetime
ON event(start_datetime);

CREATE INDEX idx_workshop_instructor_artist_id
ON workshop(instructor_artist_id);

CREATE INDEX idx_booking_member_id
ON booking(member_id);

CREATE INDEX idx_review_artwork_id
ON review(artwork_id);