package com.joutio.profile.infrastructure.out.persistence.adapter;

import com.joutio.profile.domain.port.out.GameRewardVersionPort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Version de la récompense d'une partie ({@code game_reward_version}) : un upsert atomique, qui verrouille la ligne
 * jusqu'à la fin de la transaction. Deux écritures concurrentes de la même partie obtiennent donc deux versions
 * successives, et la seconde lit l'état laissé par la première.
 */
@Component
public class GameRewardVersionAdapter implements GameRewardVersionPort {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public long increment(String gameId) {
        Number version = (Number) entityManager.createNativeQuery("""
                        INSERT INTO game_reward_version (game_id, version) VALUES (:gameId, 1)
                        ON CONFLICT (game_id) DO UPDATE SET version = game_reward_version.version + 1
                        RETURNING version""")
                .setParameter("gameId", gameId)
                .getSingleResult();
        return version.longValue();
    }

    @Override
    @Transactional(readOnly = true)
    public long current(String gameId) {
        List<?> rows = entityManager
                .createNativeQuery("SELECT version FROM game_reward_version WHERE game_id = :gameId")
                .setParameter("gameId", gameId)
                .getResultList();
        return rows.isEmpty() ? 0 : ((Number) rows.getFirst()).longValue();
    }
}
