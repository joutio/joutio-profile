package com.joutio.profile.domain.aggregate;

import com.joutio.axon.test.JoutioAxonMatchers;
import com.joutio.profile.domain.command.ProgressionCommand;
import com.joutio.profile.domain.event.ProgressionEvent;
import com.joutio.profile.domain.model.MasteryTier;
import com.joutio.profile.domain.model.ProgressionRules;
import org.axonframework.test.aggregate.AggregateTestFixture;
import org.junit.jupiter.api.Test;

import java.time.Instant;

/**
 * Test Axon in-memory de {@link PlayerProgressAggregate} : attribution XP par thème,
 * idempotence, maîtrise et Elo. Les **badges** sont évalués par {@link BadgeAggregate}
 * (cf. {@code BadgeAggregateTest}).
 */
class PlayerProgressAggregateTest {

    private final AggregateTestFixture<PlayerProgressAggregate> fixture =
            new AggregateTestFixture<>(PlayerProgressAggregate.class);

    @Test
    void firstAward_createsAggregateAndAppliesXpAwardedEvent() {
        fixture
                .givenNoPriorActivity()
                .when(new ProgressionCommand.AwardXpCommand(ProgressionRules.progressIdFor("user-1"), "user-1", "game-1", "topic-1", 100, false, false, false, 4, 2, 0, null, null, null))
                .expectEventsMatching(JoutioAxonMatchers.hasPayloadMatching(
                        ProgressionEvent.XpAwardedEvent.class,
                        e -> {
                            ProgressionEvent.XpAwardedEvent xp = (ProgressionEvent.XpAwardedEvent) e;
                            return "user-1".equals(xp.userId())
                                    && "game-1".equals(xp.gameId())
                                    && "topic-1".equals(xp.topicId())
                                    && xp.xp() == 100;
                        }));
    }

    @Test
    void botGame_isTrainingAndAwardsNoXpEvenOnWin() {
        fixture
                .givenNoPriorActivity()
                .when(new ProgressionCommand.AwardXpCommand(ProgressionRules.progressIdFor("user-1"), "user-1", "game-1", "topic-1", 160, true, false, true, 4, 2, 0, null, null, null))
                .expectEventsMatching(JoutioAxonMatchers.hasPayloadMatching(
                        ProgressionEvent.XpAwardedEvent.class,
                        e -> {
                            ProgressionEvent.XpAwardedEvent xp = (ProgressionEvent.XpAwardedEvent) e;
                            return xp.botGame() && xp.xp() == 0;
                        }));
    }

    @Test
    void crossingAMasteryTier_announcesTheThematicTitle() {
        // Seuil de Or - 100 XP, puis +160 XP : le palier thématique est franchi.
        fixture
                .given(new ProgressionEvent.XpAwardedEvent(
                        "user-1", "game-0", "topic-1",
                        MasteryTier.OR.xpThreshold() - 100, 160, true, false, false, 4, 2, 0,
                        Instant.now(), 200, 200, 0, true, 0, null, null))
                .when(new ProgressionCommand.AwardXpCommand(
                        ProgressionRules.progressIdFor("user-1"), "user-1", "game-1", "topic-1",
                        160, true, false, false, 4, 2, 0, null, null, null))
                .expectEventsMatching(JoutioAxonMatchers.hasPayloadMatching(
                        ProgressionEvent.MasteryTierReachedEvent.class,
                        e -> {
                            ProgressionEvent.MasteryTierReachedEvent reached =
                                    (ProgressionEvent.MasteryTierReachedEvent) e;
                            return MasteryTier.OR == reached.tier()
                                    && "topic-1".equals(reached.topicId());
                        }));
    }

    @Test
    void humanWin_updatesEloAndKeepsProvisionalOnFirstRatedGame() {
        fixture
                .givenNoPriorActivity()
                .when(new ProgressionCommand.AwardXpCommand(
                        ProgressionRules.progressIdFor("user-1"), "user-1", "game-1", "topic-1",
                        100, true, false, false, 4, 2, 0,
                        "user-2", 200, java.time.LocalDate.of(2026, 10, 21)))
                .expectEventsMatching(JoutioAxonMatchers.hasPayloadMatching(
                        ProgressionEvent.XpAwardedEvent.class,
                        e -> {
                            ProgressionEvent.XpAwardedEvent xp = (ProgressionEvent.XpAwardedEvent) e;
                            return xp.ratingDelta() == 20
                                    && xp.rating() == 220
                                    && xp.peakRating() == 220
                                    && xp.ratedGames() == 1
                                    && xp.provisional();
                        }));
    }

    @Test
    void sameGameId_isIdempotent() {
        fixture
                .given(new ProgressionEvent.XpAwardedEvent(
                        "user-1", "game-1", "topic-1", 100, 100, false, false, false, 4, 2, 0, Instant.now(), 200, 200, 0, true, 0, null, null))
                .when(new ProgressionCommand.AwardXpCommand(ProgressionRules.progressIdFor("user-1"), "user-1", "game-1", "topic-1", 100, true, false, false, 4, 2, 0, null, null, null))
                .expectNoEvents();
    }
}
