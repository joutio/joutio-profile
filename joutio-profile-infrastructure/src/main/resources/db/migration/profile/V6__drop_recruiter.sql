-- Parrainage et badge Recruteur supprimés (2026-11-14) : colonne et table du V5 abandonnées.
ALTER TABLE progression_entry DROP COLUMN IF EXISTS referrals_qualified;
DROP TABLE IF EXISTS recruiter_tracking;
