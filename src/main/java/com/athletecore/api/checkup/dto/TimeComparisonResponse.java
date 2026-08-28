package com.athletecore.api.checkup.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

import com.athletecore.api.checkup.CheckupTime;
import com.athletecore.api.checkup.NationalReferenceTime;
import com.athletecore.api.checkup.TimeFormatter;

/**
 * DTO de respuesta de la comparación de un tiempo de prueba contra el triple
 * nacional de referencia (1°, 2°, 3° puesto).
 * Diferencia absoluta = tiempo de prueba - tiempo de referencia: negativo
 * implica que la prueba es más rápida que la referencia.
 * Diferencia relativa = porcentaje de la diferencia absoluta sobre la
 * referencia ((prueba - referencia) / referencia * 100).
 */
public record TimeComparisonResponse(
    String style,
    Integer distance,
    String category,
    BigDecimal trialTimeSeconds,
    String trialTimeFormatted,
    List<PositionComparison> positions
) {

    private static final int SCALE = 3;

    /**
     * Comparación por posición de medallería (1=oro, 2=plata, 3=bronce).
     */
    public record PositionComparison(
        Short position,
        BigDecimal referenceTimeSeconds,
        String referenceTimeFormatted,
        BigDecimal absoluteDiffSeconds,
        String absoluteDiffFormatted,
        BigDecimal relativeDiffPercent
    ) {
    }

    /**
     * Construye la comparación desde el tiempo de prueba, la categoría del
     * chequeo y el triple de referencias activas (1°, 2°, 3°).
     *
     * @param trial      Tiempo de prueba registrado
     * @param category   Categoría de competición del chequeo
     * @param references Triple de NationalReferenceTime activos, una por posición
     * @return TimeComparisonResponse
     */
    public static TimeComparisonResponse fromCheckupTimeAndReferences(
            CheckupTime trial, String category, List<NationalReferenceTime> references) {
        List<PositionComparison> positionComparisons = references.stream()
                .sorted(Comparator.comparing(NationalReferenceTime::getPosition))
                .map(ref -> {
                    BigDecimal diff = trial.getTimeSeconds().subtract(ref.getTimeSeconds());
                    return new PositionComparison(
                        ref.getPosition(),
                        ref.getTimeSeconds(),
                        TimeFormatter.toFormatted(ref.getTimeSeconds()),
                        diff,
                        TimeFormatter.toSignedFormatted(diff),
                        relativeDiffPercent(trial.getTimeSeconds(), ref.getTimeSeconds())
                    );
                })
                .toList();
        return new TimeComparisonResponse(
            trial.getStyle(),
            trial.getDistance(),
            category,
            trial.getTimeSeconds(),
            TimeFormatter.toFormatted(trial.getTimeSeconds()),
            positionComparisons
        );
    }

    private static BigDecimal relativeDiffPercent(BigDecimal trial, BigDecimal reference) {
        return trial.subtract(reference)
                .multiply(BigDecimal.valueOf(100))
                .divide(reference, SCALE, RoundingMode.HALF_UP);
    }
}
