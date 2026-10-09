package com.athletecore.api.user.dto;

/**
 * DTO de respuesta de autenticación.
 * Incluye el access token JWT, el refresh token para renovar la sesión, el
 * tipo de token, el tiempo de expiración del access token en milisegundos y
 * la información mínima del usuario autenticado (nunca la contraseña).
 */
public record AuthResponse(
        String token,
        String refreshToken,
        String type,
        long expiresIn,
        UserResponse user
) {}
