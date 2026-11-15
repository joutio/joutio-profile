package com.joutio.profile.infrastructure.out.persistence.repository;

import com.joutio.profile.infrastructure.out.persistence.entity.TitleUnlockEntity;
import com.joutio.profile.infrastructure.out.persistence.entity.TitleUnlockKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface TitleUnlockJpaRepository extends JpaRepository<TitleUnlockEntity, TitleUnlockKey> {

    /** Insère le titre ou garde la date la plus ancienne (idempotent, rejouable). */
    @Modifying
    @Query(value = """
            INSERT INTO title_unlock (user_id, topic_id, tier, unlocked_at)
            VALUES (:userId, :topicId, :tier, :unlockedAt)
            ON CONFLICT (user_id, topic_id, tier)
            DO UPDATE SET unlocked_at = LEAST(title_unlock.unlocked_at, EXCLUDED.unlocked_at)
            """, nativeQuery = true)
    void upsert(@Param("userId") String userId,
                @Param("topicId") String topicId,
                @Param("tier") String tier,
                @Param("unlockedAt") Instant unlockedAt);

    List<TitleUnlockEntity> findByUserIdIn(Collection<String> userIds);
}
