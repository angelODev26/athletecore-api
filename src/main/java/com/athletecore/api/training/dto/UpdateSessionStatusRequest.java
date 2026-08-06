package com.athletecore.api.training.dto;

import com.athletecore.api.training.SessionStatus;

import jakarta.validation.constraints.NotNull;

/**
 * DTO de request para cambiar el estado de una sesión.
 */
public record UpdateSessionStatusRequest(
        @NotNull(message = "El estado de la sesión es obligatorio")
        SessionStatus status
) {
}