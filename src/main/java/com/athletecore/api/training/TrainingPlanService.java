package com.athletecore.api.training;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.athletecore.api.common.exception.ResourceNotFoundException;
import com.athletecore.api.common.exception.ValidationException;
import com.athletecore.api.training.dto.CreateCycleRequest;
import com.athletecore.api.training.dto.CreateTrainingPlanRequest;
import com.athletecore.api.training.dto.UpdateTrainingPlanRequest;

/**
 * Servicio de planes anuales y jerarquía de ciclos.
 * Valida rangos de fechas, jerarquía (microciclo solo bajo mesociclo) y
 * contención de fechas dentro del plan y del ciclo padre.
 */
@Service
public class TrainingPlanService {

    private final TrainingPlanRepository planRepository;
    private final TrainingCycleRepository cycleRepository;
    private final TrainingSessionRepository sessionRepository;
    private final AttendanceRepository attendanceRepository;

    public TrainingPlanService(TrainingPlanRepository planRepository,
                                TrainingCycleRepository cycleRepository,
                                TrainingSessionRepository sessionRepository,
                                AttendanceRepository attendanceRepository) {
        this.planRepository = planRepository;
        this.cycleRepository = cycleRepository;
        this.sessionRepository = sessionRepository;
        this.attendanceRepository = attendanceRepository;
    }

    /**
     * Crea un plan anual de entrenamiento.
     * @param request Datos del plan
     * @return TrainingPlan creado
     * @throws ValidationException si la fecha de fin es anterior a la de inicio
     */
    @Transactional
    public TrainingPlan createPlan(CreateTrainingPlanRequest request) {
        validateDateRange(request.startDate(), request.endDate());
        TrainingPlan plan = TrainingPlan.builder()
                .name(request.name())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .description(request.description())
                .build();
        return planRepository.save(plan);
    }

    /**
     * Obtiene los planes anuales activos.
     * @return Lista de planes activos ordenados por fecha de inicio
     */
    @Transactional(readOnly = true)
    public List<TrainingPlan> getAllPlans() {
        return planRepository.findAllByDeletedAtIsNullOrderByStartDateAsc();
    }

    /**
     * Obtiene un plan anual por ID.
     * @param id ID del plan
     * @return TrainingPlan encontrado
     * @throws ResourceNotFoundException si no existe o está eliminado lógicamente
     */
    @Transactional(readOnly = true)
    public TrainingPlan getPlanById(Long id) {
        return planRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TrainingPlan", "id", id));
    }

    /**
     * Obtiene los ciclos activos de un plan (para construir la jerarquía).
     * @param planId ID del plan
     * @return Lista de ciclos activos del plan
     */
    @Transactional(readOnly = true)
    public List<TrainingCycle> getCyclesByPlan(Long planId) {
        getPlanById(planId);
        return cycleRepository.findByPlanIdOrderByOrderIndexAscStartDateAsc(planId);
    }

    /**
     * Actualiza parcialmente un plan anual.
     * @param id ID del plan
     * @param request Datos de actualización (solo campos no nulos)
     * @return TrainingPlan actualizado
     * @throws ResourceNotFoundException si el plan no existe
     */
    @Transactional
    public TrainingPlan updatePlan(Long id, UpdateTrainingPlanRequest request) {
        TrainingPlan plan = getPlanById(id);

        if (request.name() != null) {
            plan.setName(request.name());
        }
        if (request.description() != null) {
            plan.setDescription(request.description());
        }
        if (request.startDate() != null || request.endDate() != null) {
            LocalDate effectiveStart = request.startDate() != null ? request.startDate() : plan.getStartDate();
            LocalDate effectiveEnd = request.endDate() != null ? request.endDate() : plan.getEndDate();
            validateDateRange(effectiveStart, effectiveEnd);
            if (request.startDate() != null) {
                plan.setStartDate(request.startDate());
            }
            if (request.endDate() != null) {
                plan.setEndDate(request.endDate());
            }
        }
        return planRepository.save(plan);
    }

    /**
     * Elimina lógicamente un plan anual (soft delete) y propaga la eliminación
     * en cascada a sus ciclos, sesiones de esos ciclos y asistencias de esas sesiones.
     * Toda la operación es atómica dentro de la misma transacción. Se usa una
     * marca de temporal común para todas las bajas (mismo {@link Instant}) de forma
     * que la auditoría refleje que la cascada se originó en el mismo evento.
     * <p>
     * Caso de ciclos sin sesiones asociadas: el bulk UPDATE de sesiones recibe una
     * lista de IDs vacía y se omite (la colección vacía no se envía como parámetro SQL).
     * @param id ID del plan
     * @throws ResourceNotFoundException si el plan no existe
     */
    @Transactional
    public void softDeletePlan(Long id) {
        TrainingPlan plan = getPlanById(id);
        Instant deletedAt = Instant.now();
        plan.setDeletedAt(deletedAt);
        planRepository.save(plan);

        List<Long> cycleIds = cycleRepository.findIdsByPlanId(id);
        if (cycleIds.isEmpty()) {
            return;
        }
        cycleRepository.softDeleteByPlanId(id, deletedAt);

        List<Long> sessionIds = sessionRepository.findActiveIdsByCycleIds(cycleIds);
        if (sessionIds.isEmpty()) {
            return;
        }
        sessionRepository.softDeleteByCycleIds(cycleIds, deletedAt);
        attendanceRepository.softDeleteBySessionIds(sessionIds, deletedAt);
    }

    /**
     * Agrega un ciclo (mesociclo o microciclo) a un plan existente.
     * Reglas de jerarquía:
     * - Un microciclo requiere parentCycleId apuntando a un mesociclo del mismo plan.
     * - Un mesociclo no puede tener ciclo padre.
     * - Las fechas del ciclo deben estar dentro del rango del plan (y del padre si aplica).
     * @param planId ID del plan
     * @param request Datos del ciclo
     * @return TrainingCycle creado
     * @throws ResourceNotFoundException si el plan o el ciclo padre no existen
     * @throws ValidationException si se violan las reglas de jerarquía o fechas
     */
    @Transactional
    public TrainingCycle addCycle(Long planId, CreateCycleRequest request) {
        TrainingPlan plan = getPlanById(planId);
        validateDateRange(request.startDate(), request.endDate());

        validateDatesWithin(request.startDate(), request.endDate(), plan.getStartDate(), plan.getEndDate(),
                "El ciclo debe estar dentro del rango de fechas del plan");

        TrainingCycle parent = null;
        if (request.type() == CycleType.MICROCICLO) {
            if (request.parentCycleId() == null) {
                throw new ValidationException(List.of("Un microciclo debe pertenecer a un mesociclo (parentCycleId obligatorio)"));
            }
            parent = cycleRepository.findById(request.parentCycleId())
                    .orElseThrow(() -> new ResourceNotFoundException("TrainingCycle", "id", request.parentCycleId()));
            if (parent.getType() != CycleType.MESOCICLO) {
                throw new ValidationException(List.of("Un microciclo solo puede ser hijo de un mesociclo"));
            }
            if (!parent.getPlan().getId().equals(planId)) {
                throw new ValidationException(List.of("El ciclo padre debe pertenecer al mismo plan"));
            }
            if (parent.getStartDate() != null && parent.getEndDate() != null) {
                validateDatesWithin(request.startDate(), request.endDate(),
                        parent.getStartDate(), parent.getEndDate(),
                        "El microciclo debe estar dentro del rango de fechas del mesociclo padre");
            }
        } else if (request.parentCycleId() != null) {
            throw new ValidationException(List.of("Un mesociclo no puede tener ciclo padre"));
        }

        Long effectiveParentId = request.type() == CycleType.MICROCICLO ? parent.getId() : null;
        validateCycleUniqueness(planId, effectiveParentId, request.name(), request.orderIndex());

        TrainingCycle cycle = TrainingCycle.builder()
                .plan(plan)
                .type(request.type())
                .name(request.name())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .orderIndex(request.orderIndex())
                .parent(parent)
                .build();
        return cycleRepository.save(cycle);
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (endDate.isBefore(startDate)) {
            throw new ValidationException(List.of("La fecha de fin no puede ser anterior a la fecha de inicio"));
        }
    }

    private void validateDatesWithin(LocalDate startDate, LocalDate endDate,
                                     LocalDate rangeStart, LocalDate rangeEnd,
                                     String message) {
        if (startDate.isBefore(rangeStart) || endDate.isAfter(rangeEnd)) {
            throw new ValidationException(List.of(message));
        }
    }

    /**
     * Valida unicidad de nombre dentro del plan y de order_index bajo el mismo padre.
     * - El nombre debe ser único por plan (mesociclos y microciclos comparten namespace).
     * - El order_index debe ser único por padre: entre mesociclos raíz (parentCycleId null)
     *   y entre microciclos de un mismo mesociclo.
     * @param planId ID del plan
     * @param parentCycleId ID del ciclo padre (null para mesociclos raíz)
     * @param name Nombre del ciclo a crear
     * @param orderIndex Orden del ciclo a crear (puede ser null: no se valida duplicados)
     */
    private void validateCycleUniqueness(Long planId, Long parentCycleId, String name, Integer orderIndex) {
        if (cycleRepository.existsByPlanIdAndName(planId, name)) {
            throw new ValidationException(List.of("Ya existe un ciclo con el nombre '" + name + "' en el plan"));
        }
        if (orderIndex != null
                && cycleRepository.existsByPlanIdAndParentAndOrderIndex(planId, parentCycleId, orderIndex)) {
            throw new ValidationException(List.of(
                    "Ya existe un ciclo con order_index " + orderIndex + " bajo el mismo padre"));
        }
    }
}