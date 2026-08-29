package com.athletecore.api.report.dto;

import java.time.Instant;

import com.athletecore.api.report.Report;
import com.athletecore.api.report.ReportStatus;
import com.athletecore.api.report.ReportType;

/**
 * DTO de respuesta para un reporte (sin sus exports embebidos).
 * No expone la entidad JPA directamente.
 */
public record ReportResponse(
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
    boolean active
) {
    /**
     * Crea ReportResponse desde una entidad Report.
     *
     * @param report Entidad Report
     * @return ReportResponse
     */
    public static ReportResponse fromReport(Report report) {
        return new ReportResponse(
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
            report.isActive()
        );
    }
}