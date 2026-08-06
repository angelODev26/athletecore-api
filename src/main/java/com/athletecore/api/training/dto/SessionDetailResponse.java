package com.athletecore.api.training.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.athletecore.api.training.Attendance;
import com.athletecore.api.training.SessionStatus;
import com.athletecore.api.training.TrainingSession;

/**
 * DTO de respuesta con el detalle de una sesión y su listado de asistencia.
 * No expone entidades JPA.
 */
public record SessionDetailResponse(
        Long id,
        Long cycleId,
        Long disciplineId,
        LocalDate sessionDate,
        LocalTime startTime,
        SessionStatus status,
        Integer volume,
        Integer intensity,
        Double distance,
        String observations,
        boolean active,
        List<AttendanceResponse> attendance
) {
    /**
     * Construye el detalle de la sesión con su listado de asistencia.
     * @param session Entidad TrainingSession
     * @param attendance Lista de asistencias de la sesión (con sesión y deportista cargados)
     * @return SessionDetailResponse
     */
    public static SessionDetailResponse fromSession(TrainingSession session, List<Attendance> attendance) {
        List<AttendanceResponse> attendanceResponses = attendance.stream()
                .map(AttendanceResponse::fromAttendance)
                .toList();
        SessionResponse base = SessionResponse.fromSession(session);
        return new SessionDetailResponse(
                base.id(),
                base.cycleId(),
                base.disciplineId(),
                base.sessionDate(),
                base.startTime(),
                base.status(),
                base.volume(),
                base.intensity(),
                base.distance(),
                base.observations(),
                base.active(),
                attendanceResponses
        );
    }
}