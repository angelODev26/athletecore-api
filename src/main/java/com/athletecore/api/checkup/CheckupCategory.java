package com.athletecore.api.checkup;

/**
 * Categoría de competición de un chequeo mensual.
 * Se valida en la entrada (DTO/servicio), no en base de datos, para permitir
 * extender categorías sin migración (decisión D1).
 */
public enum CheckupCategory {
    /** Categoría infantil. */
    INFANTIL,
    /** Categoría juvenil. */
    JUVENIL,
    /** Categoría mayores. */
    MAYOR
}
