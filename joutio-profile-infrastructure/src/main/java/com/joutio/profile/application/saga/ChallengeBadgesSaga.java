package com.joutio.profile.application.saga;

import com.joutio.matchmaking.domain.event.ChallengeEvent;
import com.joutio.microservice.core.domain.constant.JoutioConstants;
import com.joutio.profile.domain.command.ProgressionCommand;
import com.joutio.profile.domain.model.BadgeMetric;
import com.joutio.profile.domain.model.ProgressionRules;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.modelling.saga.SagaEventHandler;
import org.axonframework.modelling.saga.SagaLifecycle;
import org.axonframework.modelling.saga.StartSaga;
import org.axonframework.spring.stereotype.Saga;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Objects;

/**
 * ChallengeBadgesSaga — crédite le badge **Défis** du joueur qui lance un défi nominatif.
 *
 * <p>Consomme {@link ChallengeEvent.ChallengeStartedEvent} (émis par {@code joutio-matchmaking},
 * propriétaire des défis) : un défi ne compte **que s'il a été accepté et a abouti sur une partie**
 * — la simple création (ou acceptation) du défi ne crédite rien. Saga **sans état** : elle se
 * termine aussitôt.</p>
 */
@Saga
@ProcessingGroup("challenge-badges-saga")
public class ChallengeBadgesSaga {

    private static final Logger logger = LoggerFactory.getLogger(ChallengeBadgesSaga.class);

    @Autowired
    private transient CommandGateway commandGateway;

    @StartSaga
    @SagaEventHandler(associationProperty = "challengerId")
    public void on(ChallengeEvent.ChallengeStartedEvent event) {
        if (event.challengerId() == null
                || Objects.equals(JoutioConstants.SYSTEM_USER_ID, event.challengerId())) {
            SagaLifecycle.end();
            return;
        }

        commandGateway.send(new ProgressionCommand.RecordBadgeCounterCommand(
                ProgressionRules.badgeIdFor(event.challengerId()),
                event.challengerId(),
                BadgeMetric.CHALLENGES_SENT,
                event.challengeId(),
                event.startedAt()
        ));
        logger.debug("Badge défi crédité (défi abouti): userId={}, gameId={}",
                event.challengerId(), event.gameId());
        SagaLifecycle.end();
    }
}
