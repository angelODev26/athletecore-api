package com.athletecore.api.training.dto;

import java.time.LocalDate;

import com.athletecore.api.training.TrainingPlan;

/**
 * DTO de respuesta para planes anuales (sin jerarquía de ciclos).
 * No expone la entidad JPA directamente.
 */
public record TrainingPlanResponse(
        Long id,
        String name,
        LocalDate startDate,
        LocalDate endDate,
        String description,
        boolean active
) {
    /**
     * Crea el DTO desde la entidad TrainingPlan.
     * @param plan Entidad TrainingPlan
     * @return TrainingPlanResponse
     */
    public static TrainingPlanResponse fromPlan(TrainingPlan plan) {
        return new TrainingPlanResponse(
                plan.getId(),
                plan.getName(),
                plan.getStartDate(),
                plan.getEndDate(),
                plan.getDescription(),
                plan.isActive()
        );
    }
}
