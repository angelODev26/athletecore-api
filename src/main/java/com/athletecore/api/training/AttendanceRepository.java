package com.athletecore.api.training;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repositorio para entidad Attendance.
 * Los métodos que devuelven registros de asistencia usan @EntityGraph para
 * cargar sesión y deportista dentro de la transacción y evitar
 * LazyInitializationException al mapear DTOs en la capa de presentación.
 */
@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    /**
     * Busca la asistencia activa de un deportista en una sesión (upsert).
     * @param sessionId ID de la sesión
     * @param athleteId ID del deportista
     * @return Optional con el registro de asistencia si existe
     */
    @EntityGraph(attributePaths = {"session", "athlete"})
    Optional<Attendance> findBySessionIdAndAthleteId(Long sessionId, Long athleteId);

    /**
     * Obtiene la lista de asistencia de una sesión.
     * @param sessionId ID de la sesión
     * @return Lista de registros de asistencia de la sesión
     */
    @EntityGraph(attributePaths = {"session", "athlete"})
    List<Attendance> findBySessionId(Long sessionId);

    /**
     * Obtiene el historial de asistencia de un deportista ordenado por fecha de sesión.
     * @param athleteId ID del deportista
     * @return Lista de registros de asistencia del deportista
     */
    @EntityGraph(attributePaths = {"session", "athlete"})
    List<Attendance> findByAthleteIdOrderBySessionSessionDateAsc(Long athleteId);

    /**
     * Consulta para el cálculo de alertas: todas las asistencias de sesiones con
     * fecha menor o igual a hoy (Clock inyectado), ordenadas por deportista y fecha.
     * @param today Fecha actual según el Clock configurado
     * @return Lista ordenada de asistencias relevantes para el cálculo de rachas
     */
    @EntityGraph(attributePaths = {"session", "athlete"})
    @Query("""
            SELECT a FROM Attendance a
            JOIN a.session s
            WHERE s.sessionDate <= :today
            ORDER BY a.athlete.id ASC, s.sessionDate ASC, s.id ASC
            """)
    List<Attendance> findAllForAlertComputation(@Param("today") LocalDate today);

    /**
     * Consulta para el cálculo de alerta de un deportista específico.
     * @param athleteId ID del deportista
     * @param today Fecha actual según el Clock configurado
     * @return Lista ordenada de asistencias del deportista en sesiones hasta hoy
     */
    @EntityGraph(attributePaths = {"session", "athlete"})
    @Query("""
            SELECT a FROM Attendance a
            JOIN a.session s
            WHERE a.athlete.id = :athleteId AND s.sessionDate <= :today
            ORDER BY s.sessionDate ASC, s.id ASC
            """)
    List<Attendance> findForAthleteAlertComputation(@Param("athleteId") Long athleteId,
                                                    @Param("today") LocalDate today);

    /**
     * Marca como eliminadas lógicamente todas las asistencias activas asociadas
     * a cualquiera de las sesiones indicadas. Bulk UPDATE en una sola sentencia SQL,
     * usado por la cascada de soft delete de plan y de sesión.
     * @param sessionIds IDs de las sesiones cuyas asistencias se eliminan
     * @param timestamp Marca temporal a setear en deleted_at
     * @return Número de filas afectadas
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Attendance a SET a.deletedAt = :timestamp "
            + "WHERE a.session.id IN :sessionIds AND a.deletedAt IS NULL")
    int softDeleteBySessionIds(@Param("sessionIds") Collection<Long> sessionIds,
                              @Param("timestamp") Instant timestamp);
}