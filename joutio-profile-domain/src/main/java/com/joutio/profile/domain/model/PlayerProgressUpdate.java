package com.joutio.profile.domain.model;

/**
 * Update de {@code WatchProgressionsQuery} : la progression d'un joueur (badges compris) après une écriture,
 * {@code null} si le compte est supprimé. {@code version} est monotone par joueur.
 */
public record PlayerProgressUpdate(String userId, long version, PlayerProgress progress) {
}
