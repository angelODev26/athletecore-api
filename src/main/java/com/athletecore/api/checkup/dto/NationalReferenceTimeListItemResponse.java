package com.athletecore.api.checkup.dto;

import java.math.BigDecimal;

import com.athletecore.api.checkup.NationalReferenceTime;
import com.athletecore.api.checkup.TimeFormatter;

/**
 * DTO de respuesta compacto para listados de la tabla nacional de referencia.
 * Omite los timestamps de auditoría; el CRUD completo usa
 * NationalReferenceTimeResponse.
 */
public record NationalReferenceTimeListItemResponse(
    Long id,
    String style,
    Integer distance,
    String category,
    Short position,
    BigDecimal timeSeconds,
    String timeFormatted
) {
    /**
     * Crea NationalReferenceTimeListItemResponse desde una entidad NationalReferenceTime.
     *
     * @param referenceTime Entidad NationalReferenceTime
     * @return NationalReferenceTimeListItemResponse
     */
    public static NationalReferenceTimeListItemResponse fromNationalReferenceTime(NationalReferenceTime referenceTime) {
        return new NationalReferenceTimeListItemResponse(
            referenceTime.getId(),
            referenceTime.getStyle(),
            referenceTime.getDistance(),
            referenceTime.getCategory(),
            referenceTime.getPosition(),
            referenceTime.getTimeSeconds(),
            TimeFormatter.toFormatted(referenceTime.getTimeSeconds())
        );
    }
}
