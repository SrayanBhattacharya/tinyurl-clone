package com.tinyurl.backend.url;

import com.tinyurl.backend.common.ApiException;
import org.springframework.http.HttpStatus;

class UrlNotFoundException extends ApiException {
    UrlNotFoundException() {
        super(HttpStatus.NOT_FOUND, "Short URL not found or expired");
    }
}
