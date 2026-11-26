-- Version des vues surveillées par les caches du BFF (profil, progression, récompenses) : un compteur par vue et par
-- joueur, incrémenté à chaque écriture qui change la vue.
CREATE TABLE public.live_version (
    view character varying(64) NOT NULL,
    user_id character varying(255) NOT NULL,
    version bigint NOT NULL,
    PRIMARY KEY (view, user_id)
);

CREATE INDEX live_version_user_idx ON public.live_version (user_id);

COMMENT ON TABLE public.live_version IS 'Version des vues surveillées d''un joueur (WatchProfilesQuery, WatchProgressionsQuery, WatchPlayerRewardsQuery)';
