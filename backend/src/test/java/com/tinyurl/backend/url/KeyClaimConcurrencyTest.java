package com.tinyurl.backend.url;

import com.tinyurl.backend.support.ContainersConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@SpringBootTest(properties = "app.keygen.seed-on-startup=false")
@Import(ContainersConfig.class)
class KeyClaimConcurrencyTest {

    @Autowired
    UrlService service;
    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void reset() {
        jdbc.update("DELETE FROM url_mappings");
    }

    @Test
    void parallelCreatesNeverShareAKey() throws Exception {
        for (int i = 0; i < 200; i++) {
            jdbc.update("INSERT INTO url_mappings (short_url) VALUES (?)", String.format("key%05d", i));
        }

        int n = 100;
        ExecutorService pool = Executors.newFixedThreadPool(20);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<String>> results = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            int id = i;
            results.add(pool.submit(() -> {
                start.await();
                return service.create("https://example.com/" + id, 1L);
            }));
        }
        start.countDown();

        Set<String> keys = new HashSet<>();
        for (Future<String> f : results) keys.add(f.get(30, TimeUnit.SECONDS));
        pool.shutdown();

        assertThat(keys).hasSize(n);
        Integer claimed = jdbc.queryForObject(
                "SELECT count(*) FROM url_mappings WHERE long_url IS NOT NULL", Integer.class);
        assertThat(claimed).isEqualTo(n);
    }

    @Test
    void throwsWhenPoolIsExhausted() {
        for (int i = 0; i < 3; i++) {
            jdbc.update("INSERT INTO url_mappings (short_url) VALUES (?)", "only000" + i);
        }
        for (int i = 0; i < 3; i++) service.create("https://example.com/" + i, 1L);

        assertThatThrownBy(() -> service.create("https://example.com/x", 1L))
                .isInstanceOf(KeyPoolExhaustedException.class);
    }
}
