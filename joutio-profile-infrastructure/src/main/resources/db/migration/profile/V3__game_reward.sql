-- Récompense d'un duel en vue vivante : le journal d'attribution porte ce que la vue expose, et chaque partie a une
-- version incrémentée à chaque écriture de sa récompense.
ALTER TABLE public.progression_awarded_game ADD COLUMN topic_id character varying(255);
ALTER TABLE public.progression_awarded_game ADD COLUMN game_score integer DEFAULT 0 NOT NULL;
ALTER TABLE public.progression_awarded_game ADD COLUMN bot_game boolean DEFAULT false NOT NULL;

CREATE INDEX progression_awarded_game_game_idx ON public.progression_awarded_game (game_id);

CREATE TABLE public.game_reward_version (
    game_id character varying(255) PRIMARY KEY,
    version bigint NOT NULL
);

COMMENT ON TABLE public.game_reward_version IS 'Version de la récompense d''une partie (vue vivante GetGameRewardQuery)';
