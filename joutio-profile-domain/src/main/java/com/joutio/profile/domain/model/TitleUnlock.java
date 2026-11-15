package com.joutio.profile.domain.model;

import java.time.Instant;

/**
 * Titre thématique débloqué par un joueur : palier de maîtrise atteint dans un sujet, daté.
 *
 * <p>Le Fer est obtenu à la première partie humaine terminée dans le sujet (abandon compris) ;
 * les paliers suivants à leur seuil d'XP ({@link MasteryTier}).</p>
 */
public record TitleUnlock(
        String userId,
        String topicId,
        MasteryTier tier,
        Instant unlockedAt
) {
}
