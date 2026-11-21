package com.tinyurl.backend.url;

import com.tinyurl.backend.support.ContainersConfig;
import com.tinyurl.backend.url.dto.UrlTarget;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = "app.keygen.seed-on-startup=false")
@Import(ContainersConfig.class)
class ReplicaFallbackTest {

    @Autowired UrlService service;
    @Autowired JdbcTemplate jdbc;
    @Autowired StringRedisTemplate redis;

    @MockitoBean
    UrlReplicaReader replica;

    @BeforeEach
    void reset() {
        jdbc.update("DELETE FROM url_mappings");
        jdbc.update("INSERT INTO url_mappings (short_url) VALUES ('rkey0000')");
        try (var conn = redis.getConnectionFactory().getConnection()) {
            conn.serverCommands().flushAll();
        }
    }

    @Test
    void replicaMissFallsBackToLeader() {
        String key = service.create("https://example.com/leader", 1L);
        when(replica.findActiveTarget(key)).thenReturn(Optional.empty());   // lag: replica hasn't got it yet

        assertThat(service.resolve(key)).isEqualTo("https://example.com/leader");
        verify(replica).findActiveTarget(key);
    }

    @Test
    void replicaFailureFallsBackToLeader() {
        String key = service.create("https://example.com/leader", 1L);
        when(replica.findActiveTarget(key)).thenThrow(new RuntimeException("replica down"));

        assertThat(service.resolve(key)).isEqualTo("https://example.com/leader");
    }

    @Test
    void replicaHitIsUsedWhenLeaderHasNothing() {
        // "nokey000" does not exist on the leader, so only the replica can answer
        when(replica.findActiveTarget("nokey000")).thenReturn(Optional.of(
                new UrlTarget("https://from-replica.example", Instant.now().plusSeconds(3600))));

        assertThat(service.resolve("nokey000")).isEqualTo("https://from-replica.example");
    }

    @Test
    void notFoundWhenNeitherHasIt() {
        assertThatThrownBy(() -> service.resolve("nokey000"))
                .isInstanceOf(UrlNotFoundException.class);
    }
}
