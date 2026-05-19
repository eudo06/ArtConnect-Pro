CREATE DATABASE IF NOT EXISTS artconnect;
USE artconnect;


CREATE TABLE account (
    account_id INT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(120) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    account_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);


CREATE TABLE admin (
    admin_id INT AUTO_INCREMENT PRIMARY KEY,
    account_id INT NOT NULL UNIQUE,
    full_name VARCHAR(120) NOT NULL,
    phone VARCHAR(30),
    CONSTRAINT fk_admin_account
        FOREIGN KEY (account_id) REFERENCES account(account_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
);


CREATE TABLE community (
    community_id INT AUTO_INCREMENT PRIMARY KEY,
    parent_community_id INT NULL,
    name VARCHAR(120) NOT NULL UNIQUE,
    description TEXT,
    community_type VARCHAR(30) NOT NULL,
    CONSTRAINT fk_community_parent
        FOREIGN KEY (parent_community_id) REFERENCES community(community_id)
        ON DELETE SET NULL
        ON UPDATE CASCADE
);


CREATE TABLE community_member (
    member_id INT AUTO_INCREMENT PRIMARY KEY,
    account_id INT NOT NULL UNIQUE,
    full_name VARCHAR(120) NOT NULL,
    birth_year INT,
    phone VARCHAR(30),
    city VARCHAR(80),
    membership_type VARCHAR(20) NOT NULL DEFAULT 'FREE',
    CONSTRAINT chk_member_birth_year
        CHECK (birth_year IS NULL OR birth_year BETWEEN 1000 AND 2100),
    CONSTRAINT fk_member_account
        FOREIGN KEY (account_id) REFERENCES account(account_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
);

-- =========================================================
-- 5. PROMOTER
-- =========================================================
CREATE TABLE promoter (
    promoter_id INT AUTO_INCREMENT PRIMARY KEY,
    account_id INT NOT NULL UNIQUE,
    full_name VARCHAR(120) NOT NULL,
    organization_name VARCHAR(120),
    phone VARCHAR(30),
    city VARCHAR(80),
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    validated_by_admin_id INT NULL,
    validation_date DATETIME NULL,
    CONSTRAINT fk_promoter_account
        FOREIGN KEY (account_id) REFERENCES account(account_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_promoter_admin
        FOREIGN KEY (validated_by_admin_id) REFERENCES admin(admin_id)
        ON DELETE SET NULL
        ON UPDATE CASCADE
);

-- =========================================================
-- 6. ARTIST
-- =========================================================
CREATE TABLE artist (
    artist_id INT AUTO_INCREMENT PRIMARY KEY,
    account_id INT NOT NULL UNIQUE,
    stage_name VARCHAR(120) NOT NULL,
    bio TEXT,
    birth_year INT,
    contact_email VARCHAR(120), -- email public optionnel
    phone VARCHAR(30),
    city VARCHAR(80),
    website VARCHAR(160),
    social_media VARCHAR(160),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    validated_by_admin_id INT NULL,
    validation_date DATETIME NULL,
    CONSTRAINT chk_artist_birth_year
        CHECK (birth_year IS NULL OR birth_year BETWEEN 1000 AND 2100),
    CONSTRAINT fk_artist_account
        FOREIGN KEY (account_id) REFERENCES account(account_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_artist_admin
        FOREIGN KEY (validated_by_admin_id) REFERENCES admin(admin_id)
        ON DELETE SET NULL
        ON UPDATE CASCADE
);

-- =========================================================
-- 7. DISCIPLINE
-- =========================================================
CREATE TABLE discipline (
    discipline_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(80) NOT NULL UNIQUE
);

-- =========================================================
-- 8. ARTIST_DISCIPLINE
-- =========================================================
CREATE TABLE artist_discipline (
    artist_id INT NOT NULL,
    discipline_id INT NOT NULL,
    PRIMARY KEY (artist_id, discipline_id),
    CONSTRAINT fk_ad_artist
        FOREIGN KEY (artist_id) REFERENCES artist(artist_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_ad_discipline
        FOREIGN KEY (discipline_id) REFERENCES discipline(discipline_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
);

-- =========================================================
-- 9. MEMBER_DISCIPLINE
-- =========================================================
CREATE TABLE member_discipline (
    member_id INT NOT NULL,
    discipline_id INT NOT NULL,
    PRIMARY KEY (member_id, discipline_id),
    CONSTRAINT fk_md_member
        FOREIGN KEY (member_id) REFERENCES community_member(member_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_md_discipline
        FOREIGN KEY (discipline_id) REFERENCES discipline(discipline_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
);

-- =========================================================
-- 10. GALLERY
-- =========================================================
CREATE TABLE gallery (
    gallery_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    address VARCHAR(180) NOT NULL,
    owner_name VARCHAR(120),
    opening_hours VARCHAR(120),
    contact_phone VARCHAR(30),
    rating DECIMAL(3,2),
    website VARCHAR(160),
    CONSTRAINT chk_gallery_rating
        CHECK (rating IS NULL OR rating BETWEEN 0 AND 5)
);

-- =========================================================
-- 11. EVENT
-- =========================================================
CREATE TABLE event (
    event_id INT AUTO_INCREMENT PRIMARY KEY,
    promoter_id INT NOT NULL,
    title VARCHAR(160) NOT NULL,
    description TEXT,
    start_datetime DATETIME NOT NULL,
    end_datetime DATETIME NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PLANNED',
    capacity INT,
    location VARCHAR(160),
    CONSTRAINT chk_event_capacity
        CHECK (capacity IS NULL OR capacity > 0),
    CONSTRAINT fk_event_promoter
        FOREIGN KEY (promoter_id) REFERENCES promoter(promoter_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
);

-- =========================================================
-- 12. EXHIBITION
-- =========================================================
CREATE TABLE exhibition (
    event_id INT PRIMARY KEY,
    gallery_id INT NOT NULL,
    theme VARCHAR(120),
    curator_name VARCHAR(120),
    CONSTRAINT fk_exhibition_event
        FOREIGN KEY (event_id) REFERENCES event(event_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_exhibition_gallery
        FOREIGN KEY (gallery_id) REFERENCES gallery(gallery_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
);

-- =========================================================
-- 13. WORKSHOP
-- =========================================================
CREATE TABLE workshop (
    event_id INT PRIMARY KEY,
    instructor_artist_id INT NOT NULL,
    duration_minutes INT NOT NULL,
    price DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    level VARCHAR(30),
    CONSTRAINT chk_workshop_duration
        CHECK (duration_minutes > 0),
    CONSTRAINT chk_workshop_price
        CHECK (price >= 0),
    CONSTRAINT fk_workshop_event
        FOREIGN KEY (event_id) REFERENCES event(event_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_workshop_artist
        FOREIGN KEY (instructor_artist_id) REFERENCES artist(artist_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
);

-- =========================================================
-- 14. ARTIST_EVENT
-- =========================================================
CREATE TABLE artist_event (
    artist_id INT NOT NULL,
    event_id INT NOT NULL,
    role_in_event VARCHAR(60),
    PRIMARY KEY (artist_id, event_id),
    CONSTRAINT fk_ae_artist
        FOREIGN KEY (artist_id) REFERENCES artist(artist_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_ae_event
        FOREIGN KEY (event_id) REFERENCES event(event_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
);

-- =========================================================
-- 15. ARTWORK
-- =========================================================
CREATE TABLE artwork (
    artwork_id INT AUTO_INCREMENT PRIMARY KEY,
    artist_id INT NOT NULL,
    title VARCHAR(160) NOT NULL,
    creation_year INT,
    type VARCHAR(80),
    medium VARCHAR(80),
    dimensions VARCHAR(80),
    description TEXT,
    price DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    status VARCHAR(20) NOT NULL DEFAULT 'FOR_SALE',
    CONSTRAINT chk_artwork_year
        CHECK (creation_year IS NULL OR creation_year BETWEEN 1000 AND 2100),
    CONSTRAINT chk_artwork_price
        CHECK (price >= 0),
    CONSTRAINT fk_artwork_artist
        FOREIGN KEY (artist_id) REFERENCES artist(artist_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
);

-- =========================================================
-- 16. ARTWORK_TAG
-- =========================================================
CREATE TABLE artwork_tag (
    tag_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(80) NOT NULL UNIQUE
);

-- =========================================================
-- 17. ARTWORK_TAG_MAP
-- =========================================================
CREATE TABLE artwork_tag_map (
    artwork_id INT NOT NULL,
    tag_id INT NOT NULL,
    PRIMARY KEY (artwork_id, tag_id),
    CONSTRAINT fk_atm_artwork
        FOREIGN KEY (artwork_id) REFERENCES artwork(artwork_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_atm_tag
        FOREIGN KEY (tag_id) REFERENCES artwork_tag(tag_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
);

-- =========================================================
-- 18. EXHIBITION_ARTWORK
-- =========================================================
CREATE TABLE exhibition_artwork (
    exhibition_event_id INT NOT NULL,
    artwork_id INT NOT NULL,
    PRIMARY KEY (exhibition_event_id, artwork_id),
    CONSTRAINT fk_ea_exhibition
        FOREIGN KEY (exhibition_event_id) REFERENCES exhibition(event_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_ea_artwork
        FOREIGN KEY (artwork_id) REFERENCES artwork(artwork_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
);

-- =========================================================
-- 19. BOOKING
-- =========================================================
CREATE TABLE booking (
    booking_id INT AUTO_INCREMENT PRIMARY KEY,
    workshop_event_id INT NOT NULL,
    member_id INT NOT NULL,
    booking_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    payment_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    CONSTRAINT uq_booking UNIQUE (workshop_event_id, member_id),
    CONSTRAINT fk_booking_workshop
        FOREIGN KEY (workshop_event_id) REFERENCES workshop(event_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_booking_member
        FOREIGN KEY (member_id) REFERENCES community_member(member_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
);

-- =========================================================
-- 20. REVIEW
-- =========================================================
CREATE TABLE review (
    review_id INT AUTO_INCREMENT PRIMARY KEY,
    member_id INT NOT NULL,
    artwork_id INT NOT NULL,
    rating INT NOT NULL,
    comment TEXT,
    review_date DATE NOT NULL,
    CONSTRAINT chk_review_rating
        CHECK (rating BETWEEN 1 AND 5),
    CONSTRAINT fk_review_member
        FOREIGN KEY (member_id) REFERENCES community_member(member_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_review_artwork
        FOREIGN KEY (artwork_id) REFERENCES artwork(artwork_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
);

-- =========================================================
-- 21. MEMBER_COMMUNITY
-- =========================================================
CREATE TABLE member_community (
    member_id INT NOT NULL,
    community_id INT NOT NULL,
    joined_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    membership_role VARCHAR(40),
    PRIMARY KEY (member_id, community_id),
    CONSTRAINT fk_mc_member
        FOREIGN KEY (member_id) REFERENCES community_member(member_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_mc_community
        FOREIGN KEY (community_id) REFERENCES community(community_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
);

-- =========================================================
-- 22. ARTIST_COMMUNITY
-- =========================================================
CREATE TABLE artist_community (
    artist_id INT NOT NULL,
    community_id INT NOT NULL,
    assigned_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    assigned_by_admin_id INT NULL,
    PRIMARY KEY (artist_id, community_id),
    CONSTRAINT fk_ac_artist
        FOREIGN KEY (artist_id) REFERENCES artist(artist_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_ac_community
        FOREIGN KEY (community_id) REFERENCES community(community_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_ac_admin
        FOREIGN KEY (assigned_by_admin_id) REFERENCES admin(admin_id)
        ON DELETE SET NULL
        ON UPDATE CASCADE
);

-- =========================================================
-- 23. ARTIST_APPLICATION
-- =========================================================
CREATE TABLE artist_application (
    artist_application_id INT AUTO_INCREMENT PRIMARY KEY,
    account_id INT NOT NULL,
    discipline_requested VARCHAR(80),
    submission_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(30) NOT NULL DEFAULT 'SUBMITTED',
    reviewed_by_admin_id INT NULL,
    CONSTRAINT fk_artist_app_account
        FOREIGN KEY (account_id) REFERENCES account(account_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_artist_app_admin
        FOREIGN KEY (reviewed_by_admin_id) REFERENCES admin(admin_id)
        ON DELETE SET NULL
        ON UPDATE CASCADE
);

-- =========================================================
-- 24. PROMOTER_APPLICATION
-- =========================================================
CREATE TABLE promoter_application (
    promoter_application_id INT AUTO_INCREMENT PRIMARY KEY,
    account_id INT NOT NULL,
    organization_name VARCHAR(120),
    submission_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(30) NOT NULL DEFAULT 'SUBMITTED',
    reviewed_by_admin_id INT NULL,
    CONSTRAINT fk_promoter_app_account
        FOREIGN KEY (account_id) REFERENCES account(account_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_promoter_app_admin
        FOREIGN KEY (reviewed_by_admin_id) REFERENCES admin(admin_id)
        ON DELETE SET NULL
        ON UPDATE CASCADE
);

-- =========================================================
-- 25. VERIFICATION_DOCUMENT
-- =========================================================
CREATE TABLE verification_document (
    document_id INT AUTO_INCREMENT PRIMARY KEY,
    artist_application_id INT NULL,
    promoter_application_id INT NULL,
    document_type VARCHAR(50) NOT NULL,
    file_url VARCHAR(255) NOT NULL,
    submitted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    verification_note TEXT,
    CONSTRAINT fk_vd_artist_app
        FOREIGN KEY (artist_application_id) REFERENCES artist_application(artist_application_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_vd_promoter_app
        FOREIGN KEY (promoter_application_id) REFERENCES promoter_application(promoter_application_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
);

-- =========================================================
-- 26. INTERVIEW
-- =========================================================
CREATE TABLE interview (
    interview_id INT AUTO_INCREMENT PRIMARY KEY,
    artist_application_id INT NULL,
    promoter_application_id INT NULL,
    handled_by_admin_id INT NOT NULL,
    interview_type VARCHAR(20) NOT NULL,
    scheduled_at DATETIME NOT NULL,
    location_or_link VARCHAR(255),
    result VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    CONSTRAINT fk_interview_artist_app
        FOREIGN KEY (artist_application_id) REFERENCES artist_application(artist_application_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_interview_promoter_app
        FOREIGN KEY (promoter_application_id) REFERENCES promoter_application(promoter_application_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_interview_admin
        FOREIGN KEY (handled_by_admin_id) REFERENCES admin(admin_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
);




CREATE TABLE event_audit (
    audit_id INT AUTO_INCREMENT PRIMARY KEY,
    event_id INT NOT NULL,
    action_type VARCHAR(20) NOT NULL,
    old_title VARCHAR(160),
    new_title VARCHAR(160),
    old_start_datetime DATETIME,
    new_start_datetime DATETIME,
    old_end_datetime DATETIME,
    new_end_datetime DATETIME,
    changed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);
