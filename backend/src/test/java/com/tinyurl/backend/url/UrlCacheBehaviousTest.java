package com.tinyurl.backend.url;

import com.tinyurl.backend.support.ContainersConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.keygen.seed-on-startup=false")
@AutoConfigureMockMvc
@Import(ContainersConfig.class)
class UrlCacheBehaviorTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    StringRedisTemplate redis;

    @BeforeEach
    void reset() {
        jdbc.update("DELETE FROM url_mappings");
        jdbc.update("INSERT INTO url_mappings (short_url) VALUES ('ckey0000')");
        try (var conn = redis.getConnectionFactory().getConnection()) {
            conn.serverCommands().flushAll();
        }
    }

    private void createAndGetKey() throws Exception {
        mvc.perform(post("/api/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"longUrl\":\"https://example.com/c\",\"creatorId\":1}"))
                .andExpect(status().isCreated());
    }

    @Test
    void missPopulatesCacheWithBoundedTtl() throws Exception {
        createAndGetKey();
        assertThat(redis.hasKey("url:ckey0000")).isFalse();   // look-aside: not cached on create

        mvc.perform(get("/ckey0000")).andExpect(status().isFound());

        assertThat(redis.opsForValue().get("url:ckey0000")).isEqualTo("https://example.com/c");
        assertThat(redis.getExpire("url:ckey0000")).isBetween(1L, 3600L);   // assumes app.cache.max-ttl=1h
    }

    @Test
    void hitIsServedWithoutTouchingTheDatabase() throws Exception {
        createAndGetKey();
        mvc.perform(get("/ckey0000")).andExpect(status().isFound());   // miss, populates cache

        jdbc.update("UPDATE url_mappings SET long_url = 'https://changed.com' WHERE short_url = 'ckey0000'");

        mvc.perform(get("/ckey0000"))
                .andExpect(header().string("Location", "https://example.com/c"));   // still the cached value
    }
}
