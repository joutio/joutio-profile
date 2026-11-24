package com.joutio.profile.application.saga;

import com.joutio.matchmaking.domain.event.RoomEvent;
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
 * ChallengeBadgesSaga — crédite le badge **Défis** du joueur qui lance un défi ou une revanche.
 *
 * <p>Consomme {@link RoomEvent.RoomCompletedEvent} (émis par {@code joutio-matchmaking}) : un défi ne compte
 * <b>que s'il a abouti sur une partie</b>, jamais à l'envoi ni à l'acceptation. Saga <b>sans état</b> : elle se termine
 * aussitôt ; la salle sert de source idempotente du compteur.</p>
 */
@Saga
@ProcessingGroup("challenge-badges-saga")
public class ChallengeBadgesSaga {

    private static final Logger logger = LoggerFactory.getLogger(ChallengeBadgesSaga.class);

    @Autowired
    private transient CommandGateway commandGateway;

    @StartSaga
    @SagaEventHandler(associationProperty = "roomId")
    public void on(RoomEvent.RoomCompletedEvent event) {
        if (event.origin() == null
                || !event.origin().requiresAcceptance()
                || event.initiatorId() == null
                || Objects.equals(JoutioConstants.SYSTEM_USER_ID, event.initiatorId())) {
            SagaLifecycle.end();
            return;
        }

        commandGateway.send(new ProgressionCommand.RecordBadgeCounterCommand(
                ProgressionRules.badgeIdFor(event.initiatorId()),
                event.initiatorId(),
                BadgeMetric.CHALLENGES_SENT,
                event.roomId(),
                event.completedAt()
        ));
        logger.debug("Badge défi crédité (défi abouti): userId={}, gameId={}", event.initiatorId(), event.gameId());
        SagaLifecycle.end();
    }
}
