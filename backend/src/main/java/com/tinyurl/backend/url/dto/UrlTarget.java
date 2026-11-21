package com.tinyurl.backend.url.dto;

import java.time.Instant;

public record UrlTarget(String longUrl, Instant expiryTime) {}
