-- Badge Recruteur (design/specs/screens/partage.md § 6) : filleuls qualifiés (premier duel terminé).
ALTER TABLE progression_entry
    ADD COLUMN referrals_qualified integer DEFAULT 0 NOT NULL;

COMMENT ON COLUMN progression_entry.referrals_qualified IS 'Filleuls qualifiés, premier duel terminé (badge Recruteur)';

-- Rattachement et fin d'onboarding peuvent arriver dans n'importe quel ordre : le parrain est crédité une fois.
CREATE TABLE recruiter_tracking (
    user_id character varying(255) NOT NULL PRIMARY KEY,
    referrer_id character varying(255),
    onboarded_at timestamp without time zone,
    credited_at timestamp without time zone
);
