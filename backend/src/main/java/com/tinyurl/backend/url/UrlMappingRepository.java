package com.tinyurl.backend.url;

import com.tinyurl.backend.url.dto.UrlTarget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

interface UrlMappingRepository extends JpaRepository<UrlMapping, String> {
    @Query("""
    select new com.tinyurl.backend.url.dto.UrlTarget(u.longUrl, u.expiryTime)
    from UrlMapping u
    where u.shortUrl = :key and u.expiryTime > current_timestamp
    """)
    Optional<UrlTarget> findActiveTarget(@Param("key") String key);
}
