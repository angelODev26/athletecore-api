package com.athletecore.api.checkup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.athletecore.api.athlete.Athlete;
import com.athletecore.api.athlete.AthleteRepository;
import com.athletecore.api.common.exception.DuplicateResourceException;
import com.athletecore.api.common.exception.ResourceNotFoundException;

/**
 * Tests unitarios de MedalProjectionService: proyección de medallería por
 * atleta con clasificación determinística, cache de triples por consulta y
 * ausencia total de persistencia (solo lectura).
 */
@ExtendWith(MockitoExtension.class)
class MedalProjectionServiceTest {

    private static final Long ATHLETE_ID = 7L;
    private static final String CATEGORY = "MAYOR";

    @Mock
    private AthleteRepository athleteRepository;

    @Mock
    private CheckupRepository checkupRepository;

    @Mock
    private CheckupTimeRepository checkupTimeRepository;

    @Mock
    private TimeComparisonService timeComparisonService;

    private MedalProjectionService service;

    private Athlete athlete;

    @BeforeEach
    void setUp() {
        ClassificationService classificationService = new ClassificationService(new BigDecimal("1.500"));
        service = new MedalProjectionService(athleteRepository, checkupRepository, checkupTimeRepository,
                timeComparisonService, classificationService);
        athlete = Athlete.builder().id(ATHLETE_ID).firstName("Juan").lastName("Perez").build();
    }

    private static NationalReferenceTime reference(long id, short position, String time) {
        return NationalReferenceTime.builder()
                .id(id).style("LIBRE").distance(100).category(CATEGORY)
                .position(position).timeSeconds(new BigDecimal(time))
                .build();
    }

    private static CheckupTime time(long id, String style, int distance, String time) {
        return CheckupTime.builder()
                .id(id).style(style).distance(distance)
                .timeSeconds(new BigDecimal(time))
                .build();
    }

    @Test
    @DisplayName("Debe devolver una proyección por cada tiempo registrado en los chequeos del atleta")
    void getProjectionsForAthlete_devuelve3Proyecciones_cuandoAtletaTiene3Tiempos() {
        Checkup checkup = Checkup.builder().id(1L).athlete(athlete).category(CATEGORY).build();
        List<CheckupTime> times = List.of(
                time(11L, "LIBRE", 100, "60.000"),
                time(12L, "LIBRE", 200, "130.000"),
                time(13L, "ESPALDA", 100, "66.000"));

        when(athleteRepository.findById(ATHLETE_ID)).thenReturn(Optional.of(athlete));
        when(checkupRepository.findActiveByAthleteId(ATHLETE_ID)).thenReturn(List.of(checkup));
        when(checkupTimeRepository.findActiveByCheckupId(1L)).thenReturn(times);
        when(timeComparisonService.loadReferenceTriple("LIBRE", 100, CATEGORY))
                .thenReturn(List.of(reference(1L, (short) 1, "62.000"),
                        reference(2L, (short) 2, "63.500"), reference(3L, (short) 3, "65.000")));
        when(timeComparisonService.loadReferenceTriple("LIBRE", 200, CATEGORY))
                .thenReturn(List.of(reference(4L, (short) 1, "120.000"),
                        reference(5L, (short) 2, "125.000"), reference(6L, (short) 3, "130.000")));
        when(timeComparisonService.loadReferenceTriple("ESPALDA", 100, CATEGORY))
                .thenReturn(List.of(reference(7L, (short) 1, "63.000"),
                        reference(8L, (short) 2, "64.500"), reference(9L, (short) 3, "65.500")));

        List<MedalProjection> projections = service.getProjectionsForAthlete(ATHLETE_ID);

        assertEquals(3, projections.size());
        MedalProjection first = projections.get(0);
        assertEquals("LIBRE", first.style());
        assertEquals(100, first.distance());
        assertEquals(CATEGORY, first.category());
        assertEquals(Classification.POR_ENCIMA_DEL_PODIO, first.classification());
        assertEquals(new BigDecimal("60.000"), first.timeSeconds());
        assertEquals(0, first.diffVsBronzeSeconds().compareTo(new BigDecimal("-5.000")));

        MedalProjection second = projections.get(1);
        assertEquals("LIBRE", second.style());
        assertEquals(200, second.distance());
        assertEquals(Classification.POR_ENCIMA_DEL_PODIO, second.classification());
        assertEquals(0, second.diffVsBronzeSeconds().compareTo(BigDecimal.ZERO));

        MedalProjection third = projections.get(2);
        assertEquals("ESPALDA", third.style());
        assertEquals(100, third.distance());
        assertEquals(Classification.CERCANO_A_MEDALLERIA, third.classification());
        assertEquals(0, third.diffVsBronzeSeconds().compareTo(new BigDecimal("0.500")));
    }

    @Test
    @DisplayName("Debe devolver lista vacía cuando el atleta no tiene chequeos activos")
    void getProjectionsForAthlete_devuelveListaVacia_cuandoAtletaSinChequeos() {
        when(athleteRepository.findById(ATHLETE_ID)).thenReturn(Optional.of(athlete));
        when(checkupRepository.findActiveByAthleteId(ATHLETE_ID)).thenReturn(List.of());

        List<MedalProjection> projections = service.getProjectionsForAthlete(ATHLETE_ID);

        assertTrue(projections.isEmpty());
        verify(checkupTimeRepository, never()).findActiveByCheckupId(anyLong());
        verify(timeComparisonService, never()).loadReferenceTriple(anyString(), anyInt(), anyString());
    }

    @Test
    @DisplayName("Debe lanzar ResourceNotFoundException cuando el atleta no existe")
    void getProjectionsForAthlete_lanza404_cuandoAtletaNoExiste() {
        when(athleteRepository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> service.getProjectionsForAthlete(99L));

        assertEquals("Athlete not found with id: '99'", ex.getMessage());
        verify(checkupRepository, never()).findActiveByAthleteId(anyLong());
    }

    @Test
    @DisplayName("Debe calcular proyecciones sin persistir nada en ningún repositorio")
    void getProjectionsForAthlete_noPersiste_verificaQueSaveNuncaSeLlama() {
        Checkup checkup = Checkup.builder().id(1L).athlete(athlete).category(CATEGORY).build();
        when(athleteRepository.findById(ATHLETE_ID)).thenReturn(Optional.of(athlete));
        when(checkupRepository.findActiveByAthleteId(ATHLETE_ID)).thenReturn(List.of(checkup));
        when(checkupTimeRepository.findActiveByCheckupId(1L)).thenReturn(
                List.of(time(11L, "LIBRE", 100, "60.000")));
        when(timeComparisonService.loadReferenceTriple("LIBRE", 100, CATEGORY))
                .thenReturn(List.of(reference(1L, (short) 1, "62.000"),
                        reference(2L, (short) 2, "63.500"), reference(3L, (short) 3, "65.000")));

        List<MedalProjection> projections = service.getProjectionsForAthlete(ATHLETE_ID);

        assertEquals(1, projections.size());
        verify(athleteRepository, never()).save(any());
        verify(checkupRepository, never()).save(any());
        verify(checkupTimeRepository, never()).save(any());
        verify(checkupTimeRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Debe cargar el triple de referencia una sola vez por (estilo, distancia, categoría)")
    void getProjectionsForAthlete_cargaTriplesUnaSolaVezPorTupla_cuandoMismoStyleDistanceCategoriaEnMultiplesChequeos() {
        Checkup checkup1 = Checkup.builder().id(1L).athlete(athlete).category(CATEGORY).build();
        Checkup checkup2 = Checkup.builder().id(2L).athlete(athlete).category(CATEGORY).build();
        when(athleteRepository.findById(ATHLETE_ID)).thenReturn(Optional.of(athlete));
        when(checkupRepository.findActiveByAthleteId(ATHLETE_ID)).thenReturn(List.of(checkup1, checkup2));
        when(checkupTimeRepository.findActiveByCheckupId(1L)).thenReturn(
                List.of(time(11L, "LIBRE", 100, "60.000")));
        when(checkupTimeRepository.findActiveByCheckupId(2L)).thenReturn(
                List.of(time(21L, "LIBRE", 100, "61.000")));
        when(timeComparisonService.loadReferenceTriple("LIBRE", 100, CATEGORY))
                .thenReturn(List.of(reference(1L, (short) 1, "62.000"),
                        reference(2L, (short) 2, "63.500"), reference(3L, (short) 3, "65.000")));

        List<MedalProjection> projections = service.getProjectionsForAthlete(ATHLETE_ID);

        assertEquals(2, projections.size());
        verify(timeComparisonService, times(1)).loadReferenceTriple("LIBRE", 100, CATEGORY);
    }

    @Test
    @DisplayName("Debe lanzar DuplicateResourceException cuando un tiempo no tiene triple completo")
    void getProjectionsForAthlete_lanza409_cuandoUnTiempoNoTieneTripleCompleto() {
        Checkup checkup = Checkup.builder().id(1L).athlete(athlete).category(CATEGORY).build();
        when(athleteRepository.findById(ATHLETE_ID)).thenReturn(Optional.of(athlete));
        when(checkupRepository.findActiveByAthleteId(ATHLETE_ID)).thenReturn(List.of(checkup));
        when(checkupTimeRepository.findActiveByCheckupId(1L)).thenReturn(
                List.of(time(11L, "LIBRE", 100, "60.000")));
        when(timeComparisonService.loadReferenceTriple("LIBRE", 100, CATEGORY))
                .thenReturn(List.of(reference(1L, (short) 1, "62.000"),
                        reference(2L, (short) 2, "63.500")));

        DuplicateResourceException ex = assertThrows(DuplicateResourceException.class,
                () -> service.getProjectionsForAthlete(ATHLETE_ID));

        assertTrue(ex.getMessage().contains("3° puesto"));
        assertTrue(ex.getMessage().contains("LIBRE|100|MAYOR"));
    }

    @Test
    @DisplayName("Debe agrupar las proyecciones por atleta en una sola pasada en lote")
    void getProjectionsForAllAthletes_agrupaPorAtleta_enLote() {
        Athlete otro = Athlete.builder().id(2L).firstName("Maria").lastName("Lopez").build();
        Checkup checkup1 = Checkup.builder().id(1L).athlete(athlete).category(CATEGORY).build();
        Checkup checkup2 = Checkup.builder().id(2L).athlete(otro).category(CATEGORY).build();
        CheckupTime ct1 = CheckupTime.builder().id(11L).checkup(checkup1)
                .style("LIBRE").distance(100).timeSeconds(new BigDecimal("60.000")).build();
        CheckupTime ct2 = CheckupTime.builder().id(21L).checkup(checkup2)
                .style("LIBRE").distance(100).timeSeconds(new BigDecimal("61.000")).build();

        when(checkupRepository.findAllActive()).thenReturn(List.of(checkup1, checkup2));
        when(checkupTimeRepository.findAllActive()).thenReturn(List.of(ct1, ct2));
        when(timeComparisonService.loadReferenceTriple("LIBRE", 100, CATEGORY))
                .thenReturn(List.of(reference(1L, (short) 1, "62.000"),
                        reference(2L, (short) 2, "63.500"), reference(3L, (short) 3, "65.000")));

        Map<Long, List<MedalProjection>> result = service.getProjectionsForAllAthletes();

        assertEquals(2, result.size());
        assertTrue(result.containsKey(ATHLETE_ID));
        assertTrue(result.containsKey(2L));
        assertEquals(1, result.get(ATHLETE_ID).size());
        assertEquals(1, result.get(2L).size());
        verify(timeComparisonService, times(1)).loadReferenceTriple("LIBRE", 100, CATEGORY);
    }
}
