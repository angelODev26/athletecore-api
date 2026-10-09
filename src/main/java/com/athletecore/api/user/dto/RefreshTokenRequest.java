package com.athletecore.api.user.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO de solicitud de renovación de tokens.
 * Contiene el refresh token JWT emitido previamente en el login o en un
 * refresh anterior.
 */
public record RefreshTokenRequest(
        @NotBlank(message = "El refresh token es obligatorio")
        String refreshToken
) {}
