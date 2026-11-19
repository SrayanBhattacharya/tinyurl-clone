package com.tinyurl.backend.url;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

interface UrlMappingRepository extends JpaRepository<UrlMapping, String> {

    interface UrlTarget {
        String getLongUrl();
        Instant getExpiryTime();
    }

    @Query("""
        select u.longUrl as longUrl, u.expiryTime as expiryTime
        from UrlMapping u
        where u.shortUrl = :key and u.expiryTime > current_timestamp
        """)
    Optional<UrlTarget> findActiveTarget(@Param("key") String key);
}
