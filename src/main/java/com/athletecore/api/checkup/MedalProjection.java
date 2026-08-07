package com.athletecore.api.checkup;

import java.math.BigDecimal;

/**
 * Proyección de medallería para un tiempo de prueba (style + distance) contra
 * la tabla nacional de referencia.
 * Es un record de dominio calculado en tiempo de consulta, NO una entidad JPA
 * (decisión D5): no persiste y se expone vía MedalProjectionResponse.
 */
public record MedalProjection(
    String style,
    Integer distance,
    String category,
    Classification classification,
    BigDecimal timeSeconds,
    BigDecimal diffVsBronzeSeconds
) {
}
