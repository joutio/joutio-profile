-- Déblocages datés (titres thématiques et badges), projetés par le groupe unlock-projection.
-- Aucune clé étrangère vers progression_entry : un badge ou un titre peut arriver avant la
-- ligne de progression (agrégats distincts), il ne doit jamais être perdu.

CREATE TABLE title_unlock (
    user_id character varying(255) NOT NULL,
    topic_id character varying(255) NOT NULL,
    tier character varying(16) NOT NULL,
    unlocked_at timestamp with time zone NOT NULL,
    CONSTRAINT title_unlock_pkey PRIMARY KEY (user_id, topic_id, tier)
);

COMMENT ON TABLE title_unlock IS 'Titres thématiques débloqués (palier de maîtrise par sujet), datés. Fer = première partie humaine terminée dans le sujet';

CREATE INDEX idx_title_unlock_user_date ON title_unlock (user_id, unlocked_at DESC);

CREATE TABLE badge_unlock (
    user_id character varying(255) NOT NULL,
    badge character varying(50) NOT NULL,
    earned_at timestamp with time zone NOT NULL,
    CONSTRAINT badge_unlock_pkey PRIMARY KEY (user_id, badge)
);

COMMENT ON TABLE badge_unlock IS 'Badges obtenus, datés (source de vérité des badges du read model)';

-- Remplacée par badge_unlock (la prod repart d'un event store vide : aucune copie).
DROP TABLE IF EXISTS progression_badge;

COMMENT ON TABLE progression_entry IS 'Progression d''un joueur : XP par sujet, Elo, statistiques de duel et compteurs de badges (les badges eux-mêmes vivent dans badge_unlock)';
