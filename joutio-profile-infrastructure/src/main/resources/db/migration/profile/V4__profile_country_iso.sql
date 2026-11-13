-- Pays du profil en code ISO 3166-1 alpha-2 strict (design/specs/screens/editer-profil.md § 4, 2026-11-13).
-- Les apps envoient déjà des codes ; les rares noms libres connus sont convertis, le reste est retiré.
-- Un rejeu de la projection applique la même conversion (CountryCodes.normalize).
UPDATE profile_entry SET country = NULLIF(TRIM(country), '');

UPDATE profile_entry SET country = UPPER(country)
WHERE country ~* '^[a-z]{2}$';

UPDATE profile_entry AS p SET country = m.code
FROM (VALUES
    ('algérie', 'DZ'), ('algeria', 'DZ'), ('allemagne', 'DE'), ('germany', 'DE'), ('belgique', 'BE'), ('belgium', 'BE'),
    ('bénin', 'BJ'), ('benin', 'BJ'), ('burkina faso', 'BF'), ('cameroun', 'CM'), ('cameroon', 'CM'), ('canada', 'CA'),
    ('comores', 'KM'), ('comoros', 'KM'), ('côte d''ivoire', 'CI'), ('cote d''ivoire', 'CI'), ('ivory coast', 'CI'),
    ('espagne', 'ES'), ('spain', 'ES'), ('états-unis', 'US'), ('etats-unis', 'US'), ('united states', 'US'), ('usa', 'US'),
    ('france', 'FR'), ('gabon', 'GA'), ('guinée', 'GN'), ('guinea', 'GN'), ('haïti', 'HT'), ('haiti', 'HT'),
    ('italie', 'IT'), ('italy', 'IT'), ('liban', 'LB'), ('lebanon', 'LB'), ('luxembourg', 'LU'), ('madagascar', 'MG'),
    ('mali', 'ML'), ('maroc', 'MA'), ('morocco', 'MA'), ('mauritanie', 'MR'), ('mauritania', 'MR'), ('monaco', 'MC'),
    ('niger', 'NE'), ('portugal', 'PT'), ('royaume-uni', 'GB'), ('united kingdom', 'GB'), ('uk', 'GB'),
    ('sénégal', 'SN'), ('senegal', 'SN'), ('suisse', 'CH'), ('switzerland', 'CH'), ('togo', 'TG'),
    ('tunisie', 'TN'), ('tunisia', 'TN')
) AS m(name, code)
WHERE LOWER(p.country) = m.name;

UPDATE profile_entry SET country = NULL
WHERE country IS NOT NULL AND country !~ '^[A-Z]{2}$';

ALTER TABLE profile_entry ALTER COLUMN country TYPE character varying(2);

COMMENT ON COLUMN profile_entry.country IS 'Pays affiché sur le profil : code ISO 3166-1 alpha-2 ; NULL = non affiché';
