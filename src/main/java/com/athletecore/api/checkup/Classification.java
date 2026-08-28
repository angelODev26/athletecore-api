package com.athletecore.api.checkup;

/**
 * Clasificación relativa de un tiempo de prueba contra el 3° puesto de la
 * tabla nacional de referencia (umbral configurable en Fase C).
 */
public enum Classification {
    /** Tiempo igual o mejor que el 3° puesto (dentro del podio). */
    POR_ENCIMA_DEL_PODIO,
    /** Tiempo dentro del umbral de proximidad por detrás del 3° puesto. */
    CERCANO_A_MEDALLERIA,
    /** Tiempo fuera del umbral de proximidad al podio. */
    FUERA_DE_RANGO
}
