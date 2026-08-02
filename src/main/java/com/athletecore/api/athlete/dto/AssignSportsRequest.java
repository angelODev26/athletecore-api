package com.athletecore.api.athlete.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;

/**
 * DTO para asignar deportes a un atleta.
 */
public record AssignSportsRequest(
    @NotNull(message = "La lista de deportes es obligatoria")
    @Size(min = 1, max = 10, message = "Debe asignar entre 1 y 10 deportes")
    Set<Long> sportIds
) {
}
