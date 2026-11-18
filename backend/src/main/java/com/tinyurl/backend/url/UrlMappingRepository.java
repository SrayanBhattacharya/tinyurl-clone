package com.tinyurl.backend.url;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

// url/UrlMappingRepository.java
interface UrlMappingRepository extends JpaRepository<UrlMapping, String> {

    @Query("select u.longUrl from UrlMapping u where u.shortUrl = :key and u.expiryTime > current_timestamp")
    Optional<String> findActiveLongUrl(@Param("key") String key);
}
