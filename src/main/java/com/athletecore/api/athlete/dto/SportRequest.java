package com.athletecore.api.athlete.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO para crear un nuevo deporte.
 */
public record SportRequest(
    @NotBlank(message = "El nombre del deporte es obligatorio")
    @Size(max = 100, message = "El nombre debe tener menos de 100 caracteres")
    String name,

    String description
) {
}
