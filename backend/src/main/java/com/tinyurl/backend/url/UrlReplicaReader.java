package com.tinyurl.backend.url;

import com.tinyurl.backend.url.dto.UrlTarget;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
class UrlReplicaReader {
    private static final String SQL = """
        SELECT long_url, expiry_time FROM url_mappings
        WHERE short_url = ? AND expiry_time > NOW()
        """;

    private final HikariDataSource ds;   // null when disabled
    private final JdbcTemplate jdbc;

    UrlReplicaReader(@Value("${app.replica.url:}") String url,
                     @Value("${app.replica.username:}") String user,
                     @Value("${app.replica.password:}") String password) {
        if (url.isBlank()) { ds = null; jdbc = null; return; }
        HikariConfig cfg = new HikariConfig();
        cfg.setPoolName("replica-pool");
        cfg.setJdbcUrl(url);
        cfg.setUsername(user);
        cfg.setPassword(password);
        cfg.setReadOnly(true);
        cfg.setMaximumPoolSize(10);
        cfg.setConnectionTimeout(1000);          // fail fast if the replica is down
        cfg.setInitializationFailTimeout(-1);    // app still starts if the replica is down
        ds = new HikariDataSource(cfg);
        jdbc = new JdbcTemplate(ds);
    }

    Optional<UrlTarget> findActiveTarget(String key) {
        if (jdbc == null) return Optional.empty();
        return jdbc.query(SQL,
                        (rs, i) -> new UrlTarget(rs.getString(1), rs.getTimestamp(2).toInstant()), key)
                .stream().findFirst();
    }

    @PreDestroy
    void close() { if (ds != null) ds.close(); }
}
