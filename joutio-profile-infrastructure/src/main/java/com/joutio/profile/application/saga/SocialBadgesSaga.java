package com.joutio.profile.application.saga;

import com.joutio.microservice.core.domain.constant.JoutioConstants;
import com.joutio.profile.domain.command.ProgressionCommand;
import com.joutio.profile.domain.model.BadgeMetric;
import com.joutio.profile.domain.model.ProgressionRules;
import com.joutio.social.domain.event.UserFollowerEvent;
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
 * SocialBadgesSaga — crédite le badge **Social** du joueur qui gagne un abonné.
 *
 * <p>Consomme {@link UserFollowerEvent.UserFollowedEvent} (émis par {@code joutio-social}) et
 * incrémente le compteur {@code FOLLOWERS_GAINED} de l'agrégat de badges. Les désabonnements
 * n'enlèvent rien (un badge est définitif). Saga **sans état**, idempotente par agrégat.</p>
 */
@Saga
@ProcessingGroup("social-badges-saga")
public class SocialBadgesSaga {

    private static final Logger logger = LoggerFactory.getLogger(SocialBadgesSaga.class);

    @Autowired
    private transient CommandGateway commandGateway;

    @StartSaga
    @SagaEventHandler(associationProperty = "followedId")
    public void on(UserFollowerEvent.UserFollowedEvent event) {
        if (event.followedId() == null
                || Objects.equals(JoutioConstants.SYSTEM_USER_ID, event.followedId())) {
            SagaLifecycle.end();
            return;
        }

        commandGateway.send(new ProgressionCommand.RecordBadgeCounterCommand(
                ProgressionRules.badgeIdFor(event.followedId()),
                event.followedId(),
                BadgeMetric.FOLLOWERS_GAINED,
                event.followId(),
                event.followedAt()
        ));
        logger.debug("Badge social crédité: userId={}", event.followedId());
        SagaLifecycle.end();
    }
}
