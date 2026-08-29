package com.athletecore.api.report;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repositorio para entidad ReportSchedule.
 * Las consultas activas filtran deleted_at IS NULL de forma explícita además
 * de la @SQLRestriction de la entidad.
 */
@Repository
public interface ReportScheduleRepository extends JpaRepository<ReportSchedule, Long> {

    /**
     * Lista todos los schedules activos (no eliminados), con independencia del flag active.
     */
    List<ReportSchedule> findAllByDeletedAtIsNull();

    /**
     * Lista los schedules marcados como activos (active = TRUE) para el scheduler.
     */
    @Query("SELECT s FROM ReportSchedule s WHERE s.deletedAt IS NULL AND s.active = TRUE "
            + "ORDER BY s.nextRunAt ASC")
    List<ReportSchedule> findActiveSchedules();

    /**
     * Lista los schedules activos vencidos: active = TRUE y con next_run_at
     * menor o igual al instante dado. Es lo que consume el scheduler para disparar
     * las ejecuciones (decisión D2 del design).
     *
     * @param now Instante actual según el Clock inyectado
     * @return Lista de schedules vencidos, en orden ascendente de next_run_at
     */
    @Query("SELECT s FROM ReportSchedule s WHERE s.deletedAt IS NULL AND s.active = TRUE "
            + "AND s.nextRunAt IS NOT NULL AND s.nextRunAt <= :now ORDER BY s.nextRunAt ASC")
    List<ReportSchedule> findDueActiveSchedules(@Param("now") Instant now);
}