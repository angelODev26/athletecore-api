package com.athletecore.api.training.dto;

import java.time.LocalDate;

import com.athletecore.api.training.CycleType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO de request para agregar un ciclo (mesociclo/microciclo) a un plan.
 * Para microciclos, parentCycleId es obligatorio y debe apuntar a un mesociclo.
 */
public record CreateCycleRequest(
        @NotNull(message = "El tipo de ciclo es obligatorio")
        CycleType type,

        @NotBlank(message = "El nombre del ciclo es obligatorio")
        @Size(max = 150, message = "El nombre del ciclo no puede superar 150 caracteres")
        String name,

        @NotNull(message = "La fecha de inicio es obligatoria")
        LocalDate startDate,

        @NotNull(message = "La fecha de fin es obligatoria")
        LocalDate endDate,

        Integer orderIndex,

        Long parentCycleId
) {
}