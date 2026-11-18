package com.tinyurl.backend.url;

import com.tinyurl.backend.url.dto.CreateUrlRequest;
import com.tinyurl.backend.url.dto.CreateUrlResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/urls")
@RequiredArgsConstructor
class UrlController {
    private final UrlService service;

    @Value("${app.base-url}")
    private String baseUrl;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    CreateUrlResponse create(@Valid @RequestBody CreateUrlRequest req) {
        String key = service.create(req.longUrl(), req.creatorId());
        return new CreateUrlResponse(baseUrl + "/" + key);
    }
}
