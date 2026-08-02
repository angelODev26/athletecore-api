package com.athletecore.api.athlete.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO para crear un nuevo deportista.
 */
public record CreateAthleteRequest(
    @NotBlank(message = "El usuario es obligatorio")
    @Size(min = 3, max = 255, message = "El usuario debe tener entre 3 y 255 caracteres")
    String username,

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Debe ser un email válido")
    @Size(max = 255, message = "El email debe tener menos de 255 caracteres")
    String email,

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre debe tener menos de 100 caracteres")
    String firstName,

    @NotBlank(message = "El apellido es obligatorio")
    @Size(max = 100, message = "El apellido debe tener menos de 100 caracteres")
    String lastName,

    String photoUrl
) {
}
