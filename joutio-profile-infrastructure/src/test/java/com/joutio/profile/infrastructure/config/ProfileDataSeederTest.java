package com.joutio.profile.infrastructure.config;

import com.joutio.microservice.core.domain.constant.JoutioConstants;
import com.joutio.profile.domain.port.in.CheckProfileUseCase;
import com.joutio.profile.domain.port.in.CreateProfileUseCase;
import com.joutio.profile.infrastructure.properties.AppProperties;
import org.axonframework.modelling.command.AggregateStreamCreationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ProfileDataSeederTest {

    private CheckProfileUseCase checkProfileUseCase;
    private CreateProfileUseCase createProfileUseCase;

    @BeforeEach
    void setUp() {
        checkProfileUseCase = mock(CheckProfileUseCase.class);
        createProfileUseCase = mock(CreateProfileUseCase.class);
    }

    private ProfileDataSeeder seeder(boolean seedDataEnabled) {
        AppProperties properties = new AppProperties(
                new AppProperties.SeedData(seedDataEnabled),
                new AppProperties.Activity("Europe/Paris"));
        return new ProfileDataSeeder(checkProfileUseCase, createProfileUseCase, properties);
    }

    @Test
    void runSeedsSystemProfileWhenEnabledAndAbsent() {
        ProfileDataSeeder seeder = seeder(true);
        when(checkProfileUseCase.existsById(anyString()))
                .thenReturn(CompletableFuture.completedFuture(false));
        when(createProfileUseCase.create(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture("created"));

        seeder.run();

        verify(createProfileUseCase).create(
                eq(JoutioConstants.SYSTEM_USER_ID),
                eq(JoutioConstants.SYSTEM_USER_EMAIL),
                eq(JoutioConstants.SYSTEM_USER_NAME));
    }

    @Test
    void runDoesNothingWhenDisabled() {
        ProfileDataSeeder seeder = seeder(false);

        seeder.run();

        verifyNoInteractions(createProfileUseCase, checkProfileUseCase);
    }

    @Test
    void seedProfileSkipsWhenProjectionKnowsProfile() {
        ProfileDataSeeder seeder = seeder(true);
        when(checkProfileUseCase.existsById(JoutioConstants.SYSTEM_USER_ID))
                .thenReturn(CompletableFuture.completedFuture(true));

        seeder.seedProfile(
                JoutioConstants.SYSTEM_USER_ID,
                JoutioConstants.SYSTEM_USER_EMAIL,
                JoutioConstants.SYSTEM_USER_NAME,
                "System");

        verify(createProfileUseCase, never()).create(anyString(), anyString(), anyString());
    }

    @Test
    void seedProfileIgnoresExistingAggregateWhenProjectionLagging() {
        ProfileDataSeeder seeder = seeder(true);
        when(checkProfileUseCase.existsById(JoutioConstants.SYSTEM_USER_ID))
                .thenReturn(CompletableFuture.completedFuture(false));
        when(createProfileUseCase.create(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.failedFuture(
                        new AggregateStreamCreationException(JoutioConstants.SYSTEM_USER_ID)));

        assertThatCode(() -> seeder.seedProfile(
                JoutioConstants.SYSTEM_USER_ID,
                JoutioConstants.SYSTEM_USER_EMAIL,
                JoutioConstants.SYSTEM_USER_NAME,
                "System")).doesNotThrowAnyException();
    }

    @Test
    void seedProfileCreatesWhenAbsent() {
        ProfileDataSeeder seeder = seeder(true);
        when(checkProfileUseCase.existsById(JoutioConstants.SYSTEM_USER_ID))
                .thenReturn(CompletableFuture.completedFuture(false));
        when(createProfileUseCase.create(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture("created"));

        seeder.seedProfile(
                JoutioConstants.SYSTEM_USER_ID,
                JoutioConstants.SYSTEM_USER_EMAIL,
                JoutioConstants.SYSTEM_USER_NAME,
                "System");

        verify(createProfileUseCase).create(
                eq(JoutioConstants.SYSTEM_USER_ID),
                eq(JoutioConstants.SYSTEM_USER_EMAIL),
                eq(JoutioConstants.SYSTEM_USER_NAME));
    }
}
