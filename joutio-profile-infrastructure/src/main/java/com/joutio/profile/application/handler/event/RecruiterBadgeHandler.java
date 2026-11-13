package com.joutio.profile.application.handler.event;

import com.joutio.identity.domain.event.UserEvent;
import com.joutio.profile.domain.command.ProgressionCommand;
import com.joutio.profile.domain.event.ProfileEvent;
import com.joutio.profile.domain.model.BadgeMetric;
import com.joutio.profile.domain.model.ProgressionRules;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

/**
 * Badge **Recruteur** ({@code design/specs/screens/partage.md} § 6) : un filleul compte pour son parrain quand il a
 * terminé son premier duel entre joueurs (onboarding terminé), pour décourager les faux comptes.
 *
 * <p>Le rattachement ({@code UserReferredEvent}, identity) et la fin d'onboarding ({@code ProfileOnboardingCompletedEvent})
 * peuvent arriver dans n'importe quel ordre : les deux faits sont notés dans {@code recruiter_tracking} et le parrain est
 * crédité une seule fois, dès qu'ils sont réunis ({@code credited_at}).</p>
 */
@Component
@ProcessingGroup("recruiter-badge")
public class RecruiterBadgeHandler {

    private static final Logger logger = LoggerFactory.getLogger(RecruiterBadgeHandler.class);

    private final JdbcTemplate jdbcTemplate;
    private final CommandGateway commandGateway;

    public RecruiterBadgeHandler(JdbcTemplate jdbcTemplate, CommandGateway commandGateway) {
        this.jdbcTemplate = jdbcTemplate;
        this.commandGateway = commandGateway;
    }

    @EventHandler
    @Transactional
    public void on(UserEvent.UserReferredEvent event) {
        jdbcTemplate.update("""
                INSERT INTO recruiter_tracking (user_id, referrer_id) VALUES (?, ?)
                ON CONFLICT (user_id) DO UPDATE SET referrer_id = COALESCE(recruiter_tracking.referrer_id, EXCLUDED.referrer_id)
                """, event.userId(), event.referrerId());
        creditIfQualified(event.userId(), event.referredAt());
    }

    @EventHandler
    @Transactional
    public void on(ProfileEvent.ProfileOnboardingCompletedEvent event) {
        jdbcTemplate.update("""
                INSERT INTO recruiter_tracking (user_id, onboarded_at) VALUES (?, ?)
                ON CONFLICT (user_id) DO UPDATE SET onboarded_at = COALESCE(recruiter_tracking.onboarded_at, EXCLUDED.onboarded_at)
                """, event.userId(), Timestamp.from(event.completedAt()));
        creditIfQualified(event.userId(), event.completedAt());
    }

    @EventHandler
    @Transactional
    public void on(UserEvent.UserAccountDeletedEvent event) {
        jdbcTemplate.update("DELETE FROM recruiter_tracking WHERE user_id = ?", event.userId());
    }

    private void creditIfQualified(String userId, Instant at) {
        List<String> referrers = jdbcTemplate.queryForList("""
                UPDATE recruiter_tracking SET credited_at = ?
                WHERE user_id = ? AND referrer_id IS NOT NULL AND onboarded_at IS NOT NULL AND credited_at IS NULL
                RETURNING referrer_id
                """, String.class, Timestamp.from(at), userId);
        for (String referrerId : referrers) {
            commandGateway.sendAndWait(new ProgressionCommand.RecordBadgeCounterCommand(
                    ProgressionRules.badgeIdFor(referrerId),
                    referrerId,
                    BadgeMetric.REFERRALS_QUALIFIED,
                    userId,
                    at));
            logger.info("Recruiter badge credited: referrerId={}, userId={}", referrerId, userId);
        }
    }
}
