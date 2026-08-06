package com.athletecore.api.training.dto;

import java.time.LocalDate;

import com.athletecore.api.training.Attendance;
import com.athletecore.api.training.AttendanceStatus;

/**
 * DTO de respuesta para un registro de asistencia.
 * Incluye datos de contexto (fecha de sesión y nombre del deportista).
 */
public record AttendanceResponse(
        Long id,
        Long sessionId,
        LocalDate sessionDate,
        Long athleteId,
        String athleteFullName,
        AttendanceStatus status
) {
    /**
     * Crea el DTO desde la entidad Attendance.
     * Se requiere que sesión y deportista estén cargados (ver @EntityGraph del repositorio).
     * @param attendance Entidad Attendance
     * @return AttendanceResponse
     */
    public static AttendanceResponse fromAttendance(Attendance attendance) {
        return new AttendanceResponse(
                attendance.getId(),
                attendance.getSession().getId(),
                attendance.getSession().getSessionDate(),
                attendance.getAthlete().getId(),
                attendance.getAthlete().getFullName(),
                attendance.getStatus()
        );
    }
}