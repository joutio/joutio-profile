package com.joutio.profile.domain.command;

import com.joutio.profile.domain.model.RewardCode;
import com.joutio.profile.domain.model.RewardSource;

import java.time.Instant;

/** Commandes de l'agrégat des récompenses. */
public interface RewardCommand {

    record GrantRewardCommand(
            String rewardId,
            String userId,
            RewardCode rewardCode,
            RewardSource source,
            Instant unlockedAt
    ) implements RewardCommand {
    }
}
