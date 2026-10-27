package com.joutio.profile.domain.query;

import com.joutio.microservice.core.infrastructure.in.api.request.SearchRequest;

import java.util.List;

public interface PresenceQuery {

    record PresenceSearchQuery(SearchRequest request) implements PresenceQuery {
    }

    /**
     * Résolution en lot des présences (les joueurs sans ligne sont absents du résultat).
     */
    record GetPresencesByIdsQuery(List<String> userIds) implements PresenceQuery {
    }
}
