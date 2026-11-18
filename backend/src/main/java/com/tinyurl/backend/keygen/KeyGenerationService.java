package com.tinyurl.backend.keygen;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class KeyGenerationService {
    private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final int KEY_LENGTH = 8;

    private final JdbcTemplate jdbc;
    private final SecureRandom random = new SecureRandom();

    public void generate(int count) {
        List<String> keys = new ArrayList<>(count);
        for (int i = 0; i < count; i++) keys.add(randomKey());

        jdbc.batchUpdate(
                "INSERT INTO url_mappings (short_url) VALUES (?) ON CONFLICT DO NOTHING",
                keys, 1000, (ps, key) -> ps.setString(1, key));
    }

    private String randomKey() {
        StringBuilder sb = new StringBuilder(KEY_LENGTH);
        for (int i = 0; i < KEY_LENGTH; i++)
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        return sb.toString();
    }
}
