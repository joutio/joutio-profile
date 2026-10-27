package com.joutio.profile.domain.aggregate;

import com.joutio.axon.test.JoutioAxonMatchers;
import com.joutio.microservice.core.domain.model.i18n.Language;
import com.joutio.profile.domain.command.ProfileCommand;
import com.joutio.profile.domain.event.ProfileEvent;
import com.joutio.profile.domain.exception.ProfileProblems;
import org.axonframework.test.aggregate.AggregateTestFixture;
import org.junit.jupiter.api.Test;

import java.time.Instant;

/**
 * Test Axon in-memory de l'agrégat {@link ProfileAggregate} via {@link AggregateTestFixture}.
 * <p>
 * 100 % in-memory : event store de l'agrégat en mémoire, aucun Postgres ni Axon Server.
 */
class ProfileAggregateTest {

    private static final Instant NOW = Instant.parse("2026-09-20T10:00:00Z");

    private final AggregateTestFixture<ProfileAggregate> fixture =
            new AggregateTestFixture<>(ProfileAggregate.class);

    @Test
    void create_appliesProfileCreatedEvent() {
        fixture
                .givenNoPriorActivity()
                .when(new ProfileCommand.CreateProfileCommand("user-1", "user@joutio.dev", "Alice"))
                .expectEventsMatching(JoutioAxonMatchers.singlePayloadMatching(
                        ProfileEvent.ProfileCreatedEvent.class,
                        e -> "user-1".equals(((ProfileEvent.ProfileCreatedEvent) e).userId())
                                && "user@joutio.dev".equals(((ProfileEvent.ProfileCreatedEvent) e).email())
                                && "Alice".equals(((ProfileEvent.ProfileCreatedEvent) e).pseudonym())));
    }

    @Test
    void createWithBlankPseudonym_throwsValidationProblem() {
        fixture
                .givenNoPriorActivity()
                .when(new ProfileCommand.CreateProfileCommand("user-1", "user@joutio.dev", "  "))
                .expectException(ProfileProblems.PseudonymBlankProblem.class);
    }

    @Test
    void createWithTooLongPseudonym_throwsValidationProblem() {
        fixture
                .givenNoPriorActivity()
                .when(new ProfileCommand.CreateProfileCommand(
                        "user-1",
                        "user@joutio.dev",
                        "x".repeat(101)))
                .expectException(ProfileProblems.PseudonymTooLongProblem.class);
    }

    @Test
    void createWhenProfileAlreadyExists_emitsNoEvent() {
        fixture
                .given(createdProfile())
                .when(new ProfileCommand.CreateProfileCommand("user-1", "replay@joutio.dev", "Autre"))
                .expectNoEvents();
    }

    @Test
    void updatePseudonymByOwner_appliesPseudonymUpdatedEvent() {
        fixture
                .given(createdProfile())
                .when(new ProfileCommand.UpdateProfilePseudonymCommand("user-1", "user-1", "Alicia"))
                .expectEventsMatching(JoutioAxonMatchers.singlePayloadMatching(
                        ProfileEvent.ProfilePseudonymUpdatedEvent.class,
                        e -> "user-1".equals(((ProfileEvent.ProfilePseudonymUpdatedEvent) e).userId())
                                && "user-1".equals(((ProfileEvent.ProfilePseudonymUpdatedEvent) e).requestedBy())
                                && "Alicia".equals(((ProfileEvent.ProfilePseudonymUpdatedEvent) e).pseudonym())));
    }

    @Test
    void updatePseudonymWithSameValue_emitsNoEvent() {
        fixture
                .given(createdProfile())
                .when(new ProfileCommand.UpdateProfilePseudonymCommand("user-1", "user-1", "Alice"))
                .expectNoEvents();
    }

    @Test
    void updateByNonOwner_isRejected() {
        fixture
                .given(createdProfile())
                .when(new ProfileCommand.UpdateProfilePseudonymCommand("user-1", "user-2", "Alicia"))
                .expectException(ProfileProblems.ProfileNotOwnerProblem.class);
    }

    @Test
    void updatePseudonymWithBlank_throwsValidationProblem() {
        fixture
                .given(createdProfile())
                .when(new ProfileCommand.UpdateProfilePseudonymCommand("user-1", "user-1", ""))
                .expectException(ProfileProblems.PseudonymBlankProblem.class);
    }

    @Test
    void updateBio_appliesBioUpdatedEvent() {
        fixture
                .given(createdProfile())
                .when(new ProfileCommand.UpdateProfileBioCommand("user-1", "user-1", "Full stack developer"))
                .expectEventsMatching(JoutioAxonMatchers.singlePayloadMatching(
                        ProfileEvent.ProfileBioUpdatedEvent.class,
                        e -> "Full stack developer".equals(((ProfileEvent.ProfileBioUpdatedEvent) e).bio())));
    }

    @Test
    void updateBioWithSameValue_emitsNoEvent() {
        fixture
                .given(createdProfile(),
                        new ProfileEvent.ProfileBioUpdatedEvent("user-1", "user-1", "Full stack developer", NOW))
                .when(new ProfileCommand.UpdateProfileBioCommand("user-1", "user-1", "Full stack developer"))
                .expectNoEvents();
    }

    @Test
    void updateBioTooLong_throwsValidationProblem() {
        fixture
                .given(createdProfile())
                .when(new ProfileCommand.UpdateProfileBioCommand("user-1", "user-1", "x".repeat(301)))
                .expectException(ProfileProblems.BioTooLongProblem.class);
    }

    @Test
    void updateCountry_appliesCountryUpdatedEvent() {
        fixture
                .given(createdProfile())
                .when(new ProfileCommand.UpdateProfileCountryCommand("user-1", "user-1", "France"))
                .expectEventsMatching(JoutioAxonMatchers.singlePayloadMatching(
                        ProfileEvent.ProfileCountryUpdatedEvent.class,
                        e -> "France".equals(((ProfileEvent.ProfileCountryUpdatedEvent) e).country())));
    }

    @Test
    void updateCountryTooLong_throwsValidationProblem() {
        fixture
                .given(createdProfile())
                .when(new ProfileCommand.UpdateProfileCountryCommand("user-1", "user-1", "x".repeat(101)))
                .expectException(ProfileProblems.CountryTooLongProblem.class);
    }

    @Test
    void updateAvatar_appliesAvatarUpdatedEvent() {
        fixture
                .given(createdProfile())
                .when(new ProfileCommand.UpdateProfileAvatarCommand("user-1", "user-1", "{\"hairVariant\":[\"full\"]}"))
                .expectEventsMatching(JoutioAxonMatchers.singlePayloadMatching(
                        ProfileEvent.ProfileAvatarUpdatedEvent.class,
                        e -> "{\"hairVariant\":[\"full\"]}"
                                .equals(((ProfileEvent.ProfileAvatarUpdatedEvent) e).avatarOptions())));
    }

    @Test
    void updateAvatarTooLong_throwsValidationProblem() {
        fixture
                .given(createdProfile())
                .when(new ProfileCommand.UpdateProfileAvatarCommand("user-1", "user-1", "x".repeat(2001)))
                .expectException(ProfileProblems.AvatarOptionsTooLongProblem.class);
    }

    @Test
    void updateBanner_appliesBannerUpdatedEvent() {
        fixture
                .given(createdProfile())
                .when(new ProfileCommand.UpdateProfileBannerCommand("user-1", "user-1", "https://media.test/banner.jpg"))
                .expectEventsMatching(JoutioAxonMatchers.singlePayloadMatching(
                        ProfileEvent.ProfileBannerUpdatedEvent.class,
                        e -> "https://media.test/banner.jpg"
                                .equals(((ProfileEvent.ProfileBannerUpdatedEvent) e).bannerUrl())));
    }

    @Test
    void updateBannerWithSameValue_emitsNoEvent() {
        fixture
                .given(createdProfile(),
                        new ProfileEvent.ProfileBannerUpdatedEvent("user-1", "user-1", "https://media.test/banner.jpg", NOW))
                .when(new ProfileCommand.UpdateProfileBannerCommand("user-1", "user-1", "https://media.test/banner.jpg"))
                .expectNoEvents();
    }

    @Test
    void updateBannerWithNull_clearsBanner() {
        fixture
                .given(createdProfile(),
                        new ProfileEvent.ProfileBannerUpdatedEvent("user-1", "user-1", "https://media.test/banner.jpg", NOW))
                .when(new ProfileCommand.UpdateProfileBannerCommand("user-1", "user-1", null))
                .expectEventsMatching(JoutioAxonMatchers.singlePayloadMatching(
                        ProfileEvent.ProfileBannerUpdatedEvent.class,
                        e -> ((ProfileEvent.ProfileBannerUpdatedEvent) e).bannerUrl() == null));
    }

    @Test
    void updateBannerTooLong_throwsValidationProblem() {
        fixture
                .given(createdProfile())
                .when(new ProfileCommand.UpdateProfileBannerCommand("user-1", "user-1", "https://x.test/" + "a".repeat(1100)))
                .expectException(ProfileProblems.BannerUrlTooLongProblem.class);
    }

    @Test
    void updateBannerByNonOwner_throwsPermissionProblem() {
        fixture
                .given(createdProfile())
                .when(new ProfileCommand.UpdateProfileBannerCommand("user-1", "intruder", "https://media.test/banner.jpg"))
                .expectException(ProfileProblems.ProfileNotOwnerProblem.class);
    }

    @Test
    void updateLanguage_appliesLanguageUpdatedEvent() {
        fixture
                .given(createdProfile())
                .when(new ProfileCommand.UpdateProfileLanguageCommand("user-1", "user-1", Language.EN))
                .expectEventsMatching(JoutioAxonMatchers.singlePayloadMatching(
                        ProfileEvent.ProfileLanguageUpdatedEvent.class,
                        e -> Language.EN == ((ProfileEvent.ProfileLanguageUpdatedEvent) e).language()));
    }

    @Test
    void updateLanguageWithSameValue_emitsNoEvent() {
        fixture
                .given(createdProfile(),
                        new ProfileEvent.ProfileLanguageUpdatedEvent("user-1", "user-1", Language.EN, NOW))
                .when(new ProfileCommand.UpdateProfileLanguageCommand("user-1", "user-1", Language.EN))
                .expectNoEvents();
    }

    @Test
    void updateLanguageWithNull_throwsValidationProblem() {
        fixture
                .given(createdProfile())
                .when(new ProfileCommand.UpdateProfileLanguageCommand("user-1", "user-1", null))
                .expectException(ProfileProblems.LanguageMissingProblem.class);
    }

    @Test
    void updateEquippedTitle_appliesEquippedTitleUpdatedEvent() {
        fixture
                .given(createdProfile())
                .when(new ProfileCommand.UpdateEquippedTitleCommand("user-1", "user-1", "topic-1:OR"))
                .expectEventsMatching(JoutioAxonMatchers.singlePayloadMatching(
                        ProfileEvent.ProfileEquippedTitleUpdatedEvent.class,
                        e -> "topic-1:OR".equals(
                                ((ProfileEvent.ProfileEquippedTitleUpdatedEvent) e).titleKey())));
    }

    @Test
    void updateEquippedTitleWithNull_clearsIt() {
        fixture
                .given(createdProfile(),
                        new ProfileEvent.ProfileEquippedTitleUpdatedEvent(
                                "user-1", "user-1", "topic-1:OR", NOW))
                .when(new ProfileCommand.UpdateEquippedTitleCommand("user-1", "user-1", null))
                .expectEventsMatching(JoutioAxonMatchers.singlePayloadMatching(
                        ProfileEvent.ProfileEquippedTitleUpdatedEvent.class,
                        e -> ((ProfileEvent.ProfileEquippedTitleUpdatedEvent) e).titleKey() == null));
    }

    @Test
    void updateEquippedTitleWithSameValue_emitsNoEvent() {
        fixture
                .given(createdProfile(),
                        new ProfileEvent.ProfileEquippedTitleUpdatedEvent(
                                "user-1", "user-1", "topic-1:OR", NOW))
                .when(new ProfileCommand.UpdateEquippedTitleCommand("user-1", "user-1", "topic-1:OR"))
                .expectNoEvents();
    }

    @Test
    void updateEquippedTitleWithMalformedKey_throwsValidationProblem() {
        fixture
                .given(createdProfile())
                .when(new ProfileCommand.UpdateEquippedTitleCommand("user-1", "user-1", "topic-1"))
                .expectException(ProfileProblems.EquippedTitleInvalidProblem.class);
    }

    @Test
    void updateEquippedTitleWithUnknownGrade_throwsValidationProblem() {
        fixture
                .given(createdProfile())
                .when(new ProfileCommand.UpdateEquippedTitleCommand("user-1", "user-1", "topic-1:GRAND_MAITRE"))
                .expectException(ProfileProblems.EquippedTitleInvalidProblem.class);
    }

    @Test
    void updateEquippedTitleWithTooLongKey_throwsValidationProblem() {
        fixture
                .given(createdProfile())
                .when(new ProfileCommand.UpdateEquippedTitleCommand(
                        "user-1", "user-1", "x".repeat(126) + ":OR"))
                .expectException(ProfileProblems.EquippedTitleTooLongProblem.class);
    }

    @Test
    void updateEquippedTitleByAnotherUser_isRejected() {
        fixture
                .given(createdProfile())
                .when(new ProfileCommand.UpdateEquippedTitleCommand("user-1", "user-2", "topic-1:OR"))
                .expectException(ProfileProblems.ProfileNotOwnerProblem.class);
    }

    @Test
    void updateInterests_appliesOrderedNormalizedCodes() {
        fixture
                .given(createdProfile())
                .when(new ProfileCommand.UpdateProfileInterestsCommand(
                        "user-1", "user-1", java.util.List.of("history", "SCIENCE")))
                .expectEventsMatching(JoutioAxonMatchers.singlePayloadMatching(
                        ProfileEvent.ProfileInterestsUpdatedEvent.class,
                        e -> java.util.List.of("HISTORY", "SCIENCE")
                                .equals(((ProfileEvent.ProfileInterestsUpdatedEvent) e).interests())));
    }

    @Test
    void updateInterestsWithSameValue_emitsNoEvent() {
        fixture
                .given(createdProfile(),
                        new ProfileEvent.ProfileInterestsUpdatedEvent(
                                "user-1", "user-1", java.util.List.of("HISTORY"), NOW))
                .when(new ProfileCommand.UpdateProfileInterestsCommand(
                        "user-1", "user-1", java.util.List.of("history")))
                .expectNoEvents();
    }

    @Test
    void updateInterestsWithEmptyList_clearsThem() {
        fixture
                .given(createdProfile(),
                        new ProfileEvent.ProfileInterestsUpdatedEvent(
                                "user-1", "user-1", java.util.List.of("HISTORY"), NOW))
                .when(new ProfileCommand.UpdateProfileInterestsCommand(
                        "user-1", "user-1", java.util.List.of()))
                .expectEventsMatching(JoutioAxonMatchers.singlePayloadMatching(
                        ProfileEvent.ProfileInterestsUpdatedEvent.class,
                        e -> ((ProfileEvent.ProfileInterestsUpdatedEvent) e).interests().isEmpty()));
    }

    @Test
    void updateInterestsWithTooMany_throwsValidationProblem() {
        fixture
                .given(createdProfile())
                .when(new ProfileCommand.UpdateProfileInterestsCommand(
                        "user-1", "user-1",
                        java.util.List.of("HISTORY", "SCIENCE", "SPORTS", "MOVIES")))
                .expectException(ProfileProblems.InvalidInterestsProblem.class);
    }

    @Test
    void updateInterestsWithDuplicate_throwsValidationProblem() {
        fixture
                .given(createdProfile())
                .when(new ProfileCommand.UpdateProfileInterestsCommand(
                        "user-1", "user-1", java.util.List.of("HISTORY", "history")))
                .expectException(ProfileProblems.InvalidInterestsProblem.class);
    }

    @Test
    void updateInterestsWithMalformedCode_throwsValidationProblem() {
        fixture
                .given(createdProfile())
                .when(new ProfileCommand.UpdateProfileInterestsCommand(
                        "user-1", "user-1", java.util.List.of("Histoire!")))
                .expectException(ProfileProblems.InvalidInterestsProblem.class);
    }

    @Test
    void updateInterestsByAnotherUser_isRejected() {
        fixture
                .given(createdProfile())
                .when(new ProfileCommand.UpdateProfileInterestsCommand(
                        "user-1", "user-2", java.util.List.of("HISTORY")))
                .expectException(ProfileProblems.ProfileNotOwnerProblem.class);
    }

    @Test
    void completeOnboarding_appliesOnboardingCompletedEvent() {
        fixture
                .given(createdProfile())
                .when(new ProfileCommand.CompleteOnboardingCommand("user-1", NOW))
                .expectEventsMatching(JoutioAxonMatchers.singlePayloadMatching(
                        ProfileEvent.ProfileOnboardingCompletedEvent.class,
                        e -> "user-1".equals(((ProfileEvent.ProfileOnboardingCompletedEvent) e).userId())
                                && NOW.equals(((ProfileEvent.ProfileOnboardingCompletedEvent) e).completedAt())));
    }

    @Test
    void completeOnboarding_whenAlreadyCompleted_emitsNoEvent() {
        fixture
                .given(createdProfile(),
                        new ProfileEvent.ProfileOnboardingCompletedEvent("user-1", NOW))
                .when(new ProfileCommand.CompleteOnboardingCommand("user-1", NOW.plusSeconds(60)))
                .expectNoEvents();
    }

    private ProfileEvent.ProfileCreatedEvent createdProfile() {
        return new ProfileEvent.ProfileCreatedEvent("user-1", "user@joutio.dev", "Alice", NOW);
    }
}
