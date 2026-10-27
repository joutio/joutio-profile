package com.joutio.profile.domain.event;

import com.joutio.profile.domain.model.RewardCode;
import com.joutio.profile.domain.model.RewardSource;

import java.time.Instant;

/** Événements de l'agrégat des récompenses. */
public interface RewardEvent {

    String userId();

    record RewardGrantedEvent(
            String userId,
            RewardCode rewardCode,
            RewardSource source,
            Instant unlockedAt
    ) implements RewardEvent {
    }
}
