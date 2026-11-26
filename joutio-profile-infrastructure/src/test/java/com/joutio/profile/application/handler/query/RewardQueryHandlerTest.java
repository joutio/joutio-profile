package com.joutio.profile.application.handler.query;

import org.axonframework.queryhandling.QueryUpdateEmitter;
import com.joutio.profile.domain.port.out.LiveVersionPort;
import com.joutio.profile.domain.port.out.UnlockRepositoryPort;
import com.joutio.profile.domain.port.out.ProgressionRepositoryPort;
import com.joutio.profile.domain.port.out.ProfileRepositoryPort;
import com.joutio.profile.application.service.PlayerLiveViews;
import com.joutio.profile.domain.model.PlayerRewards;
import com.joutio.profile.domain.model.RewardCode;
import com.joutio.profile.domain.model.RewardGrant;
import com.joutio.profile.domain.model.RewardSource;
import com.joutio.profile.domain.port.out.PlayerRewardRepositoryPort;
import com.joutio.profile.domain.query.RewardQuery;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RewardQueryHandlerTest {

    private final PlayerRewardRepositoryPort repository = mock(PlayerRewardRepositoryPort.class);
    private final RewardQueryHandler handler = new RewardQueryHandler(new PlayerLiveViews(
            mock(ProfileRepositoryPort.class), mock(ProgressionRepositoryPort.class), mock(UnlockRepositoryPort.class),
            repository, mock(LiveVersionPort.class), mock(QueryUpdateEmitter.class)));

    @Test
    void returnsPersistedRewards() {
        RewardGrant grant = new RewardGrant(
                RewardCode.MICAH_AVATAR, RewardSource.BADGE, Instant.parse("2026-10-23T10:00:00Z"));
        when(repository.findRewards("user-1")).thenReturn(List.of(grant));

        PlayerRewards rewards = handler.handle(new RewardQuery.GetPlayerRewardsQuery("user-1"));

        assertThat(rewards.userId()).isEqualTo("user-1");
        assertThat(rewards.rewards()).containsExactly(grant);
        assertThat(rewards.has(RewardCode.MICAH_AVATAR)).isTrue();
    }
}
