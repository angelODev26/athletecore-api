package com.athletecore.api.user.dto;

/**
 * DTO de respuesta de autenticación.
 * Incluye el token JWT, su tipo, el tiempo de expiración en milisegundos y
 * la información mínima del usuario autenticado (nunca la contraseña).
 */
public record AuthResponse(
        String token,
        String type,
        long expiresIn,
        UserResponse user
) {}
