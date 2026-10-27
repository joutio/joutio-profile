package com.joutio.profile.application.service;

import com.joutio.microservice.core.infrastructure.axon.QueryResponseTypes;
import com.joutio.microservice.core.infrastructure.in.api.response.SearchResponse;
import com.joutio.profile.domain.model.PlayerPresence;
import com.joutio.profile.domain.port.in.SearchPresenceUseCase;
import com.joutio.profile.domain.query.PresenceQuery;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Service applicatif — recherche paginée de présences via le bus de queries Axon.
 */
@Service
public class PresenceQueryService implements SearchPresenceUseCase {

    private final QueryGateway queryGateway;

    public PresenceQueryService(QueryGateway queryGateway) {
        this.queryGateway = queryGateway;
    }

    @Override
    public CompletableFuture<SearchResponse<PlayerPresence>> search(PresenceQuery.PresenceSearchQuery query) {
        return queryGateway.query(query, QueryResponseTypes.searchResponseOf(PlayerPresence.class));
    }
}
