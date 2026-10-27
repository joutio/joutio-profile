package com.joutio.profile.application.projection;

import com.joutio.profile.domain.event.RewardEvent;
import com.joutio.profile.domain.model.RewardCode;
import com.joutio.profile.domain.model.RewardSource;
import com.joutio.profile.domain.port.out.PlayerRewardRepositoryPort;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PlayerRewardProjectionTest {

    private final PlayerRewardRepositoryPort repository = mock(PlayerRewardRepositoryPort.class);
    private final PlayerRewardProjection projection = new PlayerRewardProjection(repository);

    @Test
    void rewardGranted_isProjectedIdempotently() {
        Instant at = Instant.parse("2026-10-23T10:00:00Z");
        projection.on(new RewardEvent.RewardGrantedEvent(
                "user-1", RewardCode.MICAH_AVATAR, RewardSource.BADGE, at));

        verify(repository).grant("user-1", RewardCode.MICAH_AVATAR, RewardSource.BADGE, at);
    }
}
