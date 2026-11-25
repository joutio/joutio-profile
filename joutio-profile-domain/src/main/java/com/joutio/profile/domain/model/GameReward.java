package com.joutio.profile.domain.model;

import java.util.List;

/**
 * Récompense d'un duel vue par l'un de ses joueurs : vue vivante {@code GetGameRewardQuery}, émise à chaque écriture
 * qui la change (XP de l'un ou l'autre joueur, badge débloqué par la partie).
 *
 * @param version    nombre d'écritures de la récompense de cette partie, monotone
 * @param player     attribution du lecteur
 * @param opponent   attribution de l'adversaire humain, {@code null} pour un entraînement ou tant qu'elle manque
 * @param topicXp    XP du lecteur dans le sujet de la partie
 * @param peakRating meilleur Elo du lecteur
 * @param ratedGames duels classés cumulés du lecteur
 * @param badges     badges débloqués par la partie, dans l'ordre d'obtention
 */
public record GameReward(String gameId, String userId, long version, AwardedGame player, AwardedGame opponent,
                         int topicXp, int peakRating, int ratedGames, List<Badge> badges) {
}
