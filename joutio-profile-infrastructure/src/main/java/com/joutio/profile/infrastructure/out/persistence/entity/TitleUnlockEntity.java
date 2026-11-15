package com.joutio.profile.infrastructure.out.persistence.entity;

import com.joutio.profile.domain.model.MasteryTier;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/** Projection JPA d'un titre thématique débloqué. */
@Getter
@Setter
@Entity
@Table(name = "title_unlock")
@IdClass(TitleUnlockKey.class)
public class TitleUnlockEntity {

    @Id
    @Column(name = "user_id", length = 255, nullable = false)
    private String userId;

    @Id
    @Column(name = "topic_id", length = 255, nullable = false)
    private String topicId;

    @Id
    @Column(name = "tier", length = 16, nullable = false)
    @Enumerated(EnumType.STRING)
    private MasteryTier tier;

    @Column(name = "unlocked_at", nullable = false)
    private Instant unlockedAt;
}
