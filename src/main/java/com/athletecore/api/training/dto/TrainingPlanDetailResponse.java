package com.athletecore.api.training.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.athletecore.api.training.TrainingCycle;
import com.athletecore.api.training.TrainingPlan;

/**
 * DTO de respuesta con el detalle de un plan anual y su jerarquía de ciclos
 * (mesociclos con sus microciclos). No expone entidades JPA.
 */
public record TrainingPlanDetailResponse(
        Long id,
        String name,
        LocalDate startDate,
        LocalDate endDate,
        String description,
        boolean active,
        List<CycleResponse> cycles
) {
    /**
     * Construye el detalle del plan con la jerarquía de ciclos.
     * Los ciclos sin padre son mesociclos; sus hijos se agrupan como microciclos.
     * @param plan Entidad TrainingPlan
     * @param cycles Lista plana de ciclos activos del plan
     * @return TrainingPlanDetailResponse
     */
    public static TrainingPlanDetailResponse fromPlan(TrainingPlan plan, List<TrainingCycle> cycles) {
        Map<Long, List<TrainingCycle>> childrenByParentId = cycles.stream()
                .filter(cycle -> cycle.getParent() != null)
                .collect(Collectors.groupingBy(cycle -> cycle.getParent().getId()));

        List<CycleResponse> mesocycles = cycles.stream()
                .filter(cycle -> cycle.getParent() == null)
                .sorted(java.util.Comparator.comparing(TrainingCycle::getOrderIndex,
                                java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder()))
                        .thenComparing(TrainingCycle::getStartDate,
                                java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())))
                .map(cycle -> CycleResponse.fromCycle(cycle, childrenByParentId.getOrDefault(cycle.getId(), List.of())))
                .toList();

        return new TrainingPlanDetailResponse(
                plan.getId(),
                plan.getName(),
                plan.getStartDate(),
                plan.getEndDate(),
                plan.getDescription(),
                plan.isActive(),
                mesocycles
        );
    }
}
