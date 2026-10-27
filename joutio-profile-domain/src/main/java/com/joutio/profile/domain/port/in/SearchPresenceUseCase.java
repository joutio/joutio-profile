package com.joutio.profile.domain.port.in;

import com.joutio.microservice.core.infrastructure.in.api.request.SearchRequest;
import com.joutio.microservice.core.infrastructure.in.api.response.SearchResponse;
import com.joutio.profile.domain.model.PlayerPresence;
import com.joutio.profile.domain.query.PresenceQuery;

import java.util.concurrent.CompletableFuture;

/**
 * Port entrant — recherche paginée de présences (filtres/sorts/pagination standards, ex.
 * {@code userId IN [...]}).
 */
public interface SearchPresenceUseCase {

    CompletableFuture<SearchResponse<PlayerPresence>> search(PresenceQuery.PresenceSearchQuery query);

    default CompletableFuture<SearchResponse<PlayerPresence>> search(SearchRequest request) {
        return search(new PresenceQuery.PresenceSearchQuery(request));
    }
}
