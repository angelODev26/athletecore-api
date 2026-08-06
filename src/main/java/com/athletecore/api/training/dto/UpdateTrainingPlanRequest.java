package com.athletecore.api.training.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Size;

/**
 * DTO de request para actualizar un plan anual (actualización parcial).
 * Solo se actualizan los campos no nulos.
 */
public record UpdateTrainingPlanRequest(
        @Size(max = 150, message = "El nombre del plan no puede superar 150 caracteres")
        String name,

        LocalDate startDate,

        LocalDate endDate,

        String description
) {
}
