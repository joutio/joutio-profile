-- Plus de période provisoire « non classé » (2026-11-16) : facteur K fixe, Elo et palier dès le premier duel.
ALTER TABLE progression_entry DROP COLUMN IF EXISTS provisional;
