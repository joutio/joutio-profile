package com.joutio.profile.application.service;

import com.joutio.profile.domain.model.Badge;
import com.joutio.profile.domain.model.PlayerProgress;
import com.joutio.profile.domain.model.PlayerProgressUpdate;
import com.joutio.profile.domain.model.PlayerRewardsUpdate;
import com.joutio.profile.domain.model.Profile;
import com.joutio.profile.domain.model.ProfileUpdate;
import com.joutio.profile.domain.port.out.LiveVersionPort;
import com.joutio.profile.domain.port.out.PlayerRewardRepositoryPort;
import com.joutio.profile.domain.port.out.ProfileRepositoryPort;
import com.joutio.profile.domain.port.out.ProgressionRepositoryPort;
import com.joutio.profile.domain.port.out.UnlockRepositoryPort;
import com.joutio.profile.domain.query.ProfileQuery.WatchProfilesQuery;
import com.joutio.profile.domain.query.ProgressionQuery.WatchProgressionsQuery;
import com.joutio.profile.domain.query.RewardQuery.WatchPlayerRewardsQuery;
import org.axonframework.queryhandling.QueryUpdateEmitter;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlayerLiveViewsTest {

    private final ProfileRepositoryPort profiles = mock(ProfileRepositoryPort.class);
    private final ProgressionRepositoryPort progressions = mock(ProgressionRepositoryPort.class);
    private final UnlockRepositoryPort unlocks = mock(UnlockRepositoryPort.class);
    private final PlayerRewardRepositoryPort rewards = mock(PlayerRewardRepositoryPort.class);
    private final LiveVersionPort versions = mock(LiveVersionPort.class);
    private final QueryUpdateEmitter emitter = mock(QueryUpdateEmitter.class);
    private final PlayerLiveViews liveViews =
            new PlayerLiveViews(profiles, progressions, unlocks, rewards, versions, emitter);

    @Test
    void profileChangeIsEmittedWithTheNextVersion() {
        Profile profile = Profile.builder().userId("u1").pseudonym("Ada").build();
        when(versions.increment("profile", "u1")).thenReturn(4L);
        when(profiles.findById("u1")).thenReturn(Optional.of(profile));

        liveViews.profileChanged("u1");

        verify(emitter).emit(eq(WatchProfilesQuery.class), any(), eq(new ProfileUpdate("u1", 4, profile)));
    }

    @Test
    void progressionChangeCarriesTheBadges() {
        when(versions.increment("progression", "u1")).thenReturn(2L);
        when(progressions.findByIds(anyList())).thenReturn(List.of(PlayerProgress.empty("u1")));
        when(unlocks.findBadgesByUserIds(anyCollection())).thenReturn(Map.of("u1", Set.of(Badge.values()[0])));

        liveViews.progressionChanged("u1");

        ArgumentCaptor<PlayerProgressUpdate> update = ArgumentCaptor.forClass(PlayerProgressUpdate.class);
        verify(emitter).emit(eq(WatchProgressionsQuery.class), any(), update.capture());
        assertThat(update.getValue().version()).isEqualTo(2);
        assertThat(update.getValue().progress().badges()).containsExactly(Badge.values()[0]);
    }

    @Test
    void deletionEmitsEmptyViewsThatWinOverAnyVersion() {
        liveViews.deleted("u1");

        verify(emitter).emit(eq(WatchProfilesQuery.class), any(), eq(new ProfileUpdate("u1", Long.MAX_VALUE, null)));
        verify(emitter).emit(eq(WatchPlayerRewardsQuery.class), any(),
                eq(new PlayerRewardsUpdate("u1", Long.MAX_VALUE, null)));
        verify(versions, never()).increment(any(), any());
    }
}
