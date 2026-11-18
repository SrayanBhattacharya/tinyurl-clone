package com.tinyurl.backend.url.dto;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.URL;

public record CreateUrlRequest(
        @NotBlank
        @URL String longUrl,
        Long creatorId
) {
}
