package com.tinyurl.backend.keygen;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.keygen.seed-on-startup", havingValue = "true", matchIfMissing = true)
class KeySeeder implements ApplicationRunner {
    private final KeyGenerationService keys;
    private final JdbcTemplate jdbc;

    @Override
    public void run(ApplicationArguments args) {
        Long n = jdbc.queryForObject("SELECT count(*) FROM url_mappings", Long.class);
        if (n != null && n == 0) keys.generate(100_000);
    }
}
