package com.joutio.profile.domain.model;

/**
 * Règles Elo globales du profil (pures, sans dépendance framework).
 *
 * <p>L'Elo mesure la compétence en duel, tous sujets confondus. L'XP de sujet reste
 * indépendante : elle porte la maîtrise, les titres et les classements par thème.</p>
 *
 * <p>Départ à {@value #INITIAL_RATING} (échelle type chess.com), facteur K **fixe** dès le premier
 * duel classé (2026-11-16 : plus de période provisoire « non classé », le joueur a un Elo et un
 * palier dès son premier duel). Les duels contre bot ne sont jamais classés.</p>
 */
public final class EloRules {

    public static final int INITIAL_RATING = 200;
    public static final int K_FACTOR = 20;

    /** Nombre maximum de duels comptés pour l'Elo contre le même adversaire, par jour. */
    public static final int DAILY_OPPONENT_CAP = 2;

    private EloRules() {
    }

    /** Score attendu du joueur (0–1) selon l'écart de rating. */
    public static double expectedScore(int rating, int opponentRating) {
        return 1.0 / (1.0 + Math.pow(10.0, (opponentRating - rating) / 400.0));
    }

    /**
     * Nouveau rating après un duel.
     *
     * @param rating         rating courant du joueur
     * @param opponentRating rating courant de l'adversaire
     * @param score          1 victoire, 0.5 nul, 0 défaite
     * @return nouveau rating, borné à 0
     */
    public static int newRating(int rating, int opponentRating, double score) {
        double expected = expectedScore(rating, opponentRating);
        int next = (int) Math.round(rating + K_FACTOR * (score - expected));
        return Math.max(0, next);
    }
}
