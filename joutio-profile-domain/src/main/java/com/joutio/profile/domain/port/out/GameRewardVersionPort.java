package com.joutio.profile.domain.port.out;

/**
 * Port sortant - version de la récompense d'une partie : un compteur par partie, incrémenté atomiquement à chaque
 * écriture, donc monotone même si plusieurs répliques écrivent la même partie.
 */
public interface GameRewardVersionPort {

    /** Incrémente la version de la partie et renvoie la nouvelle valeur (1 à la première écriture). */
    long increment(String gameId);

    /** Version courante, 0 si la partie n'a encore aucune écriture. */
    long current(String gameId);
}
