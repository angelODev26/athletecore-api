package com.athletecore.api.checkup.dto;

import java.time.Instant;
import java.util.List;

import com.athletecore.api.checkup.Checkup;
import com.athletecore.api.checkup.CheckupTime;

/**
 * DTO de respuesta para un chequeo con sus tiempos de prueba embebidos.
 * No expone la entidad JPA directamente.
 */
public record CheckupDetailResponse(
    Long id,
    Long athleteId,
    Integer year,
    Integer month,
    String category,
    String notes,
    Instant createdAt,
    Instant updatedAt,
    boolean active,
    List<CheckupTimeResponse> times
) {
    /**
     * Crea CheckupDetailResponse desde la entidad Checkup y sus tiempos activos.
     *
     * @param checkup Entidad Checkup
     * @param times   Lista de CheckupTime activos del chequeo
     * @return CheckupDetailResponse
     */
    public static CheckupDetailResponse fromCheckup(Checkup checkup, List<CheckupTime> times) {
        List<CheckupTimeResponse> timeResponses = times.stream()
                .map(CheckupTimeResponse::fromCheckupTime)
                .toList();
        return new CheckupDetailResponse(
            checkup.getId(),
            checkup.getAthlete().getId(),
            checkup.getYear(),
            checkup.getMonth(),
            checkup.getCategory(),
            checkup.getNotes(),
            checkup.getCreatedAt(),
            checkup.getUpdatedAt(),
            checkup.isActive(),
            timeResponses
        );
    }
}
