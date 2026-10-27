package com.joutio.profile.domain.port.out;

import com.joutio.profile.domain.model.RewardCode;
import com.joutio.profile.domain.model.RewardGrant;
import com.joutio.profile.domain.model.RewardSource;

import java.time.Instant;
import java.util.List;

/** Persistance de la projection des récompenses. */
public interface PlayerRewardRepositoryPort {

    void grant(String userId, RewardCode code, RewardSource source, Instant unlockedAt);

    List<RewardGrant> findRewards(String userId);
}
