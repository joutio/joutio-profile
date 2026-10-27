package com.joutio.profile.application.handler.query;

import com.joutio.profile.domain.model.PlayerRewards;
import com.joutio.profile.domain.port.out.PlayerRewardRepositoryPort;
import com.joutio.profile.domain.query.RewardQuery;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.stereotype.Component;

/** Handler Axon des récompenses persistées. */
@Component
public class RewardQueryHandler {

    private final PlayerRewardRepositoryPort repositoryPort;

    public RewardQueryHandler(PlayerRewardRepositoryPort repositoryPort) {
        this.repositoryPort = repositoryPort;
    }

    @QueryHandler
    public PlayerRewards handle(RewardQuery.GetPlayerRewardsQuery query) {
        return new PlayerRewards(query.userId(), repositoryPort.findRewards(query.userId()));
    }
}
