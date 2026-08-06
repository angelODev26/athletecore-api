package com.athletecore.api.training;

/**
 * Tipo de ciclo de planificación dentro de un plan anual.
 * Se persiste como texto (EnumType.STRING) en la columna type de training_cycles.
 */
public enum CycleType {
    /** Mesociclo: bloque de planificación a mediano plazo (puede contener microciclos). */
    MESOCICLO,
    /** Microciclo: ciclo corto, siempre hijo de un mesociclo. */
    MICROCICLO
}
