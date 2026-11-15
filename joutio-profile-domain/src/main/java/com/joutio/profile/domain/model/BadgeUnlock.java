package com.joutio.profile.domain.model;

import java.time.Instant;

/** Badge obtenu par un joueur, avec sa date d'obtention. */
public record BadgeUnlock(
        String userId,
        Badge badge,
        Instant earnedAt
) {
}
