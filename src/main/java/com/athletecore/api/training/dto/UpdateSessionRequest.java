package com.athletecore.api.training.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import com.athletecore.api.training.SessionStatus;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * DTO de request para actualizar una sesión (actualización parcial).
 * Solo se actualizan los campos no nulos.
 */
public record UpdateSessionRequest(
        Long disciplineId,

        LocalDate sessionDate,

        LocalTime startTime,

        SessionStatus status,

        @Min(value = 0, message = "El volumen no puede ser negativo")
        @Max(value = 100000, message = "El volumen no puede superar 100000")
        Integer volume,

        @Min(value = 0, message = "La intensidad no puede ser negativa")
        @Max(value = 100, message = "La intensidad no puede superar 100")
        Integer intensity,

        @Min(value = 0, message = "La distancia no puede ser negativa")
        Double distance,

        String observations
) {
}