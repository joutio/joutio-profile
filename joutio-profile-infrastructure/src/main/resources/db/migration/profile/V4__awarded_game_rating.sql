-- Récompense d'un duel en vue vivante : l'Elo de chaque joueur après la partie, pour que la vue soit complète sans
-- l'instantané de game. Nulle pour les attributions antérieures.
ALTER TABLE public.progression_awarded_game ADD COLUMN rating integer;
