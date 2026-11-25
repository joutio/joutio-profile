package com.joutio.profile.application.service;

import com.joutio.profile.domain.model.AwardedGame;
import com.joutio.profile.domain.model.Badge;
import com.joutio.profile.domain.model.BadgeUnlock;
import com.joutio.profile.domain.model.GameReward;
import com.joutio.profile.domain.model.PlayerProgress;
import com.joutio.profile.domain.model.PlayerUnlocks;
import com.joutio.profile.domain.port.out.GameRewardVersionPort;
import com.joutio.profile.domain.port.out.ProgressionAwardedGameRepositoryPort;
import com.joutio.profile.domain.port.out.ProgressionRepositoryPort;
import com.joutio.profile.domain.port.out.UnlockRepositoryPort;
import com.joutio.profile.domain.query.ProgressionQuery.GetGameRewardQuery;
import org.axonframework.queryhandling.QueryUpdateEmitter;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GameRewardServiceTest {

    private static final Instant AT = Instant.parse("2026-11-25T10:00:00Z");
    private static final AwardedGame ME = new AwardedGame("me", "game-1", "topic-1", 120, 100, false, 1212, 12, true);
    private static final AwardedGame OPPONENT =
            new AwardedGame("opponent", "game-1", "topic-1", 80, 80, false, 988, -12, true);

    private final ProgressionAwardedGameRepositoryPort awardedGames = mock(ProgressionAwardedGameRepositoryPort.class);
    private final ProgressionRepositoryPort progressions = mock(ProgressionRepositoryPort.class);
    private final UnlockRepositoryPort unlocks = mock(UnlockRepositoryPort.class);
    private final GameRewardVersionPort versions = mock(GameRewardVersionPort.class);
    private final QueryUpdateEmitter emitter = mock(QueryUpdateEmitter.class);
    private final GameRewardService service =
            new GameRewardService(awardedGames, progressions, unlocks, versions, emitter);

    GameRewardServiceTest() {
        when(progressions.findById("me")).thenReturn(Optional.of(PlayerProgress.empty("me").toBuilder()
                .xpByTopic(Map.of("topic-1", 900))
                .peakRating(240)
                .ratedGames(7)
                .build()));
        when(progressions.findById("opponent")).thenReturn(Optional.empty());
        when(unlocks.findByUserId("me")).thenReturn(new PlayerUnlocks("me", List.of(), List.of(
                new BadgeUnlock("me", Badge.DUELIST_ARGENT, AT.plusSeconds(1), "game-1"),
                new BadgeUnlock("me", Badge.DUELIST_FER, AT, "game-1"),
                new BadgeUnlock("me", Badge.DUELIST_OR, AT, "other-game"))));
        when(unlocks.findByUserId("opponent")).thenReturn(new PlayerUnlocks("opponent", List.of(), List.of()));
    }

    @Test
    void reads_the_reward_seen_by_one_player() {
        when(awardedGames.findByGame("game-1")).thenReturn(List.of(OPPONENT, ME));
        when(versions.current("game-1")).thenReturn(3L);

        GameReward reward = service.read("game-1", "me").orElseThrow();

        assertThat(reward).isEqualTo(new GameReward("game-1", "me", 3, ME, OPPONENT, 900, 240, 7,
                List.of(Badge.DUELIST_FER, Badge.DUELIST_ARGENT)));
    }

    @Test
    void a_player_not_yet_rewarded_or_not_in_the_game_reads_nothing() {
        when(awardedGames.findByGame("game-1")).thenReturn(List.of(ME));

        assertThat(service.read("game-1", "opponent")).isEmpty();
        assertThat(service.read("game-1", "stranger")).isEmpty();
    }

    @Test
    void the_opponent_is_absent_until_rewarded() {
        when(awardedGames.findByGame("game-1")).thenReturn(List.of(ME));

        assertThat(service.read("game-1", "me").orElseThrow().opponent()).isNull();
    }

    @Test
    @SuppressWarnings("unchecked")
    void a_change_bumps_the_version_and_emits_to_each_rewarded_player_only() {
        when(awardedGames.findByGame("game-1")).thenReturn(List.of(ME, OPPONENT));
        when(versions.increment("game-1")).thenReturn(4L);

        service.changed("game-1");

        ArgumentCaptor<Predicate<GetGameRewardQuery>> filters = ArgumentCaptor.forClass(Predicate.class);
        ArgumentCaptor<GameReward> rewards = ArgumentCaptor.forClass(GameReward.class);
        verify(emitter, times(2)).emit(eq(GetGameRewardQuery.class), filters.capture(), rewards.capture());
        assertThat(rewards.getAllValues()).extracting(GameReward::userId, GameReward::version)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("me", 4L),
                        org.assertj.core.groups.Tuple.tuple("opponent", 4L));

        Predicate<GetGameRewardQuery> mine = filters.getAllValues().getFirst();
        assertThat(mine.test(new GetGameRewardQuery("game-1", "me"))).isTrue();
        assertThat(mine.test(new GetGameRewardQuery("game-1", "opponent"))).isFalse();
        assertThat(mine.test(new GetGameRewardQuery("game-1", "stranger"))).isFalse();
        assertThat(mine.test(new GetGameRewardQuery("game-2", "me"))).isFalse();
    }

    @Test
    void a_change_before_any_reward_emits_nothing() {
        when(awardedGames.findByGame("game-1")).thenReturn(List.of());

        service.changed("game-1");

        verify(versions).increment("game-1");
        verify(emitter, times(0)).emit(any(Class.class), any(Predicate.class), any());
    }
}
