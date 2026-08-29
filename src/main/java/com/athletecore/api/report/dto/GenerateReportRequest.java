package com.athletecore.api.report.dto;

import com.athletecore.api.report.ReportType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO de request para generar un reporte (individual o general).
 * Los filtros athleteId/category/year/month son opcionales según el tipo:
 * un reporte INDIVIDUAL requiere athleteId; un reporte GENERAL puede filtrar
 * por category.
 */
public record GenerateReportRequest(
    @NotNull(message = "El tipo de reporte es obligatorio")
    ReportType reportType,

    Long athleteId,

    String category,

    Integer year,

    Integer month,

    @NotBlank(message = "El título es obligatorio")
    @Size(max = 200, message = "El título no puede superar 200 caracteres")
    String title
) {
}