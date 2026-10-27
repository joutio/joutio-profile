package com.joutio.profile.infrastructure.out.persistence.mapper;

import com.joutio.microservice.core.domain.model.i18n.Language;
import com.joutio.profile.domain.model.Profile;
import com.joutio.profile.domain.model.ProfileRules;
import com.joutio.profile.infrastructure.out.persistence.entity.ProfileEntity;

import java.util.Arrays;
import java.util.List;

/**
 * Mapper infrastructure - Convertit entre ProfileEntity (JPA) et Profile (domaine).
 * Seule classe autorisée à connaitre les deux types simultanément.
 */
public final class ProfileEntityMapper {

    /**
     * Convertit une entité JPA en modèle domaine.
     */
    public static Profile toDomain(ProfileEntity entity) {
        return Profile.builder()
                .userId(entity.getUserId())
                .email(entity.getEmail())
                .pseudonym(entity.getPseudonym())
                .bio(entity.getBio())
                .country(entity.getCountry())
                .avatarOptions(entity.getAvatarOptions())
                .bannerUrl(entity.getBannerUrl())
                .equippedTitle(entity.getEquippedTitle())
                .language(toLanguage(entity.getLanguage()))
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .onboardingCompletedAt(entity.getOnboardingCompletedAt())
                .interests(parseInterests(entity.getInterests()))
                .build();
    }

    /**
     * Convertit un modèle domaine en entité JPA.
     */
    public static ProfileEntity toEntity(Profile profile) {
        ProfileEntity entity = new ProfileEntity();
        entity.setUserId(profile.userId());
        entity.setEmail(profile.email());
        entity.setPseudonym(profile.pseudonym());
        entity.setBio(profile.bio());
        entity.setCountry(profile.country());
        entity.setAvatarOptions(profile.avatarOptions());
        entity.setBannerUrl(profile.bannerUrl());
        entity.setEquippedTitle(profile.equippedTitle());
        entity.setLanguage(profile.language() == null
                ? ProfileRules.DEFAULT_LANGUAGE.code()
                : profile.language().code());
        entity.setCreatedAt(profile.createdAt());
        entity.setUpdatedAt(profile.updatedAt());
        entity.setOnboardingCompletedAt(profile.onboardingCompletedAt());
        entity.setInterests(serializeInterests(profile.interests()));
        return entity;
    }

    private static List<String> parseInterests(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(code -> !code.isEmpty())
                .toList();
    }

    private static String serializeInterests(List<String> interests) {
        if (interests == null || interests.isEmpty()) {
            return null;
        }
        return String.join(",", interests);
    }

    private static Language toLanguage(String code) {
        if (code == null) {
            return ProfileRules.DEFAULT_LANGUAGE;
        }
        return Language.fromCode(code);
    }
}
