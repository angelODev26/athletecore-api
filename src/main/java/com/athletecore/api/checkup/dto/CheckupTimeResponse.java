package com.athletecore.api.checkup.dto;

import java.math.BigDecimal;

import com.athletecore.api.checkup.CheckupTime;
import com.athletecore.api.checkup.TimeFormatter;

/**
 * DTO de respuesta para un tiempo de prueba.
 * Expone timeSeconds (numérico) y timeFormatted (mm:ss.ms) para que el cliente
 * elija el formato; el formateo es responsabilidad de la capa de presentación.
 */
public record CheckupTimeResponse(
    Long id,
    Long checkupId,
    String style,
    Integer distance,
    BigDecimal timeSeconds,
    String timeFormatted
) {
    /**
     * Crea CheckupTimeResponse desde una entidad CheckupTime.
     *
     * @param checkupTime Entidad CheckupTime
     * @return CheckupTimeResponse
     */
    public static CheckupTimeResponse fromCheckupTime(CheckupTime checkupTime) {
        return new CheckupTimeResponse(
            checkupTime.getId(),
            checkupTime.getCheckup().getId(),
            checkupTime.getStyle(),
            checkupTime.getDistance(),
            checkupTime.getTimeSeconds(),
            TimeFormatter.toFormatted(checkupTime.getTimeSeconds())
        );
    }
}
