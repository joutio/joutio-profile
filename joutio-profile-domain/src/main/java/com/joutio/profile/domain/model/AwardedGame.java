package com.joutio.profile.domain.model;

/**
 * Attribution d'une partie à un joueur, telle que la projection de progression l'a appliquée (journal d'idempotence,
 * source de la récompense du duel).
 *
 * @param xp          XP du sujet attribuée (0 pour un entraînement bot)
 * @param gameScore   score du joueur dans la partie
 * @param ratingDelta variation d'Elo (0 si non classé)
 * @param rated       le duel a fait évoluer l'Elo
 */
public record AwardedGame(String userId, String gameId, String topicId, int xp, int gameScore, boolean botGame,
                          int ratingDelta, boolean rated) {
}
