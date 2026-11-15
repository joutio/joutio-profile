package com.joutio.profile.application.handler.query;

import com.joutio.profile.domain.model.GameXp;
import com.joutio.profile.domain.model.Badge;
import com.joutio.profile.domain.model.PlayerProgress;
import com.joutio.profile.domain.model.PlayerUnlocks;
import com.joutio.profile.domain.model.TopicProgress;
import com.joutio.profile.domain.port.out.ProgressionAwardedGameRepositoryPort;
import com.joutio.profile.domain.port.out.ProgressionRepositoryPort;
import com.joutio.profile.domain.port.out.UnlockRepositoryPort;
import com.joutio.profile.domain.query.ProgressionQuery;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Handler Axon — queries de progression. Retourne une progression neutre
 * (0 XP, Elo de départ) pour un joueur n'ayant encore joué aucun duel.
 *
 * <p>Les badges viennent du read model des déblocages ({@code badge_unlock}) : ils ne dépendent
 * pas de l'ordre d'arrivée des événements de progression et de badges.</p>
 */
@Component
public class ProgressionQueryHandler {

    private final ProgressionRepositoryPort progressionRepositoryPort;
    private final ProgressionAwardedGameRepositoryPort awardedGameRepositoryPort;
    private final UnlockRepositoryPort unlockRepositoryPort;

    public ProgressionQueryHandler(ProgressionRepositoryPort progressionRepositoryPort,
                                   ProgressionAwardedGameRepositoryPort awardedGameRepositoryPort,
                                   UnlockRepositoryPort unlockRepositoryPort) {
        this.progressionRepositoryPort = progressionRepositoryPort;
        this.awardedGameRepositoryPort = awardedGameRepositoryPort;
        this.unlockRepositoryPort = unlockRepositoryPort;
    }

    @QueryHandler
    public PlayerProgress handle(ProgressionQuery.GetProgressionQuery query) {
        return handle(new ProgressionQuery.GetProgressionsByIdsQuery(List.of(query.userId())))
                .getFirst();
    }

    @QueryHandler
    public List<PlayerProgress> handle(ProgressionQuery.GetProgressionsByIdsQuery query) {
        Map<String, PlayerProgress> byId = progressionRepositoryPort.findByIds(query.userIds()).stream()
                .collect(Collectors.toMap(PlayerProgress::userId, Function.identity(), (a, _) -> a));
        Map<String, Set<Badge>> badges = unlockRepositoryPort.findBadgesByUserIds(query.userIds());
        return query.userIds().stream()
                .map(userId -> byId.getOrDefault(userId, PlayerProgress.empty(userId)).toBuilder()
                        .badges(badges.getOrDefault(userId, Set.of()))
                        .build())
                .toList();
    }

    @QueryHandler
    public PlayerUnlocks handle(ProgressionQuery.GetUnlocksQuery query) {
        return unlockRepositoryPort.findByUserId(query.userId());
    }

    @QueryHandler
    public List<PlayerUnlocks> handle(ProgressionQuery.GetUnlocksByIdsQuery query) {
        return unlockRepositoryPort.findByUserIds(query.userIds());
    }

    @QueryHandler
    public TopicProgress handle(ProgressionQuery.GetTopicProgressionQuery query) {
        PlayerProgress progress = progressionRepositoryPort.findById(query.userId())
                .orElseGet(() -> PlayerProgress.empty(query.userId()));

        return TopicProgress.of(query.topicId(),
                progress.xpForTopic(query.topicId()),
                progress.gamesForTopic(query.topicId()));
    }

    /**
     * Répartition par thème (XP et parties humaines), triée par parties décroissantes
     * puis par XP décroissante — les thèmes joués uniquement contre bot (0 partie) restent
     * présents pour leur XP.
     */
    @QueryHandler
    public List<TopicProgress> handle(ProgressionQuery.GetTopicProgressionsQuery query) {
        PlayerProgress progress = progressionRepositoryPort.findById(query.userId())
                .orElseGet(() -> PlayerProgress.empty(query.userId()));

        Set<String> topicIds = new LinkedHashSet<>(progress.xpByTopic().keySet());
        topicIds.addAll(progress.gamesByTopic().keySet());

        return topicIds.stream()
                .map(topicId -> TopicProgress.of(topicId,
                        progress.xpForTopic(topicId),
                        progress.gamesForTopic(topicId)))
                .sorted(Comparator.comparingInt(TopicProgress::games).reversed()
                        .thenComparing(Comparator.comparingInt(TopicProgress::xp).reversed())
                        .thenComparing(TopicProgress::topicId))
                .toList();
    }

    @QueryHandler
    public List<GameXp> handle(ProgressionQuery.GetGamesXpQuery query) {
        return awardedGameRepositoryPort.findGameXp(query.userId(), query.gameIds());
    }
}
