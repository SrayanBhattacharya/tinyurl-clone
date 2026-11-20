package com.tinyurl.backend.url;

import com.tinyurl.backend.support.ContainersConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.keygen.seed-on-startup=false")
@AutoConfigureMockMvc
@Import(ContainersConfig.class)
class RedisDownFallbackTest {

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    @MockitoBean
    StringRedisTemplate redis;

    @BeforeEach
    void reset() {
        jdbc.update("DELETE FROM url_mappings");
        jdbc.update("INSERT INTO url_mappings (short_url) VALUES ('dkey0000')");
        when(redis.opsForValue()).thenThrow(new RedisConnectionFailureException("redis down"));
    }

    @Test
    void redirectStillWorksWhenRedisFails() throws Exception {
        mvc.perform(post("/api/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"longUrl\":\"https://example.com/d\",\"creatorId\":1}"))
                .andExpect(status().isCreated());

        mvc.perform(get("/dkey0000"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.com/d"));
    }
}
