package com.joutio.profile.domain.model;

/**
 * Résultat de la vérification d'un pseudonyme en saisie.
 *
 * @param valid     respecte {@link ProfileRules#PSEUDONYM_PATTERN}
 * @param available libre, ou déjà détenu par le joueur qui vérifie
 */
public record PseudonymAvailability(String pseudonym, boolean valid, boolean available) {
}
