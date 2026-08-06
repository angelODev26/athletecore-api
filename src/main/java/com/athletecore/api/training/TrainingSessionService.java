package com.athletecore.api.training;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.athletecore.api.athlete.Discipline;
import com.athletecore.api.athlete.DisciplineRepository;
import com.athletecore.api.common.exception.ResourceNotFoundException;
import com.athletecore.api.common.exception.ValidationException;
import com.athletecore.api.training.dto.CreateSessionRequest;
import com.athletecore.api.training.dto.UpdateSessionRequest;

/**
 * Servicio de sesiones de entrenamiento.
 * Registra sesiones (con o sin ciclo), lista por ciclo, consulta detalle,
 * actualiza, cambia estado y elimina lógicamente.
 */
@Service
public class TrainingSessionService {

    private final TrainingSessionRepository sessionRepository;
    private final TrainingCycleRepository cycleRepository;
    private final DisciplineRepository disciplineRepository;
    private final AttendanceRepository attendanceRepository;

    public TrainingSessionService(TrainingSessionRepository sessionRepository,
                                   TrainingCycleRepository cycleRepository,
                                   DisciplineRepository disciplineRepository,
                                   AttendanceRepository attendanceRepository) {
        this.sessionRepository = sessionRepository;
        this.cycleRepository = cycleRepository;
        this.disciplineRepository = disciplineRepository;
        this.attendanceRepository = attendanceRepository;
    }

    /**
     * Registra una sesión de entrenamiento. Si no se indica estado, se asume PROGRAMADA.
     * @param request Datos de la sesión
     * @return TrainingSession creada
     * @throws ResourceNotFoundException si el ciclo o la disciplina referenciados no existen
     */
    @Transactional
    public TrainingSession createSession(CreateSessionRequest request) {
        TrainingCycle cycle = resolveCycle(request.cycleId());
        Discipline discipline = resolveDiscipline(request.disciplineId());

        SessionStatus status = request.status() != null ? request.status() : SessionStatus.PROGRAMADA;

        TrainingSession session = TrainingSession.builder()
                .cycle(cycle)
                .discipline(discipline)
                .sessionDate(request.sessionDate())
                .startTime(request.startTime())
                .status(status)
                .volume(request.volume())
                .intensity(request.intensity())
                .distance(request.distance())
                .observations(request.observations())
                .build();
        return sessionRepository.save(session);
    }

    /**
     * Registra una sesión dentro de un ciclo (endpoint anidado).
     * @param cycleId ID del ciclo
     * @param request Datos de la sesión (cycleId del request debe coincidir con el path)
     * @return TrainingSession creada
     */
    @Transactional
    public TrainingSession createSessionForCycle(Long cycleId, CreateSessionRequest request) {
        if (request.cycleId() != null && !request.cycleId().equals(cycleId)) {
            throw new ValidationException(List.of("El ciclo indicado en el cuerpo no coincide con el ciclo de la ruta"));
        }
        CreateSessionRequest effective = new CreateSessionRequest(
                cycleId,
                request.disciplineId(),
                request.sessionDate(),
                request.startTime(),
                request.status(),
                request.volume(),
                request.intensity(),
                request.distance(),
                request.observations());
        return createSession(effective);
    }

    /**
     * Obtiene las sesiones activas de un ciclo ordenadas por fecha.
     * @param cycleId ID del ciclo
     * @return Lista de sesiones del ciclo
     * @throws ResourceNotFoundException si el ciclo no existe
     */
    @Transactional(readOnly = true)
    public List<TrainingSession> getSessionsByCycle(Long cycleId) {
        getCycleById(cycleId);
        return sessionRepository.findByCycleIdOrderBySessionDateAsc(cycleId);
    }

    /**
     * Obtiene una sesión por ID.
     * @param id ID de la sesión
     * @return TrainingSession encontrada
     * @throws ResourceNotFoundException si no existe o está eliminada lógicamente
     */
    @Transactional(readOnly = true)
    public TrainingSession getSessionById(Long id) {
        return sessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TrainingSession", "id", id));
    }

    /**
     * Actualiza parcialmente una sesión. Si se cambia disciplina, se valida su existencia.
     * @param id ID de la sesión
     * @param request Datos de actualización (solo campos no nulos)
     * @return TrainingSession actualizada
     * @throws ResourceNotFoundException si la sesión o la disciplina no existen
     */
    @Transactional
    public TrainingSession updateSession(Long id, UpdateSessionRequest request) {
        TrainingSession session = getSessionById(id);

        if (request.disciplineId() != null) {
            session.setDiscipline(resolveDiscipline(request.disciplineId()));
        }
        if (request.sessionDate() != null) {
            session.setSessionDate(request.sessionDate());
        }
        if (request.startTime() != null) {
            session.setStartTime(request.startTime());
        }
        if (request.status() != null) {
            session.setStatus(request.status());
        }
        if (request.volume() != null) {
            session.setVolume(request.volume());
        }
        if (request.intensity() != null) {
            session.setIntensity(request.intensity());
        }
        if (request.distance() != null) {
            session.setDistance(request.distance());
        }
        if (request.observations() != null) {
            session.setObservations(request.observations());
        }
        return sessionRepository.save(session);
    }

    /**
     * Cambia el estado de una sesión.
     * @param id ID de la sesión
     * @param status Nuevo estado
     * @return TrainingSession actualizada
     * @throws ResourceNotFoundException si la sesión no existe
     */
    @Transactional
    public TrainingSession changeStatus(Long id, SessionStatus status) {
        TrainingSession session = getSessionById(id);
        session.setStatus(status);
        return sessionRepository.save(session);
    }

    /**
     * Elimina lógicamente una sesión (soft delete) y propaga la eliminación a las
     * asistencias asociadas. Operación atómica con marca de temporal común.
     * @param id ID de la sesión
     * @throws ResourceNotFoundException si la sesión no existe
     */
    @Transactional
    public void softDeleteSession(Long id) {
        TrainingSession session = getSessionById(id);
        Instant deletedAt = Instant.now();
        session.setDeletedAt(deletedAt);
        sessionRepository.save(session);
        attendanceRepository.softDeleteBySessionIds(List.of(id), deletedAt);
    }

    private TrainingCycle getCycleById(Long cycleId) {
        return cycleRepository.findById(cycleId)
                .orElseThrow(() -> new ResourceNotFoundException("TrainingCycle", "id", cycleId));
    }

    private TrainingCycle resolveCycle(Long cycleId) {
        return cycleId != null ? getCycleById(cycleId) : null;
    }

    private Discipline resolveDiscipline(Long disciplineId) {
        if (disciplineId == null) {
            return null;
        }
        return disciplineRepository.findById(disciplineId)
                .orElseThrow(() -> new ResourceNotFoundException("Discipline", "id", disciplineId));
    }
}