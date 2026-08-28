package com.athletecore.api.checkup.dto;

import java.time.Instant;

import com.athletecore.api.checkup.Checkup;

/**
 * DTO de respuesta para un chequeo (sin tiempos embebidos).
 * No expone la entidad JPA directamente.
 */
public record CheckupResponse(
    Long id,
    Long athleteId,
    Integer year,
    Integer month,
    String category,
    String notes,
    Instant createdAt,
    Instant updatedAt,
    boolean active
) {
    /**
     * Crea CheckupResponse desde una entidad Checkup.
     *
     * @param checkup Entidad Checkup
     * @return CheckupResponse
     */
    public static CheckupResponse fromCheckup(Checkup checkup) {
        return new CheckupResponse(
            checkup.getId(),
            checkup.getAthlete().getId(),
            checkup.getYear(),
            checkup.getMonth(),
            checkup.getCategory(),
            checkup.getNotes(),
            checkup.getCreatedAt(),
            checkup.getUpdatedAt(),
            checkup.isActive()
        );
    }
}
