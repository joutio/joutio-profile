package com.joutio.profile.application.handler.query;

import org.axonframework.queryhandling.QueryUpdateEmitter;
import com.joutio.profile.domain.port.out.LiveVersionPort;
import com.joutio.profile.domain.port.out.PlayerRewardRepositoryPort;
import com.joutio.profile.domain.port.out.ProfileRepositoryPort;
import com.joutio.profile.application.service.PlayerLiveViews;
import com.joutio.profile.domain.model.Badge;
import com.joutio.profile.domain.model.GameXp;
import com.joutio.profile.domain.model.MasteryTier;
import com.joutio.profile.domain.model.PlayerProgress;
import com.joutio.profile.domain.model.PlayerUnlocks;
import com.joutio.profile.domain.model.TopicProgress;
import com.joutio.profile.domain.port.out.ProgressionAwardedGameRepositoryPort;
import com.joutio.profile.domain.port.out.ProgressionRepositoryPort;
import com.joutio.profile.domain.port.out.UnlockRepositoryPort;
import com.joutio.profile.domain.query.ProgressionQuery;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProgressionQueryHandlerTest {

    private final ProgressionRepositoryPort progressionRepository = mock(ProgressionRepositoryPort.class);
    private final ProgressionAwardedGameRepositoryPort awardedGameRepository =
            mock(ProgressionAwardedGameRepositoryPort.class);
    private final UnlockRepositoryPort unlockRepository = mock(UnlockRepositoryPort.class);
    private final ProgressionQueryHandler handler =
            new ProgressionQueryHandler(progressionRepository, awardedGameRepository, unlockRepository,
                    new PlayerLiveViews(mock(ProfileRepositoryPort.class), progressionRepository, unlockRepository,
                            mock(PlayerRewardRepositoryPort.class), mock(LiveVersionPort.class),
                            mock(QueryUpdateEmitter.class)));

    @Test
    void games_xp_delegates_to_awarded_journal() {
        when(awardedGameRepository.findGameXp("me", List.of("game-1", "game-2")))
                .thenReturn(List.of(new GameXp("game-1", 120, 18, true)));

        List<GameXp> result = handler.handle(
                new ProgressionQuery.GetGamesXpQuery("me", List.of("game-1", "game-2")));

        assertThat(result).containsExactly(new GameXp("game-1", 120, 18, true));
        verify(awardedGameRepository).findGameXp("me", List.of("game-1", "game-2"));
    }

    @Test
    void progressions_by_ids_returns_empty_progression_for_unknown_users() {
        when(progressionRepository.findByIds(List.of("known", "unknown")))
                .thenReturn(List.of(PlayerProgress.empty("known")));

        List<PlayerProgress> result = handler.handle(
                new ProgressionQuery.GetProgressionsByIdsQuery(List.of("known", "unknown")));

        assertThat(result).hasSize(2);
        assertThat(result.get(0).userId()).isEqualTo("known");
        assertThat(result.get(1).userId()).isEqualTo("unknown");
        assertThat(result.get(1).xpByTopic()).isEmpty();
    }

    @Test
    void topic_progressions_are_sorted_by_games_then_xp() {
        when(progressionRepository.findById("me")).thenReturn(Optional.of(
                PlayerProgress.empty("me").toBuilder()
                        .xpByTopic(Map.of("dota", 500, "space", 200, "frozen", 300))
                        .gamesByTopic(Map.of("dota", 3, "space", 7, "frozen", 3))
                        .build()));

        List<TopicProgress> result = handler.handle(
                new ProgressionQuery.GetTopicProgressionsQuery("me"));

        assertThat(result).hasSize(3);
        assertThat(result).extracting(TopicProgress::topicId)
                .containsExactly("space", "dota", "frozen");
        assertThat(result.get(0).games()).isEqualTo(7);
        assertThat(result.get(0).xp()).isEqualTo(200);
        assertThat(result.get(0).tier()).isEqualTo(MasteryTier.FER);
        assertThat(result.get(1).games()).isEqualTo(3);
    }

    @Test
    void topic_progressions_keep_bot_only_topics_with_zero_games() {
        when(progressionRepository.findById("me")).thenReturn(Optional.of(
                PlayerProgress.empty("me").toBuilder()
                        .xpByTopic(Map.of("bot-topic", 120))
                        .gamesByTopic(Map.of())
                        .build()));

        List<TopicProgress> result = handler.handle(
                new ProgressionQuery.GetTopicProgressionsQuery("me"));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).topicId()).isEqualTo("bot-topic");
        assertThat(result.get(0).xp()).isEqualTo(120);
        assertThat(result.get(0).games()).isZero();
    }

    @Test
    void topic_progressions_return_empty_for_unknown_user() {
        when(progressionRepository.findById("unknown")).thenReturn(Optional.empty());

        List<TopicProgress> result = handler.handle(
                new ProgressionQuery.GetTopicProgressionsQuery("unknown"));

        assertThat(result).isEmpty();
    }

    @Test
    void progression_badges_come_from_unlocks_even_without_progression_row() {
        // Un badge hors duel (abonné gagné) peut précéder toute ligne de progression.
        when(progressionRepository.findByIds(List.of("me"))).thenReturn(List.of());
        when(unlockRepository.findBadgesByUserIds(List.of("me")))
                .thenReturn(Map.of("me", Set.of(Badge.SOCIAL_FER)));

        PlayerProgress result = handler.handle(new ProgressionQuery.GetProgressionQuery("me"));

        assertThat(result.userId()).isEqualTo("me");
        assertThat(result.badges()).containsExactly(Badge.SOCIAL_FER);
    }

    @Test
    void progressions_by_ids_attach_each_player_badges() {
        when(progressionRepository.findByIds(List.of("a", "b")))
                .thenReturn(List.of(PlayerProgress.empty("a"), PlayerProgress.empty("b")));
        when(unlockRepository.findBadgesByUserIds(List.of("a", "b")))
                .thenReturn(Map.of("a", Set.of(Badge.DUELIST_FER)));

        List<PlayerProgress> result = handler.handle(
                new ProgressionQuery.GetProgressionsByIdsQuery(List.of("a", "b")));

        assertThat(result.get(0).badges()).containsExactly(Badge.DUELIST_FER);
        assertThat(result.get(1).badges()).isEmpty();
    }

    @Test
    void unlocks_delegate_to_the_unlock_read_model() {
        PlayerUnlocks unlocks = PlayerUnlocks.empty("me");
        when(unlockRepository.findByUserId("me")).thenReturn(unlocks);

        assertThat(handler.handle(new ProgressionQuery.GetUnlocksQuery("me"))).isSameAs(unlocks);
    }
}
