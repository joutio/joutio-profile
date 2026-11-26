package com.joutio.profile.domain.query;

/** Requêtes de la projection des récompenses. */
public interface RewardQuery {

    record GetPlayerRewardsQuery(String userId) implements RewardQuery {
    }

    /**
     * Surveillance des récompenses de tous les joueurs, pour les caches du BFF : aucun résultat initial (liste vide),
     * puis un {@code PlayerRewardsUpdate} à chaque récompense accordée.
     */
    record WatchPlayerRewardsQuery() implements RewardQuery {
    }
}
