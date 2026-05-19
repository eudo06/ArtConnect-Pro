USE artconnect;

-- =========================================================
-- 1) ACCOUNT
-- =========================================================
INSERT INTO account (account_id, email, password_hash, account_status, created_at) VALUES
(1,  'admin1@artconnect.com',      'hash_admin1',   'ACTIVE',  '2025-01-01 09:00:00'),
(2,  'admin2@artconnect.com',      'hash_admin2',   'ACTIVE',  '2025-01-01 09:10:00'),

(3,  'promoter1@artconnect.com',   'hash_prom1',    'ACTIVE',  '2025-01-02 10:00:00'),
(4,  'promoter2@artconnect.com',   'hash_prom2',    'ACTIVE',  '2025-01-02 10:10:00'),
(5,  'promoter3@artconnect.com',   'hash_prom3',    'ACTIVE',  '2025-01-02 10:20:00'),
(6,  'promoter4@artconnect.com',   'hash_prom4',    'ACTIVE',  '2025-01-02 10:30:00'),

(7,  'monet@artconnect.com',       'hash_art1',     'ACTIVE',  '2025-01-03 08:00:00'),
(8,  'kahlo@artconnect.com',       'hash_art2',     'ACTIVE',  '2025-01-03 08:05:00'),
(9,  'vinci@artconnect.com',       'hash_art3',     'ACTIVE',  '2025-01-03 08:10:00'),
(10, 'rodin@artconnect.com',       'hash_art4',     'ACTIVE',  '2025-01-03 08:15:00'),
(11, 'ansel@artconnect.com',       'hash_art5',     'ACTIVE',  '2025-01-03 08:20:00'),
(12, 'picasso@artconnect.com',     'hash_art6',     'ACTIVE',  '2025-01-03 08:25:00'),
(13, 'vangogh@artconnect.com',     'hash_art7',     'ACTIVE',  '2025-01-03 08:30:00'),
(14, 'dali@artconnect.com',        'hash_art8',     'ACTIVE',  '2025-01-03 08:35:00'),
(15, 'renoir@artconnect.com',      'hash_art9',     'ACTIVE',  '2025-01-03 08:40:00'),
(16, 'banksy@artconnect.com',      'hash_art10',    'ACTIVE',  '2025-01-03 08:45:00'),

(17, 'alice@artconnect.com',       'hash_mem1',     'ACTIVE',  '2025-01-04 09:00:00'),
(18, 'bob@artconnect.com',         'hash_mem2',     'ACTIVE',  '2025-01-04 09:05:00'),
(19, 'charlie@artconnect.com',     'hash_mem3',     'ACTIVE',  '2025-01-04 09:10:00'),
(20, 'diana@artconnect.com',       'hash_mem4',     'ACTIVE',  '2025-01-04 09:15:00'),
(21, 'emma@artconnect.com',        'hash_mem5',     'ACTIVE',  '2025-01-04 09:20:00'),
(22, 'farid@artconnect.com',       'hash_mem6',     'ACTIVE',  '2025-01-04 09:25:00'),
(23, 'giulia@artconnect.com',      'hash_mem7',     'ACTIVE',  '2025-01-04 09:30:00'),
(24, 'hugo@artconnect.com',        'hash_mem8',     'ACTIVE',  '2025-01-04 09:35:00'),
(25, 'ines@artconnect.com',        'hash_mem9',     'ACTIVE',  '2025-01-04 09:40:00'),
(26, 'jules@artconnect.com',       'hash_mem10',    'ACTIVE',  '2025-01-04 09:45:00'),

(27, 'futureartist1@artconnect.com','hash_fa1',     'PENDING', '2025-01-05 10:00:00'),
(28, 'futureartist2@artconnect.com','hash_fa2',     'PENDING', '2025-01-05 10:05:00'),
(29, 'futureprom1@artconnect.com', 'hash_fp1',      'PENDING', '2025-01-05 10:10:00'),
(30, 'futureprom2@artconnect.com', 'hash_fp2',      'PENDING', '2025-01-05 10:15:00');

-- =========================================================
-- 2) ADMIN
-- =========================================================
INSERT INTO admin (admin_id, account_id, full_name, phone) VALUES
(1, 1, 'Admin Principal', '0600000001'),
(2, 2, 'Admin Technique', '0600000002');

-- =========================================================
-- 3) COMMUNITY
-- =========================================================
INSERT INTO community (community_id, parent_community_id, name, description, community_type) VALUES
(1, NULL, 'ArtConnect Global', 'Communauté générale de la plateforme', 'GENERAL'),
(2, 1, 'Painters Hub', 'Communauté des peintres', 'ART'),
(3, 1, 'Sculptors Circle', 'Communauté des sculpteurs', 'ART'),
(4, 1, 'Photo Collective', 'Communauté des photographes', 'ART'),
(5, 1, 'Modern Art Lovers', 'Amateurs d’art moderne', 'INTEREST'),
(6, 1, 'Classic Art Society', 'Passionnés d’art classique', 'INTEREST'),
(7, 1, 'Street Art Network', 'Communauté street art', 'ART'),
(8, 1, 'Workshop Fans', 'Membres actifs des ateliers', 'INTEREST');

-- =========================================================
-- 4) COMMUNITY MEMBER
-- =========================================================
INSERT INTO community_member (member_id, account_id, full_name, birth_year, phone, city, membership_type) VALUES
(1, 17, 'Alice Dupont',    1996, '0611111111', 'Paris',      'PREMIUM'),
(2, 18, 'Bob Martin',      1992, '0611111112', 'Lyon',       'FREE'),
(3, 19, 'Charlie Brown',   1990, '0611111113', 'Marseille',  'FREE'),
(4, 20, 'Diana Lopez',     1998, '0611111114', 'Bruxelles',  'PREMIUM'),
(5, 21, 'Emma Rossi',      1995, '0611111115', 'Rome',       'FREE'),
(6, 22, 'Farid Haddad',    1989, '0611111116', 'Casablanca', 'PREMIUM'),
(7, 23, 'Giulia Conti',    2000, '0611111117', 'Milan',      'FREE'),
(8, 24, 'Hugo Bernard',    1994, '0611111118', 'Paris',      'PREMIUM'),
(9, 25, 'Ines Leroy',      1997, '0611111119', 'Bordeaux',   'FREE'),
(10, 26, 'Jules Petit',    1993, '0611111120', 'Nantes',     'FREE');

-- =========================================================
-- 5) PROMOTER
-- =========================================================
INSERT INTO promoter (promoter_id, account_id, full_name, organization_name, phone, city, status, validated_by_admin_id, validation_date) VALUES
(1, 3, 'Claire Moreau', 'ArtWorld Events',  '0620000001', 'Paris',    'ACTIVE', 1, '2025-01-10 11:00:00'),
(2, 4, 'David Green',   'Urban Canvas',     '0620000002', 'London',   'ACTIVE', 1, '2025-01-10 11:10:00'),
(3, 5, 'Sara Benali',   'Mediterranean Art','0620000003', 'Barcelona','ACTIVE', 2, '2025-01-10 11:20:00'),
(4, 6, 'Thomas White',  'Creative Pulse',   '0620000004', 'New York', 'ACTIVE', 2, '2025-01-10 11:30:00');

-- =========================================================
-- 6) ARTIST
-- =========================================================


INSERT INTO artist (artist_id, account_id, stage_name, bio, birth_year, contact_email, phone, city, website, social_media, is_active, validated_by_admin_id, validation_date) VALUES
(1, 7,  'Claude Monet',   'Peintre impressionniste.',         1840, 'contact@monet.art',   '0630000001', 'Paris',     'www.monet.art',   '@monet',   TRUE, 1, '2025-01-12 09:00:00'),
(2, 8,  'Frida Kahlo',    'Artiste mexicaine emblématique.', 1907, 'contact@kahlo.art',   '0630000002', 'Mexico',    'www.kahlo.art',   '@kahlo',   TRUE, 1, '2025-01-12 09:05:00'),
(3, 9,  'Leonardo Vinci', 'Maître de la Renaissance.',       1452, 'contact@vinci.art',   '0630000003', 'Florence',  'www.vinci.art',   '@vinci',   TRUE, 1, '2025-01-12 09:10:00'),
(4, 10, 'Auguste Rodin',  'Sculpteur français.',             1840, 'contact@rodin.art',   '0630000004', 'Paris',     'www.rodin.art',   '@rodin',   TRUE, 1, '2025-01-12 09:15:00'),
(5, 11, 'Ansel Adams',    'Photographe de paysages.',        1902, 'contact@ansel.art',   '0630000005', 'San Jose',  'www.ansel.art',   '@ansel',   TRUE, 2, '2025-01-12 09:20:00'),
(6, 12, 'Pablo Picasso',  'Peintre et sculpteur espagnol.',  1881, 'contact@picasso.art', '0630000006', 'Malaga',    'www.picasso.art', '@picasso', TRUE, 2, '2025-01-12 09:25:00'),
(7, 13, 'Vincent van Gogh' ,'Peintre postimpressionniste.',   1853, 'contact@vangogh.art', '0630000007', 'Amsterdam', 'www.vangogh.art', '@vangogh', TRUE, 2, '2025-01-12 09:30:00'),
(8, 14, 'Salvador Dali',  'Artiste surréaliste.',            1904, 'contact@dali.art',    '0630000008', 'Figueres',  'www.dali.art',    '@dali',    TRUE, 1, '2025-01-12 09:35:00'),
(9, 15, 'Pierre Renoir',  'Peintre classique.',              1841, 'contact@renoir.art',  '0630000009', 'Paris',     'www.renoir.art',  '@renoir',  TRUE, 2, '2025-01-12 09:40:00'),
(10,16, 'Banksy',         'Artiste urbain contemporain.',    1974, 'contact@banksy.art',  '0630000010', 'Bristol',   'www.banksy.art',  '@banksy',  TRUE, 1, '2025-01-12 09:45:00');

-- =========================================================
-- 7) DISCIPLINE
-- =========================================================
INSERT INTO discipline (discipline_id, name) VALUES
(1, 'Painting'),
(2, 'Sculpture'),
(3, 'Photography'),
(4, 'Street Art'),
(5, 'Mixed Media'),
(6, 'Digital Art');

-- =========================================================
-- 8) ARTIST_DISCIPLINE
-- =========================================================
INSERT INTO artist_discipline (artist_id, discipline_id) VALUES
(1,1),
(2,1),(2,5),
(3,1),
(4,2),
(5,3),
(6,1),(6,2),
(7,1),
(8,1),(8,5),
(9,1),
(10,4),(10,5);

-- =========================================================
-- 9) MEMBER_DISCIPLINE
-- =========================================================
INSERT INTO member_discipline (member_id, discipline_id) VALUES
(1,1),(1,3),
(2,2),
(3,1),(3,4),
(4,5),
(5,1),(5,2),
(6,3),
(7,6),
(8,4),(8,5),
(9,1),
(10,2),(10,3);

-- =========================================================
-- 10) GALLERY
-- =========================================================
INSERT INTO gallery (gallery_id, name, address, owner_name, opening_hours, contact_phone, rating, website) VALUES
(1, 'Louvre Art House',      'Rue de Rivoli, Paris',          'Marie Laurent', '09:00-18:00', '0640000001', 4.90, 'www.louvrearthouse.com'),
(2, 'The British Gallery',   'Russell St, London',            'Henry Clark',   '10:00-19:00', '0640000002', 4.70, 'www.britishgallery.com'),
(3, 'Metropolitan Hub',      '5th Ave, New York',             'Susan Miller',  '09:00-20:00', '0640000003', 4.80, 'www.metropolitanhub.com'),
(4, 'Barcelona Arts Center', 'Gran Via, Barcelona',           'Jorge Diaz',    '10:00-18:30', '0640000004', 4.60, 'www.barcaarts.com'),
(5, 'Roma Classic Space',    'Via del Corso, Rome',           'Lucia Romano',  '09:30-18:00', '0640000005', 4.75, 'www.romaclassic.com'),
(6, 'Urban Walls Studio',    'Shoreditch, London',            'Kevin Stone',   '11:00-21:00', '0640000006', 4.55, 'www.urbanwalls.com');

-- =========================================================
-- 11) EVENT
-- =========================================================
INSERT INTO event (event_id, promoter_id, title, description, start_datetime, end_datetime, status, capacity, location) VALUES
(1,  1, 'Impressionist Dreams',     'Exposition autour de l’impressionnisme.',        '2026-01-30 10:00:00', '2026-02-05 18:00:00', 'PLANNED', 200, 'Paris'),
(2,  1, 'Renaissance Revival',      'Redécouverte des maîtres classiques.',           '2026-02-10 09:00:00', '2026-02-20 18:00:00', 'PLANNED', 180, 'Rome'),
(3,  2, 'Sculpting the Soul',       'Exposition de sculptures modernes et classiques.','2026-03-01 10:00:00', '2026-03-10 18:00:00', 'PLANNED', 150, 'London'),
(4,  3, 'Light and Shadow',         'Exposition photo et installations visuelles.',   '2026-03-12 10:00:00', '2026-03-18 18:00:00', 'PLANNED', 120, 'Barcelona'),
(5,  4, 'Street Voices',            'Exposition dédiée au street art.',               '2026-03-20 11:00:00', '2026-03-28 19:00:00', 'PLANNED', 250, 'London'),
(6,  1, 'Mastering Oil Painting',   'Atelier de peinture à l’huile.',                 '2026-04-01 10:00:00', '2026-04-01 16:00:00', 'PLANNED', 20,  'Paris'),
(7,  2, 'Modern Sculpture Lab',     'Atelier de sculpture avancée.',                  '2026-04-05 09:00:00', '2026-04-05 17:00:00', 'PLANNED', 15,  'London'),
(8,  3, 'Photography in Nature',    'Atelier de photographie extérieure.',            '2026-04-08 08:30:00', '2026-04-08 15:30:00', 'PLANNED', 25,  'Barcelona'),
(9,  4, 'Surreal Colors Workshop',  'Atelier créatif autour du surréalisme.',         '2026-04-12 10:00:00', '2026-04-12 15:00:00', 'PLANNED', 18,  'New York'),
(10, 1, 'Classic Techniques',       'Atelier de techniques classiques.',              '2026-04-15 10:00:00', '2026-04-15 16:00:00', 'PLANNED', 22,  'Rome'),
(11, 2, 'Urban Mural Session',      'Session pratique sur fresques urbaines.',        '2026-04-18 11:00:00', '2026-04-18 18:00:00', 'PLANNED', 30,  'London'),
(12, 3, 'Women in Art Showcase',    'Exposition thématique autour des artistes femmes.','2026-05-01 10:00:00','2026-05-08 18:00:00', 'PLANNED', 160, 'Barcelona'),
(13, 4, 'Masters and Modernity',    'Dialogue entre classiques et contemporains.',    '2026-05-10 10:00:00', '2026-05-18 18:00:00', 'PLANNED', 210, 'New York'),
(14, 1, 'Digital Inspiration',      'Atelier d’introduction à l’art digital.',        '2026-05-22 09:00:00', '2026-05-22 14:00:00', 'PLANNED', 24,  'Paris');

-- =========================================================
-- 12) EXHIBITION
-- =========================================================
INSERT INTO exhibition (event_id, gallery_id, theme, curator_name) VALUES
(1, 1, 'Light and Color',               'Marie Laurent'),
(2, 5, 'Classic Renaissance',           'Lucia Romano'),
(3, 2, 'Modern & Classical Sculpture',  'Henry Clark'),
(4, 4, 'Light and Shadow',              'Jorge Diaz'),
(5, 6, 'Street Expression',             'Kevin Stone'),
(12,4, 'Women, Identity and Art',       'Sara Benali'),
(13,3, 'Dialogue Across Centuries',     'Thomas White');

-- =========================================================
-- 13) WORKSHOP
-- =========================================================
INSERT INTO workshop (event_id, instructor_artist_id, duration_minutes, price, level) VALUES
(6,  1, 360, 150.00, 'Intermediate'),
(7,  4, 480, 200.00, 'Advanced'),
(8,  5, 420, 120.00, 'Beginner'),
(9,  8, 300, 180.00, 'Intermediate'),
(10, 3, 360, 160.00, 'Intermediate'),
(11, 10,420, 140.00, 'Advanced'),
(14, 6, 300, 110.00, 'Beginner');

-- =========================================================
-- 14) ARTIST_EVENT
-- =========================================================
INSERT INTO artist_event (artist_id, event_id, role_in_event) VALUES
(1, 1,  'Headliner'),
(7, 1,  'Guest Artist'),
(9, 1,  'Exhibitor'),

(3, 2,  'Headliner'),
(9, 2,  'Guest Artist'),

(4, 3,  'Headliner'),
(6, 3,  'Guest Sculptor'),

(5, 4,  'Headliner'),
(8, 4,  'Visual Guest'),

(10,5,  'Headliner'),
(6, 5,  'Guest Artist'),

(2, 12, 'Headliner'),
(8, 12, 'Guest Artist'),

(3, 13, 'Master Artist'),
(6, 13, 'Modern Guest'),
(10,13, 'Urban Guest');

-- =========================================================
-- 15) ARTWORK
-- =========================================================

INSERT INTO artwork (artwork_id, artist_id, title, creation_year, type, medium, dimensions, description, price, status) VALUES
(1,  1,  'Water Lilies',                1906, 'Painting',    'Oil on canvas', '200x180', 'Œuvre impressionniste.',                 4000000.00, 'FOR_SALE'),
(2,  1,  'Sunrise Reflections',         1904, 'Painting',    'Oil on canvas', '180x120', 'Paysage lumineux.',                      3200000.00, 'FOR_SALE'),
(3,  2,  'The Two Fridas',              1939, 'Painting',    'Oil on canvas', '173x173', 'Double autoportrait.',                   5000000.00, 'FOR_SALE'),
(4,  2,  'Broken Column',               1944, 'Painting',    'Oil on masonite','40x30',  'Œuvre intime et symbolique.',            2800000.00, 'FOR_SALE'),
(5,  3,  'Mona Lisa',                   1503, 'Painting',    'Oil on wood',   '77x53',   'Portrait iconique.',                     850000000.00, 'NOT_FOR_SALE'),
(6,  3,  'The Last Supper Study',       1495, 'Painting',    'Mixed',         '250x100', 'Étude préparatoire.',                    450000000.00, 'NOT_FOR_SALE'),
(7,  4,  'The Thinker',                 1904, 'Sculpture',   'Bronze',        '186x98',  'Sculpture célèbre.',                     15000000.00, 'FOR_SALE'),
(8,  4,  'The Kiss',                    1882, 'Sculpture',   'Marble',        '180x110', 'Sculpture romantique.',                  17000000.00, 'FOR_SALE'),
(9,  5,  'Monolith, Half Dome',         1927, 'Photography', 'Gelatin silver','40x50',   'Photographie de paysage.',               100000.00, 'FOR_SALE'),
(10, 5,  'Moonrise',                    1941, 'Photography', 'Silver print',  '45x60',   'Paysage noir et blanc.',                 120000.00, 'FOR_SALE'),
(11, 6,  'Guernica Fragment',           1937, 'Painting',    'Oil on canvas', '120x90',  'Fragment inspiré de Guernica.',          8000000.00, 'FOR_SALE'),
(12, 6,  'Blue Figure',                 1935, 'Painting',    'Oil on canvas', '100x70',  'Figure stylisée.',                       4200000.00, 'FOR_SALE'),
(13, 7,  'Starry Memory',               1889, 'Painting',    'Oil on canvas', '92x73',   'Inspiré de la nuit étoilée.',            6100000.00, 'FOR_SALE'),
(14, 7,  'Golden Field',                1890, 'Painting',    'Oil on canvas', '80x60',   'Paysage rural lumineux.',                3500000.00, 'FOR_SALE'),
(15, 8,  'Melting Time',                1931, 'Painting',    'Oil on canvas', '65x50',   'Œuvre surréaliste.',                     7300000.00, 'FOR_SALE'),
(16, 8,  'Dream Landscape',             1934, 'Painting',    'Oil on canvas', '90x60',   'Paysage onirique.',                      3900000.00, 'FOR_SALE'),
(17, 9,  'Paris Garden',                1885, 'Painting',    'Oil on canvas', '95x70',   'Scène douce et classique.',              2700000.00, 'FOR_SALE'),
(18, 9,  'River Walk',                  1888, 'Painting',    'Oil on canvas', '85x65',   'Promenade au bord de l’eau.',            2500000.00, 'FOR_SALE'),
(19, 10, 'Urban Child',                 2005, 'Street Art',  'Spray paint',   '210x150', 'Pochoir urbain.',                        900000.00, 'FOR_SALE'),
(20, 10, 'Silent Wall',                 2010, 'Street Art',  'Spray paint',   '230x160', 'Fresque engagée.',                       1100000.00, 'FOR_SALE');

-- =========================================================
-- 16) ARTWORK_TAG
-- =========================================================
INSERT INTO artwork_tag (tag_id, name) VALUES
(1, 'Impressionism'),
(2, 'Portrait'),
(3, 'Landscape'),
(4, 'Modern'),
(5, 'Classic'),
(6, 'Surrealism'),
(7, 'Street'),
(8, 'BlackAndWhite'),
(9, 'Nature'),
(10,'Identity');

-- =========================================================
-- 17) ARTWORK_TAG_MAP
-- =========================================================
INSERT INTO artwork_tag_map (artwork_id, tag_id) VALUES
(1,1),(1,3),(1,9),
(2,1),(2,3),
(3,2),(3,10),
(4,2),(4,10),
(5,2),(5,5),
(6,5),
(7,4),
(8,4),
(9,3),(9,8),(9,9),
(10,8),(10,9),
(11,4),
(12,4),
(13,1),(13,3),
(14,3),
(15,6),
(16,6),
(17,1),(17,3),
(18,3),
(19,7),(19,4),
(20,7);

-- =========================================================
-- 18) EXHIBITION_ARTWORK
-- =========================================================
INSERT INTO exhibition_artwork (exhibition_event_id, artwork_id) VALUES
(1,1),(1,2),(1,13),(1,17),
(2,5),(2,6),(2,17),
(3,7),(3,8),(3,11),
(4,9),(4,10),(4,15),
(5,19),(5,20),(5,12),
(12,3),(12,4),(12,15),
(13,5),(13,11),(13,19);

-- =========================================================
-- 19) BOOKING
-- =========================================================
INSERT INTO booking (booking_id, workshop_event_id, member_id, booking_date, payment_status) VALUES
(1,  6,  1, '2026-03-20 09:00:00', 'PAID'),
(2,  6,  2, '2026-03-20 09:02:00', 'PAID'),
(3,  6,  3, '2026-03-20 09:05:00', 'PENDING'),
(4,  6,  4, '2026-03-20 09:07:00', 'PAID'),
(5,  6,  5, '2026-03-20 09:10:00', 'PAID'),

(6,  7,  1, '2026-03-21 10:00:00', 'PAID'),
(7,  7,  6, '2026-03-21 10:05:00', 'PAID'),
(8,  7,  7, '2026-03-21 10:10:00', 'PENDING'),

(9,  8,  2, '2026-03-22 11:00:00', 'PAID'),
(10, 8,  3, '2026-03-22 11:05:00', 'PAID'),
(11, 8,  8, '2026-03-22 11:10:00', 'PAID'),
(12, 8,  9, '2026-03-22 11:15:00', 'PENDING'),

(13, 9,  4, '2026-03-23 12:00:00', 'PAID'),
(14, 9,  5, '2026-03-23 12:05:00', 'PENDING'),
(15, 9,  10,'2026-03-23 12:10:00', 'PAID'),

(16, 10, 1, '2026-03-24 13:00:00', 'PAID'),
(17, 10, 2, '2026-03-24 13:05:00', 'PENDING'),
(18, 10, 6, '2026-03-24 13:10:00', 'PAID'),

(19, 11, 3, '2026-03-25 14:00:00', 'PAID'),
(20, 11, 7, '2026-03-25 14:05:00', 'PAID'),
(21, 11, 8, '2026-03-25 14:10:00', 'PENDING'),

(22, 14, 4, '2026-03-26 15:00:00', 'PAID'),
(23, 14, 5, '2026-03-26 15:05:00', 'PAID'),
(24, 14, 9, '2026-03-26 15:10:00', 'PENDING');

-- =========================================================
-- 20) REVIEW
-- =========================================================
INSERT INTO review (review_id, member_id, artwork_id, rating, comment, review_date) VALUES
(1,  1,  1, 5, 'Magnifique lumière et profondeur.',          '2026-01-15'),
(2,  2,  1, 4, 'Très belle œuvre impressionniste.',          '2026-01-16'),
(3,  3,  3, 5, 'Émouvante et puissante.',                    '2026-01-17'),
(4,  4,  5, 5, 'Un chef-d’œuvre absolu.',                    '2026-01-18'),
(5,  5,  7, 4, 'Belle présence sculpturale.',                '2026-01-19'),
(6,  6,  9, 5, 'Superbe travail photographique.',            '2026-01-20'),
(7,  7,  11,4, 'Très moderne et expressif.',                 '2026-01-21'),
(8,  8,  13,5, 'Couleurs magnifiques.',                      '2026-01-22'),
(9,  9,  15,4, 'Univers fascinant et étrange.',              '2026-01-23'),
(10, 10, 19,5, 'Street art très percutant.',                 '2026-01-24'),
(11, 1,  20,4, 'Message fort et belle exécution.',           '2026-01-25'),
(12, 2,  10,5, 'Photo très inspirante.',                     '2026-01-26'),
(13, 3,  8, 4, 'Sculpture élégante.',                        '2026-01-27'),
(14, 4,  17,4, 'Ambiance calme et raffinée.',                '2026-01-28'),
(15, 5,  2, 5, 'Très beau jeu de reflets.',                  '2026-01-29');

-- =========================================================
-- 21) MEMBER_COMMUNITY
-- =========================================================
INSERT INTO member_community (member_id, community_id, joined_at, membership_role) VALUES
(1, 1, '2025-02-01 09:00:00', 'member'),
(1, 2, '2025-02-01 09:05:00', 'moderator'),
(1, 8, '2025-02-01 09:10:00', 'member'),

(2, 1, '2025-02-02 09:00:00', 'member'),
(2, 3, '2025-02-02 09:05:00', 'member'),

(3, 1, '2025-02-03 09:00:00', 'member'),
(3, 7, '2025-02-03 09:05:00', 'member'),

(4, 1, '2025-02-04 09:00:00', 'member'),
(4, 5, '2025-02-04 09:05:00', 'member'),

(5, 1, '2025-02-05 09:00:00', 'member'),
(5, 6, '2025-02-05 09:05:00', 'member'),

(6, 1, '2025-02-06 09:00:00', 'member'),
(6, 4, '2025-02-06 09:05:00', 'member'),

(7, 1, '2025-02-07 09:00:00', 'member'),
(7, 5, '2025-02-07 09:05:00', 'member'),

(8, 1, '2025-02-08 09:00:00', 'member'),
(8, 7, '2025-02-08 09:05:00', 'member'),

(9, 1, '2025-02-09 09:00:00', 'member'),
(9, 2, '2025-02-09 09:05:00', 'member'),

(10,1, '2025-02-10 09:00:00', 'member'),
(10,3, '2025-02-10 09:05:00', 'member');

-- =========================================================
-- 22) ARTIST_COMMUNITY
-- =========================================================
INSERT INTO artist_community (artist_id, community_id, assigned_at, assigned_by_admin_id) VALUES
(1, 2, '2025-02-11 10:00:00', 1),
(2, 2, '2025-02-11 10:05:00', 1),
(3, 6, '2025-02-11 10:10:00', 1),
(4, 3, '2025-02-11 10:15:00', 1),
(5, 4, '2025-02-11 10:20:00', 2),
(6, 5, '2025-02-11 10:25:00', 2),
(7, 2, '2025-02-11 10:30:00', 2),
(8, 5, '2025-02-11 10:35:00', 1),
(9, 6, '2025-02-11 10:40:00', 2),
(10,7, '2025-02-11 10:45:00', 1);

-- =========================================================
-- 23) ARTIST_APPLICATION
-- =========================================================
INSERT INTO artist_application (artist_application_id, account_id, discipline_requested, submission_date, status, reviewed_by_admin_id) VALUES
(1, 27, 'Digital Art', '2025-02-15 10:00:00', 'SUBMITTED', NULL),
(2, 28, 'Painting',    '2025-02-16 11:00:00', 'UNDER_REVIEW', 1);

-- =========================================================
-- 24) PROMOTER_APPLICATION
-- =========================================================
INSERT INTO promoter_application (promoter_application_id, account_id, organization_name, submission_date, status, reviewed_by_admin_id) VALUES
(1, 29, 'Future Stage Events', '2025-02-17 10:00:00', 'SUBMITTED', NULL),
(2, 30, 'Creative Loop Org',   '2025-02-18 11:00:00', 'UNDER_REVIEW', 2);

-- =========================================================
-- 25) VERIFICATION_DOCUMENT
-- =========================================================
INSERT INTO verification_document (document_id, artist_application_id, promoter_application_id, document_type, file_url, submitted_at, verification_note) VALUES
(1, 1, NULL, 'PORTFOLIO',      '/docs/artist_app_1_portfolio.pdf',    '2025-02-15 10:30:00', 'Portfolio reçu'),
(2, 1, NULL, 'ID_CARD',        '/docs/artist_app_1_id.pdf',           '2025-02-15 10:35:00', 'Pièce d’identité reçue'),
(3, 2, NULL, 'PORTFOLIO',      '/docs/artist_app_2_portfolio.pdf',    '2025-02-16 11:30:00', 'En cours de vérification'),
(4, NULL, 1, 'BUSINESS_LICENSE','/docs/promoter_app_1_license.pdf',   '2025-02-17 10:30:00', 'Licence transmise'),
(5, NULL, 2, 'BUSINESS_LICENSE','/docs/promoter_app_2_license.pdf',   '2025-02-18 11:30:00', 'Document valide'),
(6, NULL, 2, 'TAX_DOCUMENT',   '/docs/promoter_app_2_tax.pdf',        '2025-02-18 11:40:00', 'Document fiscal reçu');

-- =========================================================
-- 26) INTERVIEW
-- =========================================================
INSERT INTO interview (interview_id, artist_application_id, promoter_application_id, handled_by_admin_id, interview_type, scheduled_at, location_or_link, result) VALUES
(1, 2, NULL, 1, 'ONLINE',  '2025-02-20 14:00:00', 'meet.link/artist2',   'PENDING'),
(2, NULL, 2, 2, 'ONLINE',  '2025-02-21 15:00:00', 'meet.link/promoter2', 'PENDING');





 
