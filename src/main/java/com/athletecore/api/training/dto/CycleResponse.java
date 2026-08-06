package com.athletecore.api.training.dto;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.athletecore.api.training.CycleType;
import com.athletecore.api.training.TrainingCycle;

/**
 * DTO de respuesta para ciclos de planificación.
 * Puede contener ciclos hijos (microciclos) cuando se expone la jerarquía.
 */
public record CycleResponse(
        Long id,
        CycleType type,
        String name,
        LocalDate startDate,
        LocalDate endDate,
        Integer orderIndex,
        Long parentCycleId,
        List<CycleResponse> children
) {
    /**
     * Crea el DTO desde la entidad TrainingCycle sin ciclos hijos.
     * @param cycle Entidad TrainingCycle
     * @return CycleResponse
     */
    public static CycleResponse fromCycle(TrainingCycle cycle) {
        Long parentId = cycle.getParent() != null ? cycle.getParent().getId() : null;
        return new CycleResponse(cycle.getId(), cycle.getType(), cycle.getName(),
                cycle.getStartDate(), cycle.getEndDate(), cycle.getOrderIndex(),
                parentId, Collections.emptyList());
    }

    /**
     * Crea el DTO desde la entidad incluyendo sus ciclos hijos (microciclos).
     * @param cycle Entidad TrainingCycle
     * @param children Lista de ciclos hijos activos
     * @return CycleResponse
     */
    public static CycleResponse fromCycle(TrainingCycle cycle, List<TrainingCycle> children) {
        Long parentId = cycle.getParent() != null ? cycle.getParent().getId() : null;
        List<CycleResponse> childResponses = children.stream()
                .sorted(Comparator.comparing(TrainingCycle::getOrderIndex,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(TrainingCycle::getStartDate,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .map(CycleResponse::fromCycle)
                .toList();
        return new CycleResponse(cycle.getId(), cycle.getType(), cycle.getName(),
                cycle.getStartDate(), cycle.getEndDate(), cycle.getOrderIndex(),
                parentId, childResponses);
    }
}