package com.athletecore.api.training;

/**
 * Estado de una sesión de entrenamiento.
 * Se persiste como texto (EnumType.STRING) en la columna status de training_sessions.
 */
public enum SessionStatus {
    /** Sesión programada (pendiente de ejecución). */
    PROGRAMADA,
    /** Sesión ejecutada. */
    EJECUTADA,
    /** Sesión cancelada. No admite registro de asistencia. */
    CANCELADA
}
