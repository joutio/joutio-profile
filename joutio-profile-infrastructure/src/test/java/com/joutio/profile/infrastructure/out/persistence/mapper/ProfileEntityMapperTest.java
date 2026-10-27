package com.joutio.profile.infrastructure.out.persistence.mapper;

import com.joutio.microservice.core.domain.model.i18n.Language;
import com.joutio.profile.domain.model.Profile;
import com.joutio.profile.infrastructure.out.persistence.entity.ProfileEntity;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProfileEntityMapperTest {

    @Test
    void roundTripsOrderedInterests() {
        Instant now = Instant.parse("2026-10-26T10:00:00Z");
        Profile profile = Profile.builder()
                .userId("u1")
                .email("u1@joutio.dev")
                .pseudonym("Alice")
                .language(Language.FR)
                .createdAt(now)
                .updatedAt(now)
                .interests(List.of("SCIENCE", "HISTORY"))
                .build();

        ProfileEntity entity = ProfileEntityMapper.toEntity(profile);
        Profile mapped = ProfileEntityMapper.toDomain(entity);

        assertThat(entity.getInterests()).isEqualTo("SCIENCE,HISTORY");
        assertThat(mapped.interests()).containsExactly("SCIENCE", "HISTORY");
    }

    @Test
    void emptyInterestsAreStoredAsNullAndReadAsEmpty() {
        Instant now = Instant.parse("2026-10-26T10:00:00Z");
        Profile profile = Profile.builder()
                .userId("u1")
                .email("u1@joutio.dev")
                .pseudonym("Alice")
                .language(Language.FR)
                .createdAt(now)
                .updatedAt(now)
                .build();

        ProfileEntity entity = ProfileEntityMapper.toEntity(profile);

        assertThat(entity.getInterests()).isNull();
        assertThat(ProfileEntityMapper.toDomain(entity).interests()).isEmpty();
    }
}
