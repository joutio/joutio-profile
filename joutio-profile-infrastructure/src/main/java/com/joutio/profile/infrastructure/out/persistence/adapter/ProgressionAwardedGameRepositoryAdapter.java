package com.joutio.profile.infrastructure.out.persistence.adapter;

import com.joutio.profile.domain.model.AwardedGame;
import com.joutio.profile.domain.model.GameXp;
import com.joutio.profile.domain.port.out.ProgressionAwardedGameRepositoryPort;
import com.joutio.profile.infrastructure.out.persistence.entity.ProgressionAwardedGameEntity;
import com.joutio.profile.infrastructure.out.persistence.repository.ProgressionAwardedGameJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class ProgressionAwardedGameRepositoryAdapter implements ProgressionAwardedGameRepositoryPort {

    private final ProgressionAwardedGameJpaRepository repository;

    public ProgressionAwardedGameRepositoryAdapter(ProgressionAwardedGameJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public boolean record(AwardedGame awarded) {
        ProgressionAwardedGameEntity.AwardedGameId id =
                new ProgressionAwardedGameEntity.AwardedGameId(awarded.userId(), awarded.gameId());
        if (repository.existsById(id)) {
            return false;
        }
        repository.save(new ProgressionAwardedGameEntity(awarded.userId(), awarded.gameId(), awarded.topicId(),
                awarded.xp(), awarded.gameScore(), awarded.botGame(), awarded.rating(), awarded.ratingDelta(), awarded.rated()));
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AwardedGame> findByGame(String gameId) {
        return repository.findByGameId(gameId).stream()
                .map(entity -> new AwardedGame(entity.getUserId(), entity.getGameId(), entity.getTopicId(),
                        entity.getXp() == null ? 0 : entity.getXp(), entity.getGameScore(), entity.isBotGame(),
                        entity.getRating(), entity.getRatingDelta(), entity.isRated()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<GameXp> findGameXp(String userId, List<String> gameIds) {
        if (gameIds.isEmpty()) {
            return List.of();
        }
        return repository.findByUserIdAndGameIdIn(userId, gameIds).stream()
                .map(entity -> new GameXp(entity.getGameId(), entity.getXp(),
                        entity.getRatingDelta(), entity.isRated()))
                .toList();
    }
}
