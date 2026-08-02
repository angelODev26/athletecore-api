package com.athletecore.api.athlete.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO para crear una nueva disciplina.
 */
public record CreateDisciplineRequest(
    @NotBlank(message = "El nombre de la disciplina es obligatorio")
    @Size(max = 100, message = "El nombre debe tener menos de 100 caracteres")
    String name,

    String description
) {
}
