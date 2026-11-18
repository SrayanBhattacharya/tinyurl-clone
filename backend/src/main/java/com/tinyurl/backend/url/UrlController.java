package com.tinyurl.backend.url;

import com.tinyurl.backend.url.dto.CreateUrlRequest;
import com.tinyurl.backend.url.dto.CreateUrlResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

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

    @GetMapping("/{key:[a-z0-9]{8}}")
    ResponseEntity<Void> redirect(@PathVariable String key) {
        String longUrl = service.resolve(key);
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(longUrl)).build();
    }
}
