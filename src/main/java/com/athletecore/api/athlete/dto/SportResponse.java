package com.athletecore.api.athlete.dto;

import java.util.List;
import java.util.stream.Collectors;

import com.athletecore.api.athlete.Sport;

/**
 * DTO de respuesta para deportes.
 */
public record SportResponse(
    Long id,
    String name,
    String description,
    boolean active,
    List<Long> disciplineIds
) {
    /**
     * Crea SportResponse desde una entidad Sport.
     */
    public static SportResponse fromSport(Sport sport) {
        List<Long> disciplineIds = sport.getDisciplines().stream()
            .map(d -> d.getId())
            .collect(Collectors.toList());

        return new SportResponse(
            sport.getId(),
            sport.getName(),
            sport.getDescription(),
            sport.isActive(),
            disciplineIds
        );
    }
}
