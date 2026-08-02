package com.athletecore.api.athlete.dto;

import com.athletecore.api.athlete.Discipline;

/**
 * DTO de respuesta para disciplinas.
 */
public record DisciplineResponse(
    Long id,
    Long sportId,
    String name,
    String description,
    boolean active
) {
    /**
     * Crea DisciplineResponse desde una entidad Discipline.
     */
    public static DisciplineResponse fromDiscipline(Discipline discipline) {
        return new DisciplineResponse(
            discipline.getId(),
            discipline.getSport() != null ? discipline.getSport().getId() : null,
            discipline.getName(),
            discipline.getDescription(),
            discipline.isActive()
        );
    }
}
