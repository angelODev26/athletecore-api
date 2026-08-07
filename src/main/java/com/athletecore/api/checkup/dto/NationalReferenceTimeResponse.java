package com.athletecore.api.checkup.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.athletecore.api.checkup.NationalReferenceTime;
import com.athletecore.api.checkup.TimeFormatter;

/**
 * DTO de respuesta detallado para un tiempo nacional de referencia (CRUD).
 * No expone la entidad JPA directamente.
 */
public record NationalReferenceTimeResponse(
    Long id,
    String style,
    Integer distance,
    String category,
    Short position,
    BigDecimal timeSeconds,
    String timeFormatted,
    Instant createdAt,
    Instant updatedAt
) {
    /**
     * Crea NationalReferenceTimeResponse desde una entidad NationalReferenceTime.
     *
     * @param referenceTime Entidad NationalReferenceTime
     * @return NationalReferenceTimeResponse
     */
    public static NationalReferenceTimeResponse fromNationalReferenceTime(NationalReferenceTime referenceTime) {
        return new NationalReferenceTimeResponse(
            referenceTime.getId(),
            referenceTime.getStyle(),
            referenceTime.getDistance(),
            referenceTime.getCategory(),
            referenceTime.getPosition(),
            referenceTime.getTimeSeconds(),
            TimeFormatter.toFormatted(referenceTime.getTimeSeconds()),
            referenceTime.getCreatedAt(),
            referenceTime.getUpdatedAt()
        );
    }
}
