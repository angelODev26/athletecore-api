package com.athletecore.api.report;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repositorio para entidad ReportExport.
 * Las consultas activas filtran deleted_at IS NULL de forma explícita además
 * de la @SQLRestriction de la entidad.
 */
@Repository
public interface ReportExportRepository extends JpaRepository<ReportExport, Long> {

    /**
     * Lista los exports activos de un reporte, del más reciente al más antiguo.
     */
    @Query("SELECT e FROM ReportExport e WHERE e.report.id = :reportId AND e.deletedAt IS NULL "
            + "ORDER BY e.createdAt DESC")
    List<ReportExport> findActiveByReportId(@Param("reportId") Long reportId);
}