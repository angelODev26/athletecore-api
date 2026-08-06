package com.athletecore.api.training;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repositorio para entidad TrainingSession.
 * Consultas indexadas para el listado de sesiones por ciclo (idx_training_sessions_cycle).
 */
@Repository
public interface TrainingSessionRepository extends JpaRepository<TrainingSession, Long> {

    /**
     * Obtiene las sesiones activas de un ciclo ordenadas por fecha de sesión.
     * @param cycleId ID del ciclo
     * @return Lista de sesiones activas del ciclo
     */
    List<TrainingSession> findByCycleIdOrderBySessionDateAsc(Long cycleId);

    /**
     * Obtiene los IDs de las sesiones activas pertenecientes a una colección de ciclos.
     * Usado por la cascada de soft delete: al eliminar un plan, obtener IDs de sesión
     * para propagar la baja a las asistencias asociadas.
     * @param cycleIds IDs de los ciclos padres (no null ni vacío)
     * @return Lista de IDs de sesiones activas
     */
    @Query("SELECT s.id FROM TrainingSession s WHERE s.cycle.id IN :cycleIds AND s.deletedAt IS NULL")
    List<Long> findActiveIdsByCycleIds(@Param("cycleIds") Collection<Long> cycleIds);

    /**
     * Marca como eliminadas lógicamente todas las sesiones activas que pertenecen
     * a alguno de los ciclos indicados. Bulk UPDATE en una sola sentencia SQL.
     * @param cycleIds IDs de los ciclos cuyas sesiones se eliminan
     * @param timestamp Marca temporal a setear en deleted_at
     * @return Número de filas afectadas
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE TrainingSession s SET s.deletedAt = :timestamp "
            + "WHERE s.cycle.id IN :cycleIds AND s.deletedAt IS NULL")
    int softDeleteByCycleIds(@Param("cycleIds") Collection<Long> cycleIds,
                             @Param("timestamp") Instant timestamp);

    /**
     * Marca como eliminada lógicamente una sesión por ID (cascade a attendance).
     * Bulk UPDATE alternativo al setDeletedAt en la entidad: actualiza la columna
     * directamente sin carga previa.
     * @param sessionId ID de la sesión
     * @param timestamp Marca temporal a setear en deleted_at
     * @return Número de filas afectadas (1 si existía, 0 si no)
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE TrainingSession s SET s.deletedAt = :timestamp WHERE s.id = :sessionId AND s.deletedAt IS NULL")
    int softDeleteById(@Param("sessionId") Long sessionId, @Param("timestamp") Instant timestamp);
}