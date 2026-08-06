package com.athletecore.api.training.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO de request para crear un plan anual de entrenamiento.
 */
public record CreateTrainingPlanRequest(
        @NotBlank(message = "El nombre del plan es obligatorio")
        @Size(max = 150, message = "El nombre del plan no puede superar 150 caracteres")
        String name,

        @NotNull(message = "La fecha de inicio es obligatoria")
        LocalDate startDate,

        @NotNull(message = "La fecha de fin es obligatoria")
        LocalDate endDate,

        String description
) {
}
