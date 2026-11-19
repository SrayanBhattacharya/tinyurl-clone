package com.tinyurl.backend.url;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
class UrlCache {
    private static final String PREFIX = "url:";

    @Value("${app.cache.max-ttl}")
    private Duration maxTtl;

    private final StringRedisTemplate redis;

    Optional<String> get(String key) {
        try {
            return Optional.ofNullable(redis.opsForValue().get(PREFIX + key));
        } catch (RuntimeException e) {
            log.warn("Redis read failed, falling back to DB: {}", e.toString());
            return Optional.empty();
        }
    }

    void put(String key, String longUrl, Instant expiry) {
        Duration ttl = Duration.between(Instant.now(), expiry);
        if (ttl.compareTo(maxTtl) > 0) ttl = maxTtl;
        if (ttl.isZero() || ttl.isNegative()) return;
        try {
            redis.opsForValue().set(PREFIX + key, longUrl, ttl);
        } catch (RuntimeException e) {
            log.warn("Redis write failed: {}", e.toString());
        }
    }
}
