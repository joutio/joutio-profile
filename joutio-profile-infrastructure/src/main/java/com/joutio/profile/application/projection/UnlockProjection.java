package com.joutio.profile.application.projection;

import com.joutio.profile.application.service.PlayerLiveViews;
import com.joutio.profile.application.service.GameRewardService;
import com.joutio.profile.domain.event.ProgressionEvent;
import com.joutio.profile.domain.model.BadgeUnlock;
import com.joutio.profile.domain.model.MasteryTier;
import com.joutio.profile.domain.model.TitleUnlock;
import com.joutio.profile.domain.port.out.UnlockRepositoryPort;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.eventhandling.ResetHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Projette les déblocages datés : titres thématiques ({@code title_unlock}) et badges
 * ({@code badge_unlock}).
 *
 * <p>Groupe dédié qui ne touche à aucune autre table : un reset ou un replay est sans risque.
 * Toutes les écritures sont idempotentes et gardent la date la plus ancienne.</p>
 */
@Component
@ProcessingGroup("unlock-projection")
public class UnlockProjection {

    private final UnlockRepositoryPort repositoryPort;
    private final GameRewardService gameRewards;
    private final PlayerLiveViews liveViews;

    public UnlockProjection(UnlockRepositoryPort repositoryPort, GameRewardService gameRewards,
                            PlayerLiveViews liveViews) {
        this.repositoryPort = repositoryPort;
        this.gameRewards = gameRewards;
        this.liveViews = liveViews;
    }

    /** Titre Fer : première partie humaine terminée dans le sujet (abandon compris, bot exclu). */
    @EventHandler
    @Transactional
    public void on(ProgressionEvent.XpAwardedEvent event) {
        if (event.botGame()) {
            return;
        }
        repositoryPort.saveTitle(new TitleUnlock(
                event.userId(), event.topicId(), MasteryTier.FER, event.awardedAt()));
    }

    /** Paliers suivants : l'agrégat annonce chaque palier franchi, dans l'ordre. */
    @EventHandler
    @Transactional
    public void on(ProgressionEvent.MasteryTierReachedEvent event) {
        repositoryPort.saveTitle(new TitleUnlock(
                event.userId(), event.topicId(), event.tier(), event.reachedAt()));
    }

    @EventHandler
    @Transactional
    public void on(ProgressionEvent.BadgeEarnedEvent event) {
        repositoryPort.saveBadge(new BadgeUnlock(
                event.userId(), event.badge(), event.earnedAt(), event.gameId()));
        if (event.gameId() != null) {
            gameRewards.changed(event.gameId());
        }
        liveViews.progressionChanged(event.userId());
    }

    @ResetHandler
    @Transactional
    public void reset() {
        repositoryPort.deleteAll();
    }
}
