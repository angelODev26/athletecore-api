package com.athletecore.api.common.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private final LocalDateTime timestamp;

    private final int status;

    private final String error;

    private final String message;

    private final String details;

    public static ErrorResponseBuilder builder() {
        return ErrorResponse.builder()
                .timestamp(LocalDateTime.now());
    }
}
