package com.joutio.profile.application.saga;

import com.joutio.matchmaking.domain.event.RoomEvent;
import com.joutio.matchmaking.domain.model.RoomOrigin;
import com.joutio.profile.domain.command.ProgressionCommand;
import com.joutio.profile.domain.model.BadgeMetric;
import com.joutio.profile.domain.model.ProgressionRules;

import org.axonframework.test.saga.SagaTestFixture;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

class ChallengeBadgesSagaTest {

    private static final Instant AT = Instant.parse("2026-11-24T10:00:00Z");

    private final SagaTestFixture<ChallengeBadgesSaga> fixture = new SagaTestFixture<>(ChallengeBadgesSaga.class);

    @Test
    void completedChallenge_creditsItsInitiatorOncePerRoom() {
        fixture.givenNoPriorActivity()
                .whenPublishingA(completed(RoomOrigin.CHALLENGE))
                .expectDispatchedCommands(new ProgressionCommand.RecordBadgeCounterCommand(
                        ProgressionRules.badgeIdFor("alice"), "alice", BadgeMetric.CHALLENGES_SENT, "room-1", AT))
                .expectActiveSagas(0);
    }

    @Test
    void completedRematch_countsAsAChallenge() {
        fixture.givenNoPriorActivity()
                .whenPublishingA(completed(RoomOrigin.REMATCH))
                .expectDispatchedCommands(new ProgressionCommand.RecordBadgeCounterCommand(
                        ProgressionRules.badgeIdFor("alice"), "alice", BadgeMetric.CHALLENGES_SENT, "room-1", AT));
    }

    @Test
    void otherRooms_creditNothing() {
        fixture.givenNoPriorActivity()
                .whenPublishingA(completed(RoomOrigin.SHARED))
                .expectNoDispatchedCommands()
                .expectActiveSagas(0);
    }

    private static RoomEvent.RoomCompletedEvent completed(RoomOrigin origin) {
        return new RoomEvent.RoomCompletedEvent("room-1", "game-1", origin, "alice", Set.of("alice", "bob"), AT);
    }
}
