package com.tinyurl.backend.url;

import com.tinyurl.backend.url.dto.UrlTarget;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
class UrlService {
    private static final String CLAIM_SQL = """
        WITH candidate AS (
            SELECT short_url FROM url_mappings
            WHERE expiry_time < NOW()
            LIMIT 1
            FOR UPDATE SKIP LOCKED
        )
        UPDATE url_mappings u
        SET expiry_time = NOW() + INTERVAL '1 year',
            creator_id = ?,
            long_url = ?
        FROM candidate c
        WHERE u.short_url = c.short_url
        RETURNING u.short_url
        """;

    private final JdbcTemplate jdbc;
    private final UrlMappingRepository repo;
    private final UrlCache cache;
    private final UrlReplicaReader replica;

    String create(String longUrl, Long creatorId) {
        List<String> keys = jdbc.queryForList(CLAIM_SQL, String.class, creatorId, longUrl);
        if (keys.isEmpty()) throw new KeyPoolExhaustedException();
        return keys.get(0);
    }

    String resolve(String key) {
        return cache.get(key).orElseGet(() -> {
            UrlTarget t = lookup(key).orElseThrow(UrlNotFoundException::new);
            cache.put(key, t.longUrl(), t.expiryTime());
            return t.longUrl();
        });
    }

    private Optional<UrlTarget> lookup(String key) {
        try {
            Optional<UrlTarget> fromReplica = replica.findActiveTarget(key);
            if (fromReplica.isPresent()) return fromReplica;
        } catch (RuntimeException e) {
            log.warn("Replica read failed, using leader: {}", e.toString());
        }
        return repo.findActiveTarget(key);   // replica miss or failure -> leader
    }
}
