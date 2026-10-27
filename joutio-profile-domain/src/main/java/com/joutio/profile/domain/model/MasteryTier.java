package com.joutio.profile.domain.model;

import java.util.Optional;

/**
 * Palier de **maîtrise d'un sujet** : échelle de rareté portée par l'XP cumulée **dans le
 * sujet**. C'est le support des **titres thématiques** : le titre affiché est un palier + le nom
 * du sujet (cf. {@link PlayerTitle}).
 *
 * <p>Table de seuils <b>explicite</b> (XP dans le sujet) — volontairement plus raide que la
 * courbe de compte : une maîtrise s'entretient sur un domaine, elle ne se dilue pas.</p>
 *
 * <pre>
 * Fer 0 · Bronze 500 · Argent 1 500 · Or 4 000
 * Platine 9 000 · Émeraude 21 000 · Diamant 45 000
 * </pre>
 *
 * <p>Référence à ~120 XP par duel : Or ≈ 33 duels sur le sujet, Platine ≈ 75, Diamant
 * ≈ 375.</p>
 */
public enum MasteryTier {

    FER("Fer", 0),
    BRONZE("Bronze", 500),
    ARGENT("Argent", 1_500),
    OR("Or", 4_000),
    PLATINE("Platine", 9_000),
    EMERAUDE("Émeraude", 21_000),
    DIAMANT("Diamant", 45_000);

    private final String label;
    private final int xpThreshold;

    MasteryTier(String label, int xpThreshold) {
        this.label = label;
        this.xpThreshold = xpThreshold;
    }

    /** Libellé affiché du palier (« Argent »). */
    public String label() {
        return label;
    }

    /** XP cumulée dans le sujet nécessaire pour atteindre ce palier. */
    public int xpThreshold() {
        return xpThreshold;
    }

    public boolean isLast() {
        return ordinal() == values().length - 1;
    }

    /** Palier suivant, ou {@code null} pour le dernier (Diamant). */
    public MasteryTier next() {
        return isLast() ? null : values()[ordinal() + 1];
    }

    /** Progression (0–100) vers le palier suivant ; 100 pour le dernier palier. */
    public int progressPercent(int topicXp) {
        if (isLast()) {
            return 100;
        }

        int floor = xpThreshold;
        int span = next().xpThreshold - floor;
        if (span <= 0 || topicXp <= floor) {
            return 0;
        }

        return Math.min(100, (int) ((long) (topicXp - floor) * 100 / span));
    }

    /** XP restante dans le sujet avant le palier suivant ({@code 0} pour le dernier palier). */
    public int remainingXp(int topicXp) {
        if (isLast()) {
            return 0;
        }

        return Math.max(0, next().xpThreshold - Math.max(0, topicXp));
    }

    /**
     * Palier atteint pour une XP cumulée dans un sujet. Un joueur qui n'a jamais joué le sujet
     * est {@link #FER}.
     */
    public static MasteryTier tierFor(int topicXp) {
        MasteryTier reached = FER;
        for (MasteryTier tier : values()) {
            if (topicXp >= tier.xpThreshold) {
                reached = tier;
            }
        }

        return reached;
    }

    /** Palier depuis son nom (clé de titre persistée), insensible à la casse. */
    public static Optional<MasteryTier> fromKey(String key) {
        if (key == null || key.isBlank()) {
            return Optional.empty();
        }

        for (MasteryTier tier : values()) {
            if (tier.name().equalsIgnoreCase(key.trim())) {
                return Optional.of(tier);
            }
        }

        return Optional.empty();
    }
}
