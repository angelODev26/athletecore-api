package com.athletecore.api.report;

/**
 * Tipo de reporte del módulo de reportes.
 * Se persiste como texto (EnumType.STRING) en la columna report_type.
 */
public enum ReportType {
    /** Reporte individual: consolida la evolución de un deportista. */
    INDIVIDUAL,
    /** Reporte general: comparativo de rendimiento por equipo/categoría. */
    GENERAL
}