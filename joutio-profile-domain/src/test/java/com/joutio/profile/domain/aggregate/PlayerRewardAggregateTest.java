package com.joutio.profile.domain.aggregate;

import com.joutio.axon.test.JoutioAxonMatchers;
import com.joutio.profile.domain.command.RewardCommand;
import com.joutio.profile.domain.event.RewardEvent;
import com.joutio.profile.domain.model.RewardCode;
import com.joutio.profile.domain.model.RewardRules;
import com.joutio.profile.domain.model.RewardSource;
import org.axonframework.test.aggregate.AggregateTestFixture;
import org.junit.jupiter.api.Test;

import java.time.Instant;

class PlayerRewardAggregateTest {

    private final AggregateTestFixture<PlayerRewardAggregate> fixture =
            new AggregateTestFixture<>(PlayerRewardAggregate.class);

    @Test
    void firstGrant_emitsRewardGranted() {
        Instant at = Instant.parse("2026-10-23T10:00:00Z");
        fixture.givenNoPriorActivity()
                .when(new RewardCommand.GrantRewardCommand(
                        RewardRules.rewardIdFor("user-1"), "user-1",
                        RewardCode.MICAH_AVATAR, RewardSource.BADGE, at))
                .expectEventsMatching(JoutioAxonMatchers.hasPayloadMatching(
                        RewardEvent.RewardGrantedEvent.class,
                        e -> ((RewardEvent.RewardGrantedEvent) e).rewardCode() == RewardCode.MICAH_AVATAR
                                && "user-1".equals(((RewardEvent.RewardGrantedEvent) e).userId())));
    }

    @Test
    void duplicateGrant_isIdempotent() {
        Instant at = Instant.parse("2026-10-23T10:00:00Z");
        fixture.given(new RewardEvent.RewardGrantedEvent(
                        "user-1", RewardCode.MICAH_AVATAR, RewardSource.BADGE, at))
                .when(new RewardCommand.GrantRewardCommand(
                        RewardRules.rewardIdFor("user-1"), "user-1",
                        RewardCode.MICAH_AVATAR, RewardSource.BADGE, at))
                .expectNoEvents();
    }
}
