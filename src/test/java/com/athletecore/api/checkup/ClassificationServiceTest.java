package com.athletecore.api.checkup;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests unitarios de ClassificationService: lógica pura y determinística con
 * umbral configurable por constructor (sin Spring, sin repositorios).
 */
class ClassificationServiceTest {

    private static final BigDecimal BRONZE = new BigDecimal("65.000");

    private ClassificationService serviceWithThreshold(String threshold) {
        return new ClassificationService(new BigDecimal(threshold));
    }

    @Test
    @DisplayName("Debe clasificar POR_ENCIMA_DEL_PODIO cuando la prueba es menor al bronce")
    void classify_devuelvePorEncimaDelPodio_cuandoTrialMenorOIgualQueBronze() {
        ClassificationService service = serviceWithThreshold("1.500");

        Classification result = service.classify(new BigDecimal("64.000"), BRONZE);

        assertEquals(Classification.POR_ENCIMA_DEL_PODIO, result);
    }

    @Test
    @DisplayName("Debe clasificar POR_ENCIMA_DEL_PODIO cuando la prueba iguala el bronce")
    void classify_devuelvePorEncimaDelPodio_cuandoTrialIgualQueBronce() {
        ClassificationService service = serviceWithThreshold("1.500");

        Classification result = service.classify(new BigDecimal("65.000"), BRONZE);

        assertEquals(Classification.POR_ENCIMA_DEL_PODIO, result);
    }

    @Test
    @DisplayName("Debe clasificar CERCANO_A_MEDALLERIA cuando la diferencia es menor al umbral")
    void classify_devuelveCercanoAMedalleria_cuandoDiffMenorOIgualQueThreshold() {
        ClassificationService service = serviceWithThreshold("1.500");

        Classification result = service.classify(new BigDecimal("65.800"), BRONZE);

        assertEquals(Classification.CERCANO_A_MEDALLERIA, result);
    }

    @Test
    @DisplayName("Debe clasificar CERCANO_A_MEDALLERIA cuando la diferencia iguala el umbral")
    void classify_devuelveCercanoAMedalleria_cuandoDiffIgualAlUmbral() {
        ClassificationService service = serviceWithThreshold("1.500");

        Classification result = service.classify(new BigDecimal("66.500"), BRONZE);

        assertEquals(Classification.CERCANO_A_MEDALLERIA, result);
    }

    @Test
    @DisplayName("Debe clasificar FUERA_DE_RANGO cuando la diferencia supera el umbral")
    void classify_devuelveFueraDeRango_cuandoDiffMayorQueThreshold() {
        ClassificationService service = serviceWithThreshold("1.500");

        Classification result = service.classify(new BigDecimal("67.000"), BRONZE);

        assertEquals(Classification.FUERA_DE_RANGO, result);
    }

    @Test
    @DisplayName("Debe respetar el cambio de umbral inyectado por constructor")
    void classify_respetaCambioDeThreshold_cuandoSeInyectaThresholdDistinto() {
        ClassificationService serviceDefault = serviceWithThreshold("1.500");
        ClassificationService serviceAmpliado = serviceWithThreshold("2.000");
        BigDecimal trial = new BigDecimal("66.800");

        assertEquals(Classification.FUERA_DE_RANGO, serviceDefault.classify(trial, BRONZE));
        assertEquals(Classification.CERCANO_A_MEDALLERIA, serviceAmpliado.classify(trial, BRONZE));
    }

    @Test
    @DisplayName("Debe ser determinístico con el mismo input en llamadas repetidas")
    void classify_esDeterministico_cuandoMismoInput() {
        ClassificationService service = serviceWithThreshold("1.500");

        Classification first = service.classify(new BigDecimal("65.800"), BRONZE);
        Classification second = service.classify(new BigDecimal("65.800"), BRONZE);
        Classification third = service.classify(new BigDecimal("65.800"), BRONZE);

        assertEquals(Classification.CERCANO_A_MEDALLERIA, first);
        assertEquals(first, second);
        assertEquals(second, third);
    }
}
