package com.joutio.profile.application.handler.query;

import com.joutio.profile.domain.model.ProfileRules;
import com.joutio.profile.domain.model.PseudonymAvailability;
import com.joutio.profile.domain.port.out.PseudonymClaimPort;
import com.joutio.microservice.core.infrastructure.in.api.response.SearchResponse;
import com.joutio.profile.domain.exception.ProfileProblems;
import com.joutio.profile.domain.model.ImageReferenceCount;
import com.joutio.profile.domain.model.Profile;
import com.joutio.profile.domain.port.out.ProfileRepositoryPort;
import com.joutio.profile.domain.query.ProfileQuery;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Handler Axon — Point d'entrée des queries sur le bus Axon.
 * Délègue aux ports sortants. Ne contient aucune logique propre.
 * ProfileQueryService fait la même chose pour les appels locaux (intra-service).
 */
@Component
public class ProfileQueryHandler {

    private final ProfileRepositoryPort profileRepositoryPort;
    private final PseudonymClaimPort pseudonymClaimPort;

    public ProfileQueryHandler(ProfileRepositoryPort profileRepositoryPort,
                               PseudonymClaimPort pseudonymClaimPort) {
        this.profileRepositoryPort = profileRepositoryPort;
        this.pseudonymClaimPort = pseudonymClaimPort;
    }

    @QueryHandler
    public PseudonymAvailability handle(ProfileQuery.CheckPseudonymAvailabilityQuery query) {
        String pseudonym = query.pseudonym();
        if (!ProfileRules.isValidPseudonym(pseudonym)) {
            return new PseudonymAvailability(pseudonym, false, false);
        }
        return new PseudonymAvailability(pseudonym, true,
                pseudonymClaimPort.available(ProfileRules.pseudonymKey(pseudonym), query.userId()));
    }

    @QueryHandler
    public Profile handle(ProfileQuery.GetProfileQuery query) {
        return profileRepositoryPort.findById(query.userId())
                .orElseThrow(() -> new ProfileProblems.ProfileNotFoundProblem(query.userId()));
    }

    @QueryHandler
    public boolean handle(ProfileQuery.ProfileExistsByIdQuery query) {
        return profileRepositoryPort.existsById(query.userId());
    }

    @QueryHandler
    public List<Profile> handle(ProfileQuery.GetProfilesByIdsQuery query) {
        return profileRepositoryPort.findByIds(query.userIds());
    }

    @QueryHandler
    public SearchResponse<Profile> handle(ProfileQuery.ProfileSearchQuery query) {
        return profileRepositoryPort.findAll(query.request());
    }

    @QueryHandler
    public List<Profile> handle(ProfileQuery.GetProfileSuggestionsQuery query) {
        return profileRepositoryPort.findSuggestions(query.nameQuery(), query.limit());
    }

    @QueryHandler
    public List<ImageReferenceCount> handle(ProfileQuery.CountImageReferencesQuery query) {
        return profileRepositoryPort.countReferencesByBannerUrls(query.imageUrls());
    }
}
