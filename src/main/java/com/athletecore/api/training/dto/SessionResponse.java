package com.athletecore.api.training.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import com.athletecore.api.training.SessionStatus;
import com.athletecore.api.training.TrainingSession;

/**
 * DTO de respuesta para una sesión de entrenamiento (sin listado de asistencia).
 * No expone la entidad JPA directamente.
 */
public record SessionResponse(
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
        boolean active
) {
    /**
     * Crea el DTO desde la entidad TrainingSession.
     * @param session Entidad TrainingSession
     * @return SessionResponse
     */
    public static SessionResponse fromSession(TrainingSession session) {
        Long cycleId = session.getCycle() != null ? session.getCycle().getId() : null;
        Long disciplineId = session.getDiscipline() != null ? session.getDiscipline().getId() : null;
        return new SessionResponse(
                session.getId(),
                cycleId,
                disciplineId,
                session.getSessionDate(),
                session.getStartTime(),
                session.getStatus(),
                session.getVolume(),
                session.getIntensity(),
                session.getDistance(),
                session.getObservations(),
                session.isActive()
        );
    }
}