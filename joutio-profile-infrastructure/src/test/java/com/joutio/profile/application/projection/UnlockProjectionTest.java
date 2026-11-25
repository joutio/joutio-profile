package com.joutio.profile.application.projection;

import com.joutio.profile.application.service.GameRewardService;
import com.joutio.profile.domain.event.ProgressionEvent;
import com.joutio.profile.domain.model.Badge;
import com.joutio.profile.domain.model.BadgeUnlock;
import com.joutio.profile.domain.model.MasteryTier;
import com.joutio.profile.domain.model.TitleUnlock;
import com.joutio.profile.domain.port.out.UnlockRepositoryPort;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class UnlockProjectionTest {

    private static final Instant AT = Instant.parse("2026-11-15T10:00:00Z");

    private final UnlockRepositoryPort repository = mock(UnlockRepositoryPort.class);
    private final GameRewardService gameRewards = mock(GameRewardService.class);
    private final UnlockProjection projection = new UnlockProjection(repository, gameRewards);

    @Test
    void humanGame_unlocksIronTitleOfTheTopic_evenWithZeroXp() {
        projection.on(xpAwarded(0, false));

        verify(repository).saveTitle(new TitleUnlock("user-1", "topic-1", MasteryTier.FER, AT));
    }

    @Test
    void botGame_unlocksNothing() {
        projection.on(xpAwarded(0, true));

        verify(repository, never()).saveTitle(any());
    }

    @Test
    void tierReached_isProjectedWithItsDate() {
        projection.on(new ProgressionEvent.MasteryTierReachedEvent(
                "user-1", "topic-1", MasteryTier.ARGENT, AT));

        verify(repository).saveTitle(new TitleUnlock("user-1", "topic-1", MasteryTier.ARGENT, AT));
    }

    @Test
    void badgeEarned_isProjectedWithItsDate() {
        projection.on(new ProgressionEvent.BadgeEarnedEvent("user-1", Badge.DUELIST_FER, AT, "game-1"));

        verify(repository).saveBadge(new BadgeUnlock("user-1", Badge.DUELIST_FER, AT, "game-1"));
        verify(gameRewards).changed("game-1");
    }

    @Test
    void badgeEarnedOutsideAGame_changesNoGameReward() {
        projection.on(new ProgressionEvent.BadgeEarnedEvent("user-1", Badge.DUELIST_FER, AT, null));

        verify(gameRewards, never()).changed(any());
    }

    @Test
    void reset_clearsTheReadModel() {
        projection.reset();

        verify(repository).deleteAll();
    }

    private static ProgressionEvent.XpAwardedEvent xpAwarded(int xp, boolean botGame) {
        return new ProgressionEvent.XpAwardedEvent(
                "user-1", "game-1", "topic-1", xp, 30, false, false, botGame, 0, 0, 0, AT,
                200, 200, 0, 0, null, null);
    }
}
