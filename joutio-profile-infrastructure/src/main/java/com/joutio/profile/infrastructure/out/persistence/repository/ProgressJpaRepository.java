package com.joutio.profile.infrastructure.out.persistence.repository;

import com.joutio.profile.infrastructure.out.persistence.entity.ProgressEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProgressJpaRepository extends JpaRepository<ProgressEntity, String> {
}
