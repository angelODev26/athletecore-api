package com.athletecore.api.training;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repositorio para entidad TrainingCycle.
 * Consultas indexadas por plan y por ciclo padre (autorelación).
 */
@Repository
public interface TrainingCycleRepository extends JpaRepository<TrainingCycle, Long> {

    /**
     * Obtiene los ciclos activos de un plan ordenados por orden y fecha de inicio.
     * Usa el índice parcial idx_training_cycles_plan (plan_id, parent_cycle_id).
     * @param planId ID del plan anual
     * @return Lista de ciclos activos del plan
     */
    List<TrainingCycle> findByPlanIdOrderByOrderIndexAscStartDateAsc(Long planId);

    /**
     * Obtiene los ciclos activos hijos de un ciclo padre (microciclos de un mesociclo).
     * @param parentCycleId ID del ciclo padre
     * @return Lista de ciclos hijos activos
     */
    List<TrainingCycle> findByParentIdOrderByOrderIndexAsc(Long parentCycleId);

    /**
     * Comprueba si ya existe un ciclo activo con el order_index indicado bajo el mismo plan
     * y mismo padre. El filtro por parentCycleId usa IS NULL cuando es null (mesociclos raíz).
     * @param planId ID del plan
     * @param parentCycleId ID del ciclo padre (null para mesociclos raíz)
     * @param orderIndex Índice de orden a comprobar (null se considera no duplicable)
     * @return true si ya existe un ciclo activo con ese order_index bajo el mismo padre
     */
    @Query("""
            SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END
            FROM TrainingCycle c
            WHERE c.plan.id = :planId
              AND (:parentCycleId IS NULL AND c.parent.id IS NULL
                   OR :parentCycleId IS NOT NULL AND c.parent.id = :parentCycleId)
              AND c.orderIndex = :orderIndex
            """)
    boolean existsByPlanIdAndParentAndOrderIndex(@Param("planId") Long planId,
                                                  @Param("parentCycleId") Long parentCycleId,
                                                  @Param("orderIndex") Integer orderIndex);

    /**
     * Comprueba si ya existe un ciclo activo con el nombre indicado dentro del plan.
     * @param planId ID del plan
     * @param name Nombre del ciclo a comprobar
     * @return true si ya existe un ciclo activo con ese nombre en el plan
     */
    boolean existsByPlanIdAndName(Long planId, String name);

    /**
     * Obtiene los IDs de los ciclos activos de un plan (para propagar el soft delete
     * a sesiones y asistencias en cascada).
     * @param planId ID del plan
     * @return Lista de IDs de ciclos activos
     */
    @Query("SELECT c.id FROM TrainingCycle c WHERE c.plan.id = :planId")
    List<Long> findIdsByPlanId(@Param("planId") Long planId);

    /**
     * Marca como eliminados lógicamente todos los ciclos activos del plan.
     * Bulk UPDATE en una sola sentencia SQL. El {@link @Modifying} flush + clear
     * asegura consistencia con la sesión JPA.
     * @param planId ID del plan cuyos ciclos se eliminan
     * @param timestamp Marca temporal a setear en deleted_at (no reloj del sistema)
     * @return Número de filas afectadas
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE TrainingCycle c SET c.deletedAt = :timestamp WHERE c.plan.id = :planId AND c.deletedAt IS NULL")
    int softDeleteByPlanId(@Param("planId") Long planId, @Param("timestamp") Instant timestamp);
}