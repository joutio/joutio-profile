package com.joutio.profile.application.handler.query;

import com.joutio.profile.application.service.PlayerLiveViews;
import com.joutio.profile.domain.model.PlayerRewards;
import com.joutio.profile.domain.model.PlayerRewardsUpdate;
import com.joutio.profile.domain.query.RewardQuery;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.stereotype.Component;

import java.util.List;

/** Handler Axon des récompenses persistées. */
@Component
public class RewardQueryHandler {

    private final PlayerLiveViews liveViews;

    public RewardQueryHandler(PlayerLiveViews liveViews) {
        this.liveViews = liveViews;
    }

    @QueryHandler
    public PlayerRewards handle(RewardQuery.GetPlayerRewardsQuery query) {
        return liveViews.rewards(query.userId());
    }

    /** Surveillance pour les caches du BFF : aucun résultat initial, les updates viennent de {@link PlayerLiveViews}. */
    @QueryHandler
    public List<PlayerRewardsUpdate> handle(RewardQuery.WatchPlayerRewardsQuery query) {
        return List.of();
    }
}
