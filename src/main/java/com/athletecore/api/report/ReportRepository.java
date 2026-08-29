package com.athletecore.api.report;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repositorio para entidad Report.
 * Las consultas activas filtran deleted_at IS NULL de forma explícita además
 * de la @SQLRestriction de la entidad.
 */
@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    /**
     * Lista los reportes activos de un deportista, del más reciente al más antiguo.
     */
    @Query("SELECT r FROM Report r WHERE r.athleteId = :athleteId AND r.deletedAt IS NULL "
            + "ORDER BY r.createdAt DESC")
    List<Report> findActiveByAthleteId(@Param("athleteId") Long athleteId);

    /**
     * Lista los reportes activos de un tipo dado, del más reciente al más antiguo.
     */
    @Query("SELECT r FROM Report r WHERE r.reportType = :type AND r.deletedAt IS NULL "
            + "ORDER BY r.createdAt DESC")
    List<Report> findActiveByReportType(@Param("type") ReportType type);

    /**
     * Lista los reportes activos de un estado dado, del más reciente al más antiguo.
     */
    @Query("SELECT r FROM Report r WHERE r.status = :status AND r.deletedAt IS NULL "
            + "ORDER BY r.createdAt DESC")
    List<Report> findActiveByStatus(@Param("status") ReportStatus status);

    /**
     * Lista todos los reportes activos, del más reciente al más antiguo.
     */
    List<Report> findAllByDeletedAtIsNull();
}