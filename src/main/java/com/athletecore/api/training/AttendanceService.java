package com.athletecore.api.training;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.athletecore.api.athlete.Athlete;
import com.athletecore.api.athlete.AthleteRepository;
import com.athletecore.api.common.exception.ResourceNotFoundException;
import com.athletecore.api.common.exception.ValidationException;
import com.athletecore.api.training.dto.RegisterAttendanceRequest;

/**
 * Servicio de control de asistencia.
 * Garantiza el invariante de una fila por par (sesión, deportista): el
 * re-registro actualiza el estado existente (upsert).
 */
@Service
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final TrainingSessionRepository sessionRepository;
    private final AthleteRepository athleteRepository;

    public AttendanceService(AttendanceRepository attendanceRepository,
                             TrainingSessionRepository sessionRepository,
                             AthleteRepository athleteRepository) {
        this.attendanceRepository = attendanceRepository;
        this.sessionRepository = sessionRepository;
        this.athleteRepository = athleteRepository;
    }

    /**
     * Registra (o actualiza mediante upsert) la asistencia de un deportista a una sesión.
     * @param sessionId ID de la sesión
     * @param request Datos de asistencia (deportista y estado)
     * @return Attendance creada o actualizada
     * @throws ResourceNotFoundException si la sesión o el deportista no existen
     * @throws ValidationException si la sesión está cancelada
     */
    @Transactional
    public Attendance registerAttendance(Long sessionId, RegisterAttendanceRequest request) {
        TrainingSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("TrainingSession", "id", sessionId));

        if (session.getStatus() == SessionStatus.CANCELADA) {
            throw new ValidationException(List.of("No se puede registrar asistencia en una sesión cancelada"));
        }

        Athlete athlete = athleteRepository.findById(request.athleteId())
                .orElseThrow(() -> new ResourceNotFoundException("Athlete", "id", request.athleteId()));

        return attendanceRepository.findBySessionIdAndAthleteId(sessionId, request.athleteId())
                .map(existing -> {
                    existing.setStatus(request.status());
                    return attendanceRepository.save(existing);
                })
                .orElseGet(() -> attendanceRepository.save(Attendance.builder()
                        .session(session)
                        .athlete(athlete)
                        .status(request.status())
                        .build()));
    }

    /**
     * Obtiene la lista de asistencia de una sesión.
     * @param sessionId ID de la sesión
     * @return Lista de registros de asistencia de la sesión
     * @throws ResourceNotFoundException si la sesión no existe
     */
    @Transactional(readOnly = true)
    public List<Attendance> getAttendanceBySession(Long sessionId) {
        sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("TrainingSession", "id", sessionId));
        return attendanceRepository.findBySessionId(sessionId);
    }

    /**
     * Obtiene el historial de asistencia de un deportista ordenado por fecha de sesión.
     * @param athleteId ID del deportista
     * @return Lista de registros de asistencia del deportista
     * @throws ResourceNotFoundException si el deportista no existe
     */
    @Transactional(readOnly = true)
    public List<Attendance> getAttendanceByAthlete(Long athleteId) {
        athleteRepository.findById(athleteId)
                .orElseThrow(() -> new ResourceNotFoundException("Athlete", "id", athleteId));
        return attendanceRepository.findByAthleteIdOrderBySessionSessionDateAsc(athleteId);
    }
}