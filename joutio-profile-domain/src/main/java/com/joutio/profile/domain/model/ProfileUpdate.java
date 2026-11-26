package com.joutio.profile.domain.model;

/**
 * Update de {@code WatchProfilesQuery} : le profil d'un joueur après une écriture, {@code null} si le compte est
 * supprimé. {@code version} est monotone par joueur.
 */
public record ProfileUpdate(String userId, long version, Profile profile) {
}
