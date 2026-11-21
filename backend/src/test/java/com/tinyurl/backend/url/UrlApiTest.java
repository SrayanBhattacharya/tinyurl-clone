package com.tinyurl.backend.url;

import com.jayway.jsonpath.JsonPath;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@SpringBootTest(properties = "app.keygen.seed-on-startup=false")
@AutoConfigureMockMvc
@Import(ContainersConfig.class)
class UrlApiTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    StringRedisTemplate redis;

    @BeforeEach
    void reset() {
        jdbc.update("DELETE FROM url_mappings");
        for (int i = 0; i < 5; i++) {
            jdbc.update("INSERT INTO url_mappings (short_url) VALUES (?)", "tkey000" + i);
        }
        try (var conn = redis.getConnectionFactory().getConnection()) {
            conn.serverCommands().flushAll();
        }
    }

    private String create(String longUrl) throws Exception {
        String body = mvc.perform(post("/api/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"longUrl\":\"" + longUrl + "\",\"creatorId\":1}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String shortUrl = JsonPath.read(body, "$.shortUrl");
        return shortUrl.substring(shortUrl.lastIndexOf('/') + 1);
    }

    @Test
    void createThenRedirect() throws Exception {
        String key = create("https://example.com/a");
        mvc.perform(get("/" + key))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.com/a"));
    }

    @Test
    void unknownKeyReturns404InErrorShape() throws Exception {
        mvc.perform(get("/zzzzzzzz"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void unclaimedKeyReturns404() throws Exception {
        mvc.perform(get("/tkey0000")).andExpect(status().isNotFound());
    }

    @Test
    void expiredKeyReturns404() throws Exception {
        String key = create("https://example.com/b");
        jdbc.update("UPDATE url_mappings SET expiry_time = NOW() - INTERVAL '1 second' WHERE short_url = ?", key);
        mvc.perform(get("/" + key)).andExpect(status().isNotFound());
    }

    @Test
    void invalidUrlReturns400NamingTheField() throws Exception {
        String body = mvc.perform(post("/api/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"longUrl\":\"not-a-url\"}"))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();

        assertThat(JsonPath.<String>read(body, "$.message")).contains("longUrl");
    }

    @Test
    void malformedJsonReturns400() throws Exception {
        mvc.perform(post("/api/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{bad"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}
