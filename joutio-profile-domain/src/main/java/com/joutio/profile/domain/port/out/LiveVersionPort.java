package com.joutio.profile.domain.port.out;

/**
 * Port sortant - version des vues surveillées d'un joueur (profil, progression, récompenses) : un compteur par vue et par
 * joueur, incrémenté atomiquement à chaque écriture, donc monotone même si plusieurs groupes ou répliques écrivent pour
 * le même joueur.
 */
public interface LiveVersionPort {

    /** Incrémente la version de la vue pour ce joueur et renvoie la nouvelle valeur (1 à la première écriture). */
    long increment(String view, String userId);
}
