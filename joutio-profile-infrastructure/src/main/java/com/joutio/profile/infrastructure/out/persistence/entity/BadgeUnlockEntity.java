package com.joutio.profile.infrastructure.out.persistence.entity;

import com.joutio.profile.domain.model.Badge;
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

/** Projection JPA d'un badge obtenu. */
@Getter
@Setter
@Entity
@Table(name = "badge_unlock")
@IdClass(BadgeUnlockKey.class)
public class BadgeUnlockEntity {

    @Id
    @Column(name = "user_id", length = 255, nullable = false)
    private String userId;

    @Id
    @Column(name = "badge", length = 50, nullable = false)
    @Enumerated(EnumType.STRING)
    private Badge badge;

    @Column(name = "earned_at", nullable = false)
    private Instant earnedAt;
}
