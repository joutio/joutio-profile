package com.joutio.profile.domain.port.in;

import com.joutio.microservice.core.infrastructure.in.api.request.SearchRequest;
import com.joutio.microservice.core.infrastructure.in.api.response.SearchResponse;
import com.joutio.profile.domain.model.Profile;
import com.joutio.profile.domain.query.ProfileQuery;

import java.util.concurrent.CompletableFuture;

/**
 * Port entrant - Cas d'utilisation : recherche paginée de profils
 * (par pseudonyme ou email, cf. @Searchable sur la projection).
 */
public interface SearchProfileUseCase {

    CompletableFuture<SearchResponse<Profile>> search(ProfileQuery.ProfileSearchQuery query);

    default CompletableFuture<SearchResponse<Profile>> search(SearchRequest request) {
        return search(new ProfileQuery.ProfileSearchQuery(request));
    }
}
