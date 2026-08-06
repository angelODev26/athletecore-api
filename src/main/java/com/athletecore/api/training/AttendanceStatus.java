package com.athletecore.api.training;

/**
 * Estado de asistencia de un deportista a una sesión.
 * Se persiste como texto (EnumType.STRING) en la columna status de attendance.
 */
public enum AttendanceStatus {
    /** Deportista presente en la sesión. */
    PRESENTE,
    /** Deportista ausente sin justificación. */
    AUSENTE,
    /** Ausencia justificada (no rompe el cálculo de ausencias consecutivas). */
    JUSTIFICADO
}
