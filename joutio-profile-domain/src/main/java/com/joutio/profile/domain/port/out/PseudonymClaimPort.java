package com.joutio.profile.domain.port.out;

/**
 * Port sortant — réservation atomique d'un pseudonyme (clé normalisée, voir
 * {@link com.joutio.profile.domain.model.ProfileRules#pseudonymKey(String)}).
 *
 * <p>Sur le modèle de {@code EmailClaimPort} (identity) : la projection {@code profile_entry} est
 * éventuellement cohérente, l'unicité repose sur une contrainte unique en base.</p>
 */
public interface PseudonymClaimPort {

    /**
     * Réserve la clé pour ce joueur. Idempotent : une clé déjà réservée par le même joueur est
     * acceptée.
     *
     * @return {@code true} si la clé est libre ou déjà à ce joueur, {@code false} si un autre la détient.
     */
    boolean claim(String pseudonymKey, String userId);

    /** Libère la clé si elle appartient encore à ce joueur (idempotent). */
    void release(String pseudonymKey, String userId);

    /** Vrai si la clé est libre ou détenue par ce joueur (lecture, sans réservation). */
    boolean available(String pseudonymKey, String userId);
}
