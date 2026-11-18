package com.tinyurl.backend.url;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
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

    String create(String longUrl, Long creatorId) {
        List<String> keys = jdbc.queryForList(CLAIM_SQL, String.class, creatorId, longUrl);
        if (keys.isEmpty()) throw new KeyPoolExhaustedException();
        return keys.get(0);
    }
}
