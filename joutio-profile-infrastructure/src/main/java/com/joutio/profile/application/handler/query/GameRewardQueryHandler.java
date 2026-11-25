package com.joutio.profile.application.handler.query;

import com.joutio.profile.application.service.GameRewardService;
import com.joutio.profile.domain.model.GameReward;
import com.joutio.profile.domain.query.ProgressionQuery.GetGameRewardQuery;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.stereotype.Component;

/** Vue vivante de la récompense d'un duel (lecture initiale ; les updates viennent de {@link GameRewardService}). */
@Component
public class GameRewardQueryHandler {

    private final GameRewardService gameRewards;

    public GameRewardQueryHandler(GameRewardService gameRewards) {
        this.gameRewards = gameRewards;
    }

    @QueryHandler
    public GameReward handle(GetGameRewardQuery query) {
        return gameRewards.read(query.gameId(), query.userId()).orElse(null);
    }
}
