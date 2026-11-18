package com.tinyurl.backend.url;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
class KeyPoolExhaustedException extends RuntimeException {}
