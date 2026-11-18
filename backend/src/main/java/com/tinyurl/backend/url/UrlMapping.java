package com.tinyurl.backend.url;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;


import java.time.Instant;

@Entity
@Table(name = "url_mappings")
@Getter
@NoArgsConstructor
public class UrlMapping {
    @Id
    private String shortUrl;
    private String longUrl;
    private Long creatorId;
    private Instant expiryTime;
}
