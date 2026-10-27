package com.joutio.profile.domain.aggregate;

import com.joutio.profile.domain.command.ProgressionCommand;
import com.joutio.profile.domain.event.ProgressionEvent;
import com.joutio.profile.domain.model.EloRules;
import com.joutio.profile.domain.model.MasteryTier;
import com.joutio.profile.domain.model.ProgressionRules;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateCreationPolicy;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.modelling.command.CreationPolicy;
import org.axonframework.spring.stereotype.Aggregate;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * PlayerProgressAggregate — progression d'un joueur : **XP par thème** et **Elo**. Les
 * **badges** vivent dans `BadgeAggregate`.
 *
 * <p>Identifiant d'agrégat namespacé ({@code progress:<userId>}). Créé à la volée
 * au premier {@code AwardXpCommand} ({@link AggregateCreationPolicy#CREATE_IF_MISSING}).
 * L'attribution est idempotente : un même {@code gameId} n'est crédité qu'une fois.</p>
 */
@Aggregate
public class PlayerProgressAggregate {

    @AggregateIdentifier
    private String progressId;

    private String userId;

    private final Map<String, Integer> xpByTopic = new HashMap<>();
    private final Set<String> awardedGameIds = new HashSet<>();
    /** Nombre de duels classés contre un adversaire pour un jour local : plafond anti-farm. */
    private final Map<String, Integer> ratedOpponentDayCounts = new HashMap<>();

    private int rating = EloRules.INITIAL_RATING;
    private int peakRating = EloRules.INITIAL_RATING;
    private int ratedGames;
    private boolean provisional = true;

    protected PlayerProgressAggregate() {
    }

    @CommandHandler
    @CreationPolicy(AggregateCreationPolicy.CREATE_IF_MISSING)
    public void handle(ProgressionCommand.AwardXpCommand command) {
        if (this.progressId == null) {
            this.progressId = command.progressId();
        }
        if (this.userId == null) {
            this.userId = command.userId();
        }

        if (awardedGameIds.contains(command.gameId())) {
            return;
        }

        int xp = ProgressionRules.xpFor(command.gameScore(), command.won(), command.botGame());
        Instant now = Instant.now();

        int newRating = rating;
        int newPeakRating = peakRating;
        int newRatedGames = ratedGames;
        int ratingDelta = 0;
        String ratedOpponentId = null;
        java.time.LocalDate ratedDay = null;

        if (!command.botGame()
                && command.opponentId() != null
                && command.opponentRating() != null
                && command.ratedDay() != null) {
            String opponentDayKey = command.ratedDay() + "|" + command.opponentId();
            int playedToday = ratedOpponentDayCounts.getOrDefault(opponentDayKey, 0);
            if (playedToday < EloRules.DAILY_OPPONENT_CAP) {
                double score = command.draw() ? 0.5 : command.won() ? 1.0 : 0.0;
                newRating = EloRules.newRating(rating, command.opponentRating(), score, ratedGames);
                newPeakRating = Math.max(peakRating, newRating);
                newRatedGames = ratedGames + 1;
                ratingDelta = newRating - rating;
                ratedOpponentId = command.opponentId();
                ratedDay = command.ratedDay();
            }
        }

        AggregateLifecycle.apply(new ProgressionEvent.XpAwardedEvent(
                command.userId(),
                command.gameId(),
                command.topicId(),
                xp,
                command.gameScore(),
                command.won(),
                command.draw(),
                command.botGame(),
                command.correctAnswers(),
                command.fastAnswers(),
                command.imageCorrectAnswers(),
                now,
                newRating,
                newPeakRating,
                newRatedGames,
                EloRules.isProvisional(newRatedGames),
                ratingDelta,
                ratedOpponentId,
                ratedDay
        ));

        // Titre thématique : le palier de maîtrise du sujet franchi est annoncé (célébration).
        int topicXpAfter = xpByTopic.getOrDefault(command.topicId(), 0);
        MasteryTier tierBefore = MasteryTier.tierFor(topicXpAfter - xp);
        MasteryTier tierAfter = MasteryTier.tierFor(topicXpAfter);
        if (tierAfter.ordinal() > tierBefore.ordinal()) {
            AggregateLifecycle.apply(new ProgressionEvent.MasteryTierReachedEvent(
                    command.userId(),
                    command.topicId(),
                    tierAfter,
                    now
            ));
        }
    }

    @EventSourcingHandler
    public void on(ProgressionEvent.XpAwardedEvent event) {
        this.progressId = ProgressionRules.progressIdFor(event.userId());
        this.userId = event.userId();
        this.xpByTopic.merge(event.topicId(), event.xp(), Integer::sum);
        this.awardedGameIds.add(event.gameId());
        this.rating = event.rating();
        this.peakRating = event.peakRating();
        this.ratedGames = event.ratedGames();
        this.provisional = event.provisional();
        if (event.ratedOpponentId() != null && event.ratedDay() != null) {
            this.ratedOpponentDayCounts.merge(
                    event.ratedDay() + "|" + event.ratedOpponentId(), 1, Integer::sum);
        }
    }
}
