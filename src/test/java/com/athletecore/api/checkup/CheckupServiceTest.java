package com.athletecore.api.checkup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.athletecore.api.athlete.Athlete;
import com.athletecore.api.athlete.AthleteRepository;
import com.athletecore.api.checkup.dto.AddCheckupTimeRequest;
import com.athletecore.api.checkup.dto.CreateCheckupRequest;
import com.athletecore.api.common.exception.DuplicateResourceException;
import com.athletecore.api.common.exception.ResourceNotFoundException;

/**
 * Tests unitarios de CheckupService: registro de chequeos, agregado de tiempos,
 * lectura y soft delete atómico (chequeo + tiempos con la misma marca temporal).
 */
@ExtendWith(MockitoExtension.class)
class CheckupServiceTest {

    private static final Long ATHLETE_ID = 7L;
    private static final Long CHECKUP_ID = 1L;

    @Mock
    private CheckupRepository checkupRepository;

    @Mock
    private CheckupTimeRepository checkupTimeRepository;

    @Mock
    private AthleteRepository athleteRepository;

    @InjectMocks
    private CheckupService checkupService;

    private Athlete athlete;
    private Checkup checkup;
    private CreateCheckupRequest createRequest;

    @BeforeEach
    void setUp() {
        athlete = Athlete.builder().id(ATHLETE_ID).firstName("Juan").lastName("Perez").build();
        checkup = Checkup.builder()
                .id(CHECKUP_ID)
                .athlete(athlete)
                .year(2026)
                .month(8)
                .category("MAYOR")
                .notes("Chequeo mensual")
                .build();
        createRequest = new CreateCheckupRequest(ATHLETE_ID, 2026, 8, "MAYOR", "Chequeo mensual");
    }

    @Test
    @DisplayName("Debe crear y persistir un chequeo cuando el atleta existe y no hay duplicado")
    void crearChequeo_devuelveChequeoPersistido_cuandoAtletaExisteYSinDuplicado() {
        when(athleteRepository.findById(ATHLETE_ID)).thenReturn(Optional.of(athlete));
        when(checkupRepository.existsActiveByAthleteIdAndYearMonthAndCategory(
                ATHLETE_ID, 2026, 8, "MAYOR")).thenReturn(false);
        when(checkupRepository.save(any(Checkup.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Checkup result = checkupService.createCheckup(ATHLETE_ID, createRequest);

        assertNotNull(result);
        assertEquals(ATHLETE_ID, result.getAthlete().getId());
        assertEquals(2026, result.getYear());
        assertEquals(8, result.getMonth());
        assertEquals("MAYOR", result.getCategory());
        assertEquals("Chequeo mensual", result.getNotes());
        verify(checkupRepository, times(1)).save(any(Checkup.class));
    }

    @Test
    @DisplayName("Debe lanzar ResourceNotFoundException al crear un chequeo de un atleta inexistente")
    void crearChequeo_lanza404_cuandoAtletaNoExiste() {
        when(athleteRepository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> checkupService.createCheckup(99L, createRequest));

        assertEquals("Athlete not found with id: '99'", ex.getMessage());
        verify(checkupRepository, never()).save(any(Checkup.class));
    }

    @Test
    @DisplayName("Debe lanzar DuplicateResourceException si ya existe chequeo activo para (atleta, año, mes, categoría)")
    void crearChequeo_lanza409_cuandoYaExisteChequeoActivoParaAtletaAnoMesCategoria() {
        when(athleteRepository.findById(ATHLETE_ID)).thenReturn(Optional.of(athlete));
        when(checkupRepository.existsActiveByAthleteIdAndYearMonthAndCategory(
                ATHLETE_ID, 2026, 8, "MAYOR")).thenReturn(true);

        DuplicateResourceException ex = assertThrows(DuplicateResourceException.class,
                () -> checkupService.createCheckup(ATHLETE_ID, createRequest));

        assertEquals("Checkup with athleteId/year/month/category already exists", ex.getMessage());
        verify(checkupRepository, never()).save(any(Checkup.class));
    }

    @Test
    @DisplayName("Debe devolver el chequeo existente al consultar por ID")
    void getCheckupById_devuelveChequeo_cuandoExiste() {
        when(checkupRepository.findById(CHECKUP_ID)).thenReturn(Optional.of(checkup));

        Checkup result = checkupService.getCheckupById(CHECKUP_ID);

        assertNotNull(result);
        assertEquals(CHECKUP_ID, result.getId());
        assertEquals("MAYOR", result.getCategory());
    }

    @Test
    @DisplayName("Debe lanzar ResourceNotFoundException al consultar un chequeo inexistente")
    void getCheckupById_lanza404_cuandoNoExiste() {
        when(checkupRepository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> checkupService.getCheckupById(99L));

        assertEquals("Checkup not found with id: '99'", ex.getMessage());
    }

    @Test
    @DisplayName("Debe filtrar por año y mes cuando ambos filtros son provistos")
    void getCheckupsByAthlete_devuelveListaFiltrada_cuandoYearYMonthProveidos() {
        when(athleteRepository.findById(ATHLETE_ID)).thenReturn(Optional.of(athlete));
        when(checkupRepository.findActiveByAthleteIdAndYearMonth(ATHLETE_ID, 2026, 8))
                .thenReturn(List.of(checkup));

        List<Checkup> result = checkupService.getCheckupsByAthlete(ATHLETE_ID, 2026, 8);

        assertEquals(1, result.size());
        assertEquals(CHECKUP_ID, result.get(0).getId());
        verify(checkupRepository, times(1)).findActiveByAthleteIdAndYearMonth(ATHLETE_ID, 2026, 8);
        verify(checkupRepository, never()).findActiveByAthleteId(anyLong());
    }

    @Test
    @DisplayName("Debe devolver todos los chequeos del atleta cuando falta año o mes")
    void getCheckupsByAthlete_devuelveListaCompleta_cuandoSoloOYearOMonthProveido() {
        when(athleteRepository.findById(ATHLETE_ID)).thenReturn(Optional.of(athlete));
        when(checkupRepository.findActiveByAthleteId(ATHLETE_ID)).thenReturn(List.of(checkup));

        List<Checkup> result = checkupService.getCheckupsByAthlete(ATHLETE_ID, 2026, null);

        assertEquals(1, result.size());
        verify(checkupRepository, times(1)).findActiveByAthleteId(ATHLETE_ID);
        verify(checkupRepository, never()).findActiveByAthleteIdAndYearMonth(anyLong(), any(), any());
    }

    @Test
    @DisplayName("Debe lanzar ResourceNotFoundException al listar chequeos de un atleta inexistente")
    void getCheckupsByAthlete_lanza404_cuandoAtletaNoExiste() {
        when(athleteRepository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> checkupService.getCheckupsByAthlete(99L, 2026, 8));

        assertEquals("Athlete not found with id: '99'", ex.getMessage());
        verify(checkupRepository, never()).findActiveByAthleteId(anyLong());
    }

    @Test
    @DisplayName("Debe devolver los tiempos activos de un chequeo")
    void getTimesByAthlete_devuelveListaDeTiempos_cuandoChequeoExiste() {
        CheckupTime time = CheckupTime.builder()
                .id(11L)
                .checkup(checkup)
                .style("LIBRE")
                .distance(100)
                .timeSeconds(new BigDecimal("65.250"))
                .build();
        when(checkupTimeRepository.findActiveByCheckupId(CHECKUP_ID)).thenReturn(List.of(time));

        List<CheckupTime> result = checkupService.getTimesByCheckup(CHECKUP_ID);

        assertEquals(1, result.size());
        assertEquals("LIBRE", result.get(0).getStyle());
        verify(checkupTimeRepository, times(1)).findActiveByCheckupId(CHECKUP_ID);
    }

    @Test
    @DisplayName("Debe agregar y persistir un tiempo cuando el chequeo existe y no hay duplicado")
    void addCheckupTime_devuelveTiempoPersistido_cuandoChequeoExisteYNoDuplicado() {
        AddCheckupTimeRequest request = new AddCheckupTimeRequest("LIBRE", 100, new BigDecimal("65.250"));
        when(checkupRepository.findById(CHECKUP_ID)).thenReturn(Optional.of(checkup));
        when(checkupTimeRepository.existsActiveByCheckupIdAndStyleAndDistance(CHECKUP_ID, "LIBRE", 100))
                .thenReturn(false);
        when(checkupTimeRepository.save(any(CheckupTime.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CheckupTime result = checkupService.addCheckupTime(CHECKUP_ID, request);

        assertNotNull(result);
        assertEquals(CHECKUP_ID, result.getCheckup().getId());
        assertEquals("LIBRE", result.getStyle());
        assertEquals(100, result.getDistance());
        assertEquals(new BigDecimal("65.250"), result.getTimeSeconds());
        verify(checkupTimeRepository, times(1)).save(any(CheckupTime.class));
    }

    @Test
    @DisplayName("Debe lanzar DuplicateResourceException si ya existe tiempo para (estilo, distancia)")
    void addCheckupTime_lanza409_cuandoYaExisteTiempoParaStyleDistance() {
        AddCheckupTimeRequest request = new AddCheckupTimeRequest("LIBRE", 100, new BigDecimal("65.250"));
        when(checkupRepository.findById(CHECKUP_ID)).thenReturn(Optional.of(checkup));
        when(checkupTimeRepository.existsActiveByCheckupIdAndStyleAndDistance(CHECKUP_ID, "LIBRE", 100))
                .thenReturn(true);

        DuplicateResourceException ex = assertThrows(DuplicateResourceException.class,
                () -> checkupService.addCheckupTime(CHECKUP_ID, request));

        assertEquals("CheckupTime with checkupId/style/distance already exists", ex.getMessage());
        verify(checkupTimeRepository, never()).save(any(CheckupTime.class));
    }

    @Test
    @DisplayName("Debe lanzar ResourceNotFoundException al agregar un tiempo a un chequeo inexistente")
    void addCheckupTime_lanza404_cuandoChequeoNoExiste() {
        AddCheckupTimeRequest request = new AddCheckupTimeRequest("LIBRE", 100, new BigDecimal("65.250"));
        when(checkupRepository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> checkupService.addCheckupTime(99L, request));

        assertEquals("Checkup not found with id: '99'", ex.getMessage());
        verify(checkupTimeRepository, never()).save(any(CheckupTime.class));
    }

    @Test
    @DisplayName("Debe marcar chequeo y tiempos con la misma marca temporal al eliminar con tiempos")
    void softDeleteCheckup_marcaChequeoYTiemposConMismoInstant_cuandoHayTiempos() {
        CheckupTime time1 = CheckupTime.builder().id(11L).checkup(checkup).build();
        CheckupTime time2 = CheckupTime.builder().id(12L).checkup(checkup).build();
        when(checkupRepository.findById(CHECKUP_ID)).thenReturn(Optional.of(checkup));
        when(checkupTimeRepository.findActiveByCheckupId(CHECKUP_ID)).thenReturn(List.of(time1, time2));

        checkupService.softDeleteCheckup(CHECKUP_ID);

        assertNotNull(time1.getDeletedAt());
        assertNotNull(time2.getDeletedAt());
        assertNotNull(checkup.getDeletedAt());
        assertEquals(time1.getDeletedAt(), time2.getDeletedAt());
        assertEquals(checkup.getDeletedAt(), time1.getDeletedAt());
        verify(checkupTimeRepository, times(1)).saveAll(List.of(time1, time2));
        verify(checkupRepository, times(1)).save(checkup);
    }

    @Test
    @DisplayName("Debe marcar solo el chequeo cuando no tiene tiempos registrados")
    void softDeleteCheckup_marcaSoloChequeo_cuandoNoHayTiempos() {
        when(checkupRepository.findById(CHECKUP_ID)).thenReturn(Optional.of(checkup));
        when(checkupTimeRepository.findActiveByCheckupId(CHECKUP_ID)).thenReturn(List.of());

        checkupService.softDeleteCheckup(CHECKUP_ID);

        assertNotNull(checkup.getDeletedAt());
        verify(checkupTimeRepository, never()).saveAll(anyList());
        verify(checkupRepository, times(1)).save(checkup);
    }

    @Test
    @DisplayName("Debe lanzar ResourceNotFoundException al eliminar un chequeo inexistente")
    void softDeleteCheckup_lanza404_cuandoChequeoNoExiste() {
        when(checkupRepository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> checkupService.softDeleteCheckup(99L));

        assertEquals("Checkup not found with id: '99'", ex.getMessage());
        verify(checkupTimeRepository, never()).saveAll(anyList());
    }
}
