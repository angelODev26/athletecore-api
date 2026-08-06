package com.athletecore.api.training;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio para entidad TrainingPlan.
 * @SQLRestriction de la entidad filtra automáticamente los registros
 * eliminados lógicamente (deleted_at IS NULL).
 */
@Repository
public interface TrainingPlanRepository extends JpaRepository<TrainingPlan, Long> {

    /**
     * Obtiene los planes activos ordenados por fecha de inicio.
     * @return Lista de planes activos
     */
    List<TrainingPlan> findAllByDeletedAtIsNullOrderByStartDateAsc();
}