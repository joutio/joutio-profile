package com.joutio.profile.application.handler.event;

import com.joutio.profile.domain.command.RewardCommand;
import com.joutio.profile.domain.event.ProgressionEvent;
import com.joutio.profile.domain.model.Badge;
import com.joutio.profile.domain.model.RewardCode;
import com.joutio.profile.domain.model.RewardRules;
import com.joutio.profile.domain.model.RewardSource;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class BadgeRewardHandlerTest {

    private final CommandGateway commandGateway = mock(CommandGateway.class);
    private final BadgeRewardHandler handler = new BadgeRewardHandler(commandGateway);

    @Test
    void assiduityBronzeGrantsMicahPermanently() {
        Instant at = Instant.parse("2026-10-23T10:00:00Z");

        handler.on(new ProgressionEvent.BadgeEarnedEvent("user-1", Badge.ASSIDUITY_BRONZE, at));

        verify(commandGateway).sendAndWait(new RewardCommand.GrantRewardCommand(
                RewardRules.rewardIdFor("user-1"),
                "user-1",
                RewardCode.MICAH_AVATAR,
                RewardSource.BADGE,
                at));
    }
}
