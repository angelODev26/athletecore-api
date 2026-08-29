package com.athletecore.api.report.dto;

import com.athletecore.api.report.ReportType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO de request para crear una programación automática de reportes.
 * La expresión cron se valida vía Spring CronExpression en el servicio (Fase C).
 */
public record CreateReportScheduleRequest(
    @NotNull(message = "El tipo de reporte es obligatorio")
    ReportType reportType,

    Long athleteId,

    String category,

    @NotBlank(message = "La expresión cron es obligatoria")
    @Size(max = 100, message = "La expresión cron no puede superar 100 caracteres")
    String cronExpression,

    @Size(max = 63, message = "La zona horaria no puede superar 63 caracteres")
    String timezone,

    Boolean active
) {
}