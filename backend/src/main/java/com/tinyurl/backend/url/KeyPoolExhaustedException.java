package com.tinyurl.backend.url;

import com.tinyurl.backend.common.ApiException;
import org.springframework.http.HttpStatus;

class KeyPoolExhaustedException extends ApiException {
    KeyPoolExhaustedException() {
        super(HttpStatus.SERVICE_UNAVAILABLE, "No short keys available, try again later");
    }
}
