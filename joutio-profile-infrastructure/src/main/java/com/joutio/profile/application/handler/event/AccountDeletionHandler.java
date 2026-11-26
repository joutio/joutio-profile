package com.joutio.profile.application.handler.event;

import com.joutio.profile.application.service.PlayerLiveViews;
import com.joutio.axon.autoconfigure.erasure.AxonEventStoreErasureService;
import com.joutio.identity.domain.event.UserEvent.UserAccountDeletedEvent;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Effacement local du profil, de la progression et de la présence à la suppression du compte.
 * Les événements Axon correspondants sont purgés par le job d'effacement dédié.
 */
@Component
@ProcessingGroup("account-deletion-profile")
public class AccountDeletionHandler {

    private final JdbcTemplate jdbcTemplate;
    private final AxonEventStoreErasureService eventStoreErasureService;
    private final PlayerLiveViews liveViews;

    public AccountDeletionHandler(JdbcTemplate jdbcTemplate,
                                  AxonEventStoreErasureService eventStoreErasureService,
                                  PlayerLiveViews liveViews) {
        this.jdbcTemplate = jdbcTemplate;
        this.eventStoreErasureService = eventStoreErasureService;
        this.liveViews = liveViews;
    }

    @EventHandler
    @Transactional
    public void on(UserAccountDeletedEvent event) {
        String userId = event.userId();
        jdbcTemplate.update("DELETE FROM progression_activity_day_game WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM progression_activity_day WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM progression_activity WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM progression_awarded_game WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM progression_topic_games WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM progression_topic_entry WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM badge_unlock WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM title_unlock WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM progression_entry WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM player_reward WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM presence_session WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM presence_entry WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM profile_entry WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM pseudonym_claim WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM live_version WHERE user_id = ?", userId);
        liveViews.deleted(userId);
        eventStoreErasureService.eraseAggregates(List.of(
                userId,
                "progress:" + userId,
                "badges:" + userId,
                "rewards:" + userId));
    }
}
