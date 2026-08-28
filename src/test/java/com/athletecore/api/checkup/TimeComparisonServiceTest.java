package com.athletecore.api.checkup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.athletecore.api.checkup.dto.TimeComparisonResponse;
import com.athletecore.api.checkup.dto.TimeComparisonResponse.PositionComparison;
import com.athletecore.api.common.exception.DuplicateResourceException;
import com.athletecore.api.common.exception.ResourceNotFoundException;

/**
 * Tests unitarios de TimeComparisonService con datos fijos: la comparación es
 * aritmética pura y determinística (spec time-comparison, sin Clock).
 */
@ExtendWith(MockitoExtension.class)
class TimeComparisonServiceTest {

    private static final String CATEGORY = "MAYOR";
    private static final String STYLE = "LIBRE";
    private static final int DISTANCE = 100;

    @Mock
    private CheckupRepository checkupRepository;

    @Mock
    private CheckupTimeRepository checkupTimeRepository;

    @Mock
    private NationalReferenceTimeRepository nationalReferenceTimeRepository;

    @InjectMocks
    private TimeComparisonService service;

    private static NationalReferenceTime reference(long id, short position, String time) {
        return NationalReferenceTime.builder()
                .id(id)
                .style(STYLE)
                .distance(DISTANCE)
                .category(CATEGORY)
                .position(position)
                .timeSeconds(new BigDecimal(time))
                .build();
    }

    private static CheckupTime trial(String time) {
        return CheckupTime.builder()
                .id(1L)
                .style(STYLE)
                .distance(DISTANCE)
                .timeSeconds(new BigDecimal(time))
                .build();
    }

    private List<NationalReferenceTime> tripleCompleto() {
        return List.of(
                reference(1L, (short) 1, "62.000"),
                reference(2L, (short) 2, "63.500"),
                reference(3L, (short) 3, "65.000"));
    }

    @Test
    @DisplayName("Debe calcular diferencias absolutas negativas cuando la prueba es más rápida que el oro")
    void compareTrial_devuelveDiferenciasNegativas_cuandoTrialMasRapidoQueGold() {
        when(nationalReferenceTimeRepository.findActiveTripleByStyleDistanceCategory(
                STYLE, DISTANCE, CATEGORY)).thenReturn(tripleCompleto());

        TimeComparisonResponse response = service.compareTrial(trial("60.000"), CATEGORY);

        assertEquals(STYLE, response.style());
        assertEquals(DISTANCE, response.distance());
        assertEquals(CATEGORY, response.category());
        assertEquals(3, response.positions().size());
        assertEquals(0, response.positions().get(0).absoluteDiffSeconds().compareTo(new BigDecimal("-2.000")));
        assertEquals(0, response.positions().get(1).absoluteDiffSeconds().compareTo(new BigDecimal("-3.500")));
        assertEquals(0, response.positions().get(2).absoluteDiffSeconds().compareTo(new BigDecimal("-5.000")));
        assertTrue(response.positions().stream()
                .allMatch(position -> position.absoluteDiffSeconds().signum() < 0));
    }

    @Test
    @DisplayName("Debe calcular diferencias mixtas cuando la prueba queda entre dos posiciones")
    void compareTrial_devuelveDiferenciasMixtas_cuandoTrialEntreDosPosiciones() {
        when(nationalReferenceTimeRepository.findActiveTripleByStyleDistanceCategory(
                STYLE, DISTANCE, CATEGORY)).thenReturn(tripleCompleto());

        TimeComparisonResponse response = service.compareTrial(trial("64.000"), CATEGORY);

        assertEquals(3, response.positions().size());
        assertEquals(0, response.positions().get(0).absoluteDiffSeconds().compareTo(new BigDecimal("2.000")));
        assertEquals(0, response.positions().get(1).absoluteDiffSeconds().compareTo(new BigDecimal("0.500")));
        assertEquals(0, response.positions().get(2).absoluteDiffSeconds().compareTo(new BigDecimal("-1.000")));
        assertTrue(response.positions().get(0).absoluteDiffSeconds().signum() > 0);
        assertTrue(response.positions().get(1).absoluteDiffSeconds().signum() > 0);
        assertTrue(response.positions().get(2).absoluteDiffSeconds().signum() < 0);
    }

    @Test
    @DisplayName("Debe calcular diferencias positivas cuando la prueba es más lenta que el bronce")
    void compareTrial_devuelveDiferenciasPositivas_cuandoTrialMasLentoQueBronze() {
        when(nationalReferenceTimeRepository.findActiveTripleByStyleDistanceCategory(
                STYLE, DISTANCE, CATEGORY)).thenReturn(tripleCompleto());

        TimeComparisonResponse response = service.compareTrial(trial("70.000"), CATEGORY);

        assertEquals(3, response.positions().size());
        assertEquals(0, response.positions().get(0).absoluteDiffSeconds().compareTo(new BigDecimal("8.000")));
        assertEquals(0, response.positions().get(1).absoluteDiffSeconds().compareTo(new BigDecimal("6.500")));
        assertEquals(0, response.positions().get(2).absoluteDiffSeconds().compareTo(new BigDecimal("5.000")));
        assertTrue(response.positions().stream()
                .allMatch(position -> position.absoluteDiffSeconds().signum() > 0));
    }

    @Test
    @DisplayName("Debe lanzar DuplicateResourceException cuando falta el triple completo de referencia")
    void compareTrial_lanza409_cuandoFaltaTripleCompleto() {
        when(nationalReferenceTimeRepository.findActiveTripleByStyleDistanceCategory(
                STYLE, DISTANCE, CATEGORY)).thenReturn(List.of(
                        reference(1L, (short) 1, "62.000"),
                        reference(2L, (short) 2, "63.500")));

        DuplicateResourceException ex = assertThrows(DuplicateResourceException.class,
                () -> service.compareTrial(trial("60.000"), CATEGORY));

        assertTrue(ex.getMessage().contains(STYLE + " " + DISTANCE + "m"));
        assertTrue(ex.getMessage().contains(CATEGORY));
        assertTrue(ex.getMessage().contains("triple completo"));
    }

    @Test
    @DisplayName("Debe ser determinístico con el mismo input en llamadas repetidas")
    void compareTrial_esDeterministico_cuandoMismoInput() {
        when(nationalReferenceTimeRepository.findActiveTripleByStyleDistanceCategory(
                STYLE, DISTANCE, CATEGORY)).thenReturn(tripleCompleto());

        TimeComparisonResponse first = service.compareTrial(trial("64.000"), CATEGORY);
        TimeComparisonResponse second = service.compareTrial(trial("64.000"), CATEGORY);

        assertEquals(first, second);
        verify(nationalReferenceTimeRepository, times(2))
                .findActiveTripleByStyleDistanceCategory(STYLE, DISTANCE, CATEGORY);
    }

    @Test
    @DisplayName("Debe lanzar ResourceNotFoundException al comparar un chequeo inexistente")
    void compareCheckup_lanza404_cuandoChequeoNoExiste() {
        when(checkupRepository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> service.compareCheckup(99L));

        assertEquals("Checkup not found with id: '99'", ex.getMessage());
    }

    @Test
    @DisplayName("Debe devolver una comparación por cada tiempo registrado en el chequeo")
    void compareCheckup_devuelveComparacionesPorTiempo_cuandoChequeoTieneVariosTiempos() {
        Checkup checkup = Checkup.builder().id(1L).category(CATEGORY).build();
        CheckupTime time1 = CheckupTime.builder().style("LIBRE").distance(100)
                .timeSeconds(new BigDecimal("60.000")).build();
        CheckupTime time2 = CheckupTime.builder().style("LIBRE").distance(200)
                .timeSeconds(new BigDecimal("125.000")).build();
        CheckupTime time3 = CheckupTime.builder().style("ESPALDA").distance(100)
                .timeSeconds(new BigDecimal("66.000")).build();

        List<NationalReferenceTime> tripleLibre100 = List.of(
                reference(1L, (short) 1, "62.000"),
                reference(2L, (short) 2, "63.500"),
                reference(3L, (short) 3, "65.000"));
        List<NationalReferenceTime> tripleLibre200 = List.of(
                reference(4L, (short) 1, "120.000"),
                reference(5L, (short) 2, "123.000"),
                reference(6L, (short) 3, "126.000"));
        List<NationalReferenceTime> tripleEspalda100 = List.of(
                reference(7L, (short) 1, "63.000"),
                reference(8L, (short) 2, "64.500"),
                reference(9L, (short) 3, "65.500"));

        when(checkupRepository.findById(1L)).thenReturn(Optional.of(checkup));
        when(checkupTimeRepository.findActiveByCheckupId(1L)).thenReturn(List.of(time1, time2, time3));
        when(nationalReferenceTimeRepository.findActiveTripleByStyleDistanceCategory("LIBRE", 100, CATEGORY))
                .thenReturn(tripleLibre100);
        when(nationalReferenceTimeRepository.findActiveTripleByStyleDistanceCategory("LIBRE", 200, CATEGORY))
                .thenReturn(tripleLibre200);
        when(nationalReferenceTimeRepository.findActiveTripleByStyleDistanceCategory("ESPALDA", 100, CATEGORY))
                .thenReturn(tripleEspalda100);

        List<TimeComparisonResponse> result = service.compareCheckup(1L);

        assertEquals(3, result.size());
        assertEquals("LIBRE", result.get(0).style());
        assertEquals(100, result.get(0).distance());
        assertEquals("LIBRE", result.get(1).style());
        assertEquals(200, result.get(1).distance());
        assertEquals("ESPALDA", result.get(2).style());
        result.forEach(response -> assertEquals(3, response.positions().size()));
        verify(checkupTimeRepository, times(1)).findActiveByCheckupId(1L);
    }
}
