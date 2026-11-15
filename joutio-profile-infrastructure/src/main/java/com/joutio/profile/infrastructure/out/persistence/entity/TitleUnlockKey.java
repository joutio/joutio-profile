package com.joutio.profile.infrastructure.out.persistence.entity;

import com.joutio.profile.domain.model.MasteryTier;

import java.io.Serializable;
import java.util.Objects;

/** Clé composite de {@link TitleUnlockEntity}. */
public class TitleUnlockKey implements Serializable {

    private String userId;
    private String topicId;
    private MasteryTier tier;

    public TitleUnlockKey() {
    }

    public TitleUnlockKey(String userId, String topicId, MasteryTier tier) {
        this.userId = userId;
        this.topicId = topicId;
        this.tier = tier;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof TitleUnlockKey that)) {
            return false;
        }
        return Objects.equals(userId, that.userId) && Objects.equals(topicId, that.topicId)
                && tier == that.tier;
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, topicId, tier);
    }
}
