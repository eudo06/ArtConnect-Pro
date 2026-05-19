USE artconnect;

-- =========================================================
-- TABLE : community_post
-- Forum communautaire — messages postés par les membres
-- =========================================================
CREATE TABLE IF NOT EXISTS community_post (
    post_id      INT AUTO_INCREMENT PRIMARY KEY,
    community_id INT NOT NULL,
    member_id    INT NOT NULL,
    content      TEXT NOT NULL,
    posted_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_post_community
        FOREIGN KEY (community_id) REFERENCES community(community_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_post_member
        FOREIGN KEY (member_id) REFERENCES community_member(member_id)
        ON DELETE CASCADE ON UPDATE CASCADE
);

-- Index pour accélérer le chargement des posts par communauté
CREATE INDEX idx_post_community_id ON community_post(community_id);
CREATE INDEX idx_post_posted_at    ON community_post(posted_at);

-- =========================================================
-- DONNÉES D'EXEMPLE
-- =========================================================
INSERT INTO community_post (community_id, member_id, content, posted_at) VALUES
-- ArtConnect Global (1)
(1, 1, 'Bienvenue à tous sur ArtConnect ! Partagez vos créations et vos coups de cœur.', '2026-01-10 09:00:00'),
(1, 2, 'Super plateforme ! J''ai découvert des artistes locaux incroyables.', '2026-01-11 14:30:00'),
(1, 3, 'Quelqu''un sait s''il y a des expositions prévues en mars ?', '2026-01-12 10:15:00'),
(1, 1, 'Oui ! Regardez l''onglet Exhibitions, il y a déjà plusieurs événements planifiés.', '2026-01-12 11:00:00'),

-- Painters Hub (2)
(2, 2, 'Je cherche des conseils sur les techniques à l''huile. Des recommandations ?', '2026-02-01 08:45:00'),
(2, 1, 'Je conseille de commencer par des fonds neutres. L''atelier de Claude Monet est excellent !', '2026-02-01 09:30:00'),
(2, 3, 'J''ai participé au workshop Mastering Oil Painting — vraiment top !', '2026-02-02 16:00:00'),
(2, 2, 'Merci pour les retours, je vais m''inscrire à la prochaine session.', '2026-02-03 10:00:00'),

-- Photo Collective (4)
(4, 3, 'Partage de mes dernières photos de rue à Paris. Vos avis ?', '2026-03-05 18:00:00'),
(4, 1, 'Très belles compositions ! La lumière naturelle est vraiment bien capturée.', '2026-03-06 09:15:00'),
(4, 2, 'Bravo ! Le workshop Photography in Nature m''a aussi beaucoup appris.', '2026-03-07 14:30:00'),

-- Modern Art Lovers (5)
(5, 1, 'Que pensez-vous de l''exposition Renaissance Revival ? J''y suis allé hier.', '2026-04-01 20:00:00'),
(5, 3, 'Magnifique ! La salle sur les fresques romaines était impressionnante.', '2026-04-02 08:00:00'),
(5, 2, 'Je n''ai pas encore pu y aller, c''est encore ouvert ?', '2026-04-02 12:00:00'),
(5, 1, 'Oui jusqu''au 20 février ! Foncez, ça vaut vraiment le détour.', '2026-04-02 13:00:00');