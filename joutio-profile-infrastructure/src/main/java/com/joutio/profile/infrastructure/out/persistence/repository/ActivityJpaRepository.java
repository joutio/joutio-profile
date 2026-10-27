package com.joutio.profile.infrastructure.out.persistence.repository;

import com.joutio.profile.infrastructure.out.persistence.entity.ActivityEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityJpaRepository extends JpaRepository<ActivityEntity, String> {
}
