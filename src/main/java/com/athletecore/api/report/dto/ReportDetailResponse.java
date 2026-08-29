package com.athletecore.api.report.dto;

import java.time.Instant;
import java.util.List;

import com.athletecore.api.report.Report;
import com.athletecore.api.report.ReportStatus;
import com.athletecore.api.report.ReportType;

/**
 * DTO de respuesta de detalle de un reporte, con sus exports asociados.
 * No expone la entidad JPA directamente.
 */
public record ReportDetailResponse(
    Long id,
    ReportType reportType,
    Long athleteId,
    String category,
    Integer year,
    Integer month,
    String title,
    ReportStatus status,
    String errorMessage,
    Instant createdAt,
    Instant updatedAt,
    List<ReportExportResponse> exports
) {
    /**
     * Crea ReportDetailResponse desde una entidad Report y sus exports.
     *
     * @param report  Entidad Report
     * @param exports Lista de ReportExportResponse asociados
     * @return ReportDetailResponse
     */
    public static ReportDetailResponse fromReport(Report report, List<ReportExportResponse> exports) {
        return new ReportDetailResponse(
            report.getId(),
            report.getReportType(),
            report.getAthleteId(),
            report.getCategory(),
            report.getYear(),
            report.getMonth(),
            report.getTitle(),
            report.getStatus(),
            report.getErrorMessage(),
            report.getCreatedAt(),
            report.getUpdatedAt(),
            exports
        );
    }
}