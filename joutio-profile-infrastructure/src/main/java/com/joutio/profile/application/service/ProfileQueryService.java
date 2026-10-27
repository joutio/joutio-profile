package com.joutio.profile.application.service;

import com.joutio.microservice.core.infrastructure.axon.QueryResponseTypes;
import com.joutio.microservice.core.infrastructure.in.api.response.SearchResponse;
import com.joutio.profile.domain.exception.ProfileProblems;
import com.joutio.profile.domain.model.Profile;
import com.joutio.profile.domain.port.in.CheckProfileUseCase;
import com.joutio.profile.domain.port.in.GetProfileUseCase;
import com.joutio.profile.domain.port.in.SearchProfileUseCase;
import com.joutio.profile.domain.query.ProfileQuery;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Service applicatif - Implémente GetProfileUseCase, SearchProfileUseCase et CheckProfileUseCase.
 * Cette classe est autorisée à connaitre QueryGateway (couche application).
 */
@Service
public class ProfileQueryService implements GetProfileUseCase, SearchProfileUseCase, CheckProfileUseCase {

    private final QueryGateway queryGateway;

    public ProfileQueryService(QueryGateway queryGateway) {
        this.queryGateway = queryGateway;
    }

    @Override
    public CompletableFuture<Profile> getById(ProfileQuery.GetProfileQuery query) throws ProfileProblems.ProfileNotFoundProblem {
        return queryGateway.query(query, QueryResponseTypes.instanceOf(Profile.class));
    }

    @Override
    public CompletableFuture<SearchResponse<Profile>> search(ProfileQuery.ProfileSearchQuery query) {
        return queryGateway.query(query, QueryResponseTypes.searchResponseOf(Profile.class));
    }

    @Override
    public CompletableFuture<Boolean> existsById(ProfileQuery.ProfileExistsByIdQuery query) {
        return queryGateway.query(query, QueryResponseTypes.instanceOf(Boolean.class));
    }
}
