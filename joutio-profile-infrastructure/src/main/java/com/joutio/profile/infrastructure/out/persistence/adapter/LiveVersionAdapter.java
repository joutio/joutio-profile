package com.joutio.profile.infrastructure.out.persistence.adapter;

import com.joutio.profile.domain.port.out.LiveVersionPort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Version des vues surveillées d'un joueur ({@code live_version}) : un upsert atomique, qui verrouille la ligne jusqu'à
 * la fin de la transaction. Deux écritures concurrentes de la même vue obtiennent donc deux versions successives, et la
 * seconde lit l'état laissé par la première.
 */
@Component
public class LiveVersionAdapter implements LiveVersionPort {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public long increment(String view, String userId) {
        Number version = (Number) entityManager.createNativeQuery("""
                        INSERT INTO live_version (view, user_id, version) VALUES (:view, :userId, 1)
                        ON CONFLICT (view, user_id) DO UPDATE SET version = live_version.version + 1
                        RETURNING version""")
                .setParameter("view", view)
                .setParameter("userId", userId)
                .getSingleResult();
        return version.longValue();
    }
}
