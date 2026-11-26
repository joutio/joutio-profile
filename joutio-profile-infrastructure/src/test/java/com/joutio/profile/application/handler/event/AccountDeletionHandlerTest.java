package com.joutio.profile.application.handler.event;

import com.joutio.profile.application.service.PlayerLiveViews;
import com.joutio.axon.autoconfigure.erasure.AxonEventStoreErasureService;
import com.joutio.identity.domain.event.UserEvent.UserAccountDeletedEvent;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Suppression du compte sur le vrai schéma du profil : chaque table qui porte un {@code user_id} est purgée, et une
 * table absente du schéma fait échouer le test (elle annulait toute la suppression, 2026-11-19).
 */
@Testcontainers
class AccountDeletionHandlerTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    private static final Pattern DELETE = Pattern.compile("DELETE FROM (\\w+) WHERE user_id = \\?");

    private static DataSource dataSource;

    @BeforeAll
    static void schema() {
        dataSource = new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration/profile").load().migrate();
    }

    @Test
    void purgesEveryUserTableOfTheProfileSchema() {
        Set<String> purged = new TreeSet<>();
        JdbcTemplate recording = new JdbcTemplate(dataSource) {
            @Override
            public int update(String sql, Object... args) {
                Matcher table = DELETE.matcher(sql);
                if (table.matches()) purged.add(table.group(1));
                return super.update(sql, args);
            }
        };
        AxonEventStoreErasureService erasure = mock(AxonEventStoreErasureService.class);
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.update("""
                INSERT INTO profile_entry (user_id, email, pseudonym, created_at, updated_at)
                VALUES ('u1', 'u1@example.com', 'Nadia', now(), now())""");
        jdbc.update("INSERT INTO badge_unlock (user_id, badge, earned_at) VALUES ('u1', 'FIRST_WIN', now())");

        new AccountDeletionHandler(recording, erasure, mock(PlayerLiveViews.class)).on(new UserAccountDeletedEvent("u1", "d1", Instant.now()));

        List<String> userTables = jdbc.queryForList("""
                SELECT table_name FROM information_schema.columns
                WHERE table_schema = 'public' AND column_name = 'user_id'""", String.class);
        assertThat(purged).containsExactlyInAnyOrderElementsOf(userTables);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM profile_entry", Long.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM badge_unlock", Long.class)).isZero();
        verify(erasure).eraseAggregates(List.of("u1", "progress:u1", "badges:u1", "rewards:u1"));
    }
}
