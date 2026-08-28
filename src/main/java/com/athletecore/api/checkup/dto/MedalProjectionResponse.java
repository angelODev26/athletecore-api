package com.athletecore.api.checkup.dto;

import java.math.BigDecimal;

import com.athletecore.api.checkup.Classification;
import com.athletecore.api.checkup.MedalProjection;
import com.athletecore.api.checkup.TimeFormatter;

/**
 * DTO de respuesta para una proyección de medallería.
 * Envuelve el record de dominio MedalProjection para exponerlo por HTTP;
 * diffVsBronzeSeconds negativo implica tiempo mejor (más rápido) que el bronce.
 */
public record MedalProjectionResponse(
    String style,
    Integer distance,
    String category,
    Classification classification,
    BigDecimal timeSeconds,
    String timeFormatted,
    BigDecimal diffVsBronzeSeconds,
    String diffVsBronzeFormatted
) {
    /**
     * Crea MedalProjectionResponse desde el record de dominio MedalProjection.
     *
     * @param projection Proyección de medallería calculada
     * @return MedalProjectionResponse
     */
    public static MedalProjectionResponse fromMedalProjection(MedalProjection projection) {
        return new MedalProjectionResponse(
            projection.style(),
            projection.distance(),
            projection.category(),
            projection.classification(),
            projection.timeSeconds(),
            TimeFormatter.toFormatted(projection.timeSeconds()),
            projection.diffVsBronzeSeconds(),
            TimeFormatter.toSignedFormatted(projection.diffVsBronzeSeconds())
        );
    }
}
