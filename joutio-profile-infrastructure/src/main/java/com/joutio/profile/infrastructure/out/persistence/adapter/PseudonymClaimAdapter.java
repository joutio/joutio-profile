package com.joutio.profile.infrastructure.out.persistence.adapter;

import com.joutio.profile.domain.port.out.PseudonymClaimPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

/**
 * Réservation des pseudonymes. {@code INSERT … ON CONFLICT DO NOTHING} : un conflit ne lève pas
 * d'erreur SQL, donc n'interrompt pas la transaction de l'unité de travail Axon (la réservation est
 * annulée avec elle si la commande échoue ensuite).
 */
@Component
public class PseudonymClaimAdapter implements PseudonymClaimPort {

    private final JdbcTemplate jdbcTemplate;

    public PseudonymClaimAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean claim(String pseudonymKey, String userId) {
        if (pseudonymKey == null || pseudonymKey.isEmpty()) {
            return false;
        }
        jdbcTemplate.update(
                "INSERT INTO pseudonym_claim (pseudonym_key, user_id, created_at) VALUES (?, ?, ?) "
                        + "ON CONFLICT (pseudonym_key) DO NOTHING",
                pseudonymKey, userId, Timestamp.from(Instant.now()));
        return userId.equals(owner(pseudonymKey));
    }

    @Override
    public void release(String pseudonymKey, String userId) {
        if (pseudonymKey == null) {
            return;
        }
        jdbcTemplate.update("DELETE FROM pseudonym_claim WHERE pseudonym_key = ? AND user_id = ?",
                pseudonymKey, userId);
    }

    @Override
    public boolean available(String pseudonymKey, String userId) {
        if (pseudonymKey == null || pseudonymKey.isEmpty()) {
            return false;
        }
        String owner = owner(pseudonymKey);
        return owner == null || owner.equals(userId);
    }

    private String owner(String pseudonymKey) {
        List<String> owners = jdbcTemplate.queryForList(
                "SELECT user_id FROM pseudonym_claim WHERE pseudonym_key = ?", String.class, pseudonymKey);
        return owners.isEmpty() ? null : owners.getFirst();
    }
}
