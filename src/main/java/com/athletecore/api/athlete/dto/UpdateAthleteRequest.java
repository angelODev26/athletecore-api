package com.athletecore.api.athlete.dto;

import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * DTO para actualizar un deportista existente.
 */
public record UpdateAthleteRequest(
    @Size(max = 100, message = "El nombre debe tener menos de 100 caracteres")
    String firstName,

    @Size(max = 100, message = "El apellido debe tener menos de 100 caracteres")
    String lastName,

    LocalDate birthDate,

    String photoUrl
) {
}
