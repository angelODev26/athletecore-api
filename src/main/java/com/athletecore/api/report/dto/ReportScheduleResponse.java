package com.athletecore.api.report.dto;

import java.time.Instant;

import com.athletecore.api.report.ReportSchedule;
import com.athletecore.api.report.ReportType;

/**
 * DTO de respuesta para una programación automática de reportes.
 * No expone la entidad JPA directamente.
 */
public record ReportScheduleResponse(
    Long id,
    ReportType reportType,
    Long athleteId,
    String category,
    String cronExpression,
    String timezone,
    Boolean active,
    Instant lastRunAt,
    Instant nextRunAt,
    Instant createdAt,
    Instant updatedAt
) {
    /**
     * Crea ReportScheduleResponse desde una entidad ReportSchedule.
     *
     * @param schedule Entidad ReportSchedule
     * @return ReportScheduleResponse
     */
    public static ReportScheduleResponse fromSchedule(ReportSchedule schedule) {
        return new ReportScheduleResponse(
            schedule.getId(),
            schedule.getReportType(),
            schedule.getAthleteId(),
            schedule.getCategory(),
            schedule.getCronExpression(),
            schedule.getTimezone(),
            schedule.getActive(),
            schedule.getLastRunAt(),
            schedule.getNextRunAt(),
            schedule.getCreatedAt(),
            schedule.getUpdatedAt()
        );
    }
}