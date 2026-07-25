package com.athletecore.api.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class AccessDeniedException extends RuntimeException {

    public AccessDeniedException(String message) {
        super(message);
    }

    public AccessDeniedException() {
        super("Access denied. You do not have permission to perform this action.");
    }
}
