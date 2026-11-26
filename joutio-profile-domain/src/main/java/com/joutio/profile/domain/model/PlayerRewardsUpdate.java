package com.joutio.profile.domain.model;

/**
 * Update de {@code WatchPlayerRewardsQuery} : les récompenses d'un joueur après une écriture, {@code null} si le compte
 * est supprimé. {@code version} est monotone par joueur.
 */
public record PlayerRewardsUpdate(String userId, long version, PlayerRewards rewards) {
}
