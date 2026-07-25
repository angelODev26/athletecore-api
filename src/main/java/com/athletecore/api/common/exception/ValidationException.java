package com.athletecore.api.common.exception;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class ValidationException extends RuntimeException {

    private final List<String> validationErrors;

    public ValidationException(String message) {
        super(message);
        this.validationErrors = null;
    }

    public ValidationException(List<String> errors) {
        super("Validation failed");
        this.validationErrors = errors;
    }

    public ValidationException(String message, List<String> errors) {
        super(message);
        this.validationErrors = errors;
    }

    public List<String> getValidationErrors() {
        return validationErrors;
    }
}
