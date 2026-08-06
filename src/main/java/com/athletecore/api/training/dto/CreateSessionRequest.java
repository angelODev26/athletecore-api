package com.athletecore.api.training.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import com.athletecore.api.training.SessionStatus;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * DTO de request para registrar una sesión de entrenamiento.
 * cycleId y disciplineId son opcionales (la sesión puede existir sin ciclo/disciplina).
 */
public record CreateSessionRequest(
        Long cycleId,

        Long disciplineId,

        @NotNull(message = "La fecha de la sesión es obligatoria")
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