-- Unicité des pseudonymes (2026-11-12) : réservation par clé normalisée (minuscules, sans espaces
-- en bord), sur le modèle de email_claim (identity).
CREATE TABLE pseudonym_claim (
    pseudonym_key character varying(100) NOT NULL PRIMARY KEY,
    user_id character varying(255) NOT NULL,
    created_at timestamp without time zone NOT NULL
);

CREATE INDEX idx_pseudonym_claim_user ON pseudonym_claim (user_id);

COMMENT ON TABLE pseudonym_claim IS 'Réservation unique d''un pseudonyme (clé insensible à la casse)';

-- Existant toléré : le profil le plus ancien réserve la clé, les doublons historiques restent tels
-- quels (non réservés) jusqu'à leur prochain changement de pseudonyme.
INSERT INTO pseudonym_claim (pseudonym_key, user_id, created_at)
SELECT DISTINCT ON (lower(trim(pseudonym))) lower(trim(pseudonym)), user_id, now()
FROM profile_entry
WHERE trim(pseudonym) <> ''
ORDER BY lower(trim(pseudonym)), created_at, user_id
ON CONFLICT (pseudonym_key) DO NOTHING;
