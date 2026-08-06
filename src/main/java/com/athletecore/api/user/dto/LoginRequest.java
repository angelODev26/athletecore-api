package com.athletecore.api.user.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO de solicitud de login.
 * Valida que tanto el username como la contraseña sean obligatorios.
 */
public record LoginRequest(
        @NotBlank(message = "El nombre de usuario es obligatorio")
        String username,

        @NotBlank(message = "La contraseña es obligatoria")
        String password
) {}
