package com.joutio.profile.infrastructure.out.persistence.adapter;

import com.joutio.profile.domain.model.Badge;
import com.joutio.profile.domain.model.BadgeUnlock;
import com.joutio.profile.domain.model.PlayerUnlocks;
import com.joutio.profile.domain.model.TitleUnlock;
import com.joutio.profile.domain.port.out.UnlockRepositoryPort;
import com.joutio.profile.infrastructure.out.persistence.entity.BadgeUnlockEntity;
import com.joutio.profile.infrastructure.out.persistence.entity.TitleUnlockEntity;
import com.joutio.profile.infrastructure.out.persistence.repository.BadgeUnlockJpaRepository;
import com.joutio.profile.infrastructure.out.persistence.repository.TitleUnlockJpaRepository;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class UnlockRepositoryAdapter implements UnlockRepositoryPort {

    /** Plus récents d'abord, puis palier le plus haut, puis sujet (ordre stable). */
    private static final Comparator<TitleUnlock> TITLE_ORDER =
            Comparator.comparing(TitleUnlock::unlockedAt).reversed()
                    .thenComparing(Comparator.comparing(TitleUnlock::tier).reversed())
                    .thenComparing(TitleUnlock::topicId);

    private static final Comparator<BadgeUnlock> BADGE_ORDER =
            Comparator.comparing(BadgeUnlock::earnedAt).reversed()
                    .thenComparing(BadgeUnlock::badge);

    private final TitleUnlockJpaRepository titleRepository;
    private final BadgeUnlockJpaRepository badgeRepository;

    public UnlockRepositoryAdapter(TitleUnlockJpaRepository titleRepository,
                                   BadgeUnlockJpaRepository badgeRepository) {
        this.titleRepository = titleRepository;
        this.badgeRepository = badgeRepository;
    }

    @Override
    public void saveTitle(TitleUnlock unlock) {
        titleRepository.upsert(unlock.userId(), unlock.topicId(), unlock.tier().name(),
                unlock.unlockedAt());
    }

    @Override
    public void saveBadge(BadgeUnlock unlock) {
        badgeRepository.upsert(unlock.userId(), unlock.badge().name(), unlock.earnedAt(),
                unlock.gameId());
    }

    @Override
    public PlayerUnlocks findByUserId(String userId) {
        return findByUserIds(List.of(userId)).getFirst();
    }

    @Override
    public List<PlayerUnlocks> findByUserIds(Collection<String> userIds) {
        if (userIds.isEmpty()) {
            return List.of();
        }
        Map<String, List<TitleUnlock>> titles = titleRepository.findByUserIdIn(userIds).stream()
                .map(UnlockRepositoryAdapter::toDomain)
                .collect(Collectors.groupingBy(TitleUnlock::userId));
        Map<String, List<BadgeUnlock>> badges = badgeRepository.findByUserIdIn(userIds).stream()
                .map(UnlockRepositoryAdapter::toDomain)
                .collect(Collectors.groupingBy(BadgeUnlock::userId));

        return userIds.stream()
                .map(userId -> new PlayerUnlocks(userId,
                        titles.getOrDefault(userId, List.of()).stream().sorted(TITLE_ORDER).toList(),
                        badges.getOrDefault(userId, List.of()).stream().sorted(BADGE_ORDER).toList()))
                .toList();
    }

    @Override
    public Map<String, Set<Badge>> findBadgesByUserIds(Collection<String> userIds) {
        Map<String, Set<Badge>> byUser = new HashMap<>();
        if (userIds.isEmpty()) {
            return byUser;
        }
        for (BadgeUnlockEntity entity : badgeRepository.findByUserIdIn(userIds)) {
            byUser.computeIfAbsent(entity.getUserId(), ignored -> EnumSet.noneOf(Badge.class))
                    .add(entity.getBadge());
        }
        return byUser;
    }

    @Override
    public void deleteAll() {
        titleRepository.deleteAllInBatch();
        badgeRepository.deleteAllInBatch();
    }

    private static TitleUnlock toDomain(TitleUnlockEntity entity) {
        return new TitleUnlock(entity.getUserId(), entity.getTopicId(), entity.getTier(),
                entity.getUnlockedAt());
    }

    private static BadgeUnlock toDomain(BadgeUnlockEntity entity) {
        return new BadgeUnlock(entity.getUserId(), entity.getBadge(), entity.getEarnedAt(),
                entity.getGameId());
    }
}
