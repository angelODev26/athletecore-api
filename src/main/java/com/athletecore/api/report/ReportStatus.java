package com.athletecore.api.report;

/**
 * Estado del ciclo de vida de un reporte generado.
 * Se persiste como texto (EnumType.STRING) en la columna status.
 */
public enum ReportStatus {
    /** Reporte creado, pendiente de generación. */
    PENDING,
    /** Reporte generado correctamente (con al menos un export). */
    GENERATED,
    /** La generación falló (mensaje en error_message). */
    FAILED
}