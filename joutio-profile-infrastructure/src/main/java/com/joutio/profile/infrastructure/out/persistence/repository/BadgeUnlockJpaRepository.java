package com.joutio.profile.infrastructure.out.persistence.repository;

import com.joutio.profile.infrastructure.out.persistence.entity.BadgeUnlockEntity;
import com.joutio.profile.infrastructure.out.persistence.entity.BadgeUnlockKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface BadgeUnlockJpaRepository extends JpaRepository<BadgeUnlockEntity, BadgeUnlockKey> {

    /** Insère le badge ou garde la date la plus ancienne (idempotent, rejouable). */
    @Modifying
    @Query(value = """
            INSERT INTO badge_unlock (user_id, badge, earned_at, game_id)
            VALUES (:userId, :badge, :earnedAt, :gameId)
            ON CONFLICT (user_id, badge)
            DO UPDATE SET earned_at = LEAST(badge_unlock.earned_at, EXCLUDED.earned_at),
                          game_id = COALESCE(badge_unlock.game_id, EXCLUDED.game_id)
            """, nativeQuery = true)
    void upsert(@Param("userId") String userId,
                @Param("badge") String badge,
                @Param("earnedAt") Instant earnedAt,
                @Param("gameId") String gameId);

    List<BadgeUnlockEntity> findByUserIdIn(Collection<String> userIds);
}
