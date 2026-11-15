package com.joutio.profile.domain.model;

import java.util.List;

/**
 * Déblocages datés d'un joueur : titres thématiques (plus récents d'abord) et badges.
 */
public record PlayerUnlocks(
        String userId,
        List<TitleUnlock> titles,
        List<BadgeUnlock> badges
) {

    public PlayerUnlocks {
        titles = titles == null ? List.of() : List.copyOf(titles);
        badges = badges == null ? List.of() : List.copyOf(badges);
    }

    public static PlayerUnlocks empty(String userId) {
        return new PlayerUnlocks(userId, List.of(), List.of());
    }
}
