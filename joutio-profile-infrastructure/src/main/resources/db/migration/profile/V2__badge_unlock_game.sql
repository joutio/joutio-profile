-- Partie qui a débloqué un badge (séquence des badges du résultat) ; NULL hors duel.
ALTER TABLE public.badge_unlock ADD COLUMN game_id character varying(255);

COMMENT ON COLUMN public.badge_unlock.game_id IS 'Partie qui a débloqué le badge, NULL hors duel (abonnés, défis)';
