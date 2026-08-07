package com.athletecore.api.checkup;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Servicio de clasificación relativa de un tiempo de prueba contra el 3° puesto
 * de la tabla nacional. Lógica pura y determinística a partir de los valores
 * numéricos y del umbral configurable {@code checkup.medal-proximity-threshold-seconds}:
 * <ul>
 *   <li>{@code trialSeconds <= bronzeSeconds} → POR_ENCIMA_DEL_PODIO</li>
 *   <li>{@code 0 < trialSeconds - bronzeSeconds <= umbral} → CERCANO_A_MEDALLERIA</li>
 *   <li>en otro caso → FUERA_DE_RANGO</li>
 * </ul>
 */
@Service
public class ClassificationService {

    private final BigDecimal proximityThresholdSeconds;

    /**
     * @param proximityThresholdSeconds Umbral de proximidad en segundos
     *        (default 1.500; override por env var sin recodificar).
     */
    public ClassificationService(
            @Value("${checkup.medal-proximity-threshold-seconds:1.500}") BigDecimal proximityThresholdSeconds) {
        this.proximityThresholdSeconds = proximityThresholdSeconds;
    }

    /**
     * Clasifica un tiempo de prueba contra el tiempo de bronce.
     *
     * @param trialSeconds  Tiempo de prueba en segundos
     * @param bronzeSeconds Tiempo de referencia del 3° puesto en segundos
     * @return Clasificación determinística
     */
    public Classification classify(BigDecimal trialSeconds, BigDecimal bronzeSeconds) {
        BigDecimal diff = trialSeconds.subtract(bronzeSeconds);
        if (diff.compareTo(BigDecimal.ZERO) <= 0) {
            return Classification.POR_ENCIMA_DEL_PODIO;
        }
        if (diff.compareTo(proximityThresholdSeconds) <= 0) {
            return Classification.CERCANO_A_MEDALLERIA;
        }
        return Classification.FUERA_DE_RANGO;
    }
}
