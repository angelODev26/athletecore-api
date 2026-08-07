package com.athletecore.api.checkup;

/**
 * Estilo de nado de una prueba.
 * Se valida en la entrada (DTO/servicio), no en base de datos, para permitir
 * extender estilos sin migración.
 */
public enum SwimmingStyle {
    /** Estilo libre (crol). */
    LIBRE,
    /** Estilo espalda. */
    ESPALDA,
    /** Estilo pecho. */
    BRAZA,
    /** Estilo mariposa. */
    MARIPOSA,
    /** Prueba combinada (estilos). */
    COMBINADO
}
