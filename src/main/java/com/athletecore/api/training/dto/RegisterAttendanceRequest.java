package com.athletecore.api.training.dto;

import com.athletecore.api.training.AttendanceStatus;

import jakarta.validation.constraints.NotNull;

/**
 * DTO de request para registrar (o actualizar mediante upsert) la asistencia
 * de un deportista a una sesión.
 */
public record RegisterAttendanceRequest(
        @NotNull(message = "El deportista es obligatorio")
        Long athleteId,

        @NotNull(message = "El estado de asistencia es obligatorio")
        AttendanceStatus status
) {
}