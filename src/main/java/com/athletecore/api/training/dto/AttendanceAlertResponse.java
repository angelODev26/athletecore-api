package com.athletecore.api.training.dto;

import java.time.LocalDate;

/**
 * DTO de respuesta para una alerta de ausencias consecutivas.
 * Representa el modelo derivado (sin persistencia) de un deportista que alcanzó
 * el umbral configurado de ausencias consecutivas en sesiones recientes.
 */
public record AttendanceAlertResponse(
        Long athleteId,
        String athleteFullName,
        int consecutiveAbsenceCount,
        LocalDate lastAbsenceDate
) {
}