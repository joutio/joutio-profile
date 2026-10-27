package com.joutio.profile.application.service;

import com.joutio.microservice.core.infrastructure.axon.QueryResponseTypes;
import com.joutio.profile.domain.model.PlayerProgress;
import com.joutio.profile.domain.model.TopicProgress;
import com.joutio.profile.domain.port.in.GetProgressionUseCase;
import com.joutio.profile.domain.query.ProgressionQuery;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service applicatif — implémente {@link GetProgressionUseCase} via le
 * {@link QueryGateway} (même pattern que ProfileQueryService).
 */
@Service
public class ProgressionQueryService implements GetProgressionUseCase {

    private final QueryGateway queryGateway;

    public ProgressionQueryService(QueryGateway queryGateway) {
        this.queryGateway = queryGateway;
    }

    @Override
    public CompletableFuture<PlayerProgress> getById(ProgressionQuery.GetProgressionQuery query) {
        return queryGateway.query(query, QueryResponseTypes.instanceOf(PlayerProgress.class));
    }

    @Override
    public CompletableFuture<TopicProgress> getTopic(ProgressionQuery.GetTopicProgressionQuery query) {
        return queryGateway.query(query, QueryResponseTypes.instanceOf(TopicProgress.class));
    }

    @Override
    public CompletableFuture<List<TopicProgress>> getTopics(ProgressionQuery.GetTopicProgressionsQuery query) {
        return queryGateway.query(query, QueryResponseTypes.multipleInstancesOf(TopicProgress.class));
    }
}
