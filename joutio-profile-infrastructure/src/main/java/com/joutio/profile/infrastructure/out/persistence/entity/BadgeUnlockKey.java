package com.joutio.profile.infrastructure.out.persistence.entity;

import com.joutio.profile.domain.model.Badge;

import java.io.Serializable;
import java.util.Objects;

/** Clé composite de {@link BadgeUnlockEntity}. */
public class BadgeUnlockKey implements Serializable {

    private String userId;
    private Badge badge;

    public BadgeUnlockKey() {
    }

    public BadgeUnlockKey(String userId, Badge badge) {
        this.userId = userId;
        this.badge = badge;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof BadgeUnlockKey that)) {
            return false;
        }
        return Objects.equals(userId, that.userId) && badge == that.badge;
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, badge);
    }
}
