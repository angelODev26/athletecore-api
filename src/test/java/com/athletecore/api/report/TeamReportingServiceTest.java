package com.athletecore.api.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.athletecore.api.athlete.Athlete;
import com.athletecore.api.athlete.AthleteRepository;
import com.athletecore.api.checkup.Classification;
import com.athletecore.api.checkup.MedalProjection;
import com.athletecore.api.checkup.MedalProjectionService;
import com.athletecore.api.report.dto.TeamReportResponse;

/**
 * Tests unitarios de TeamReportingService: agregación por (style, distance,
 * category) con mejor tiempo por atleta, filtrado por categoría y determinismo.
 */
@ExtendWith(MockitoExtension.class)
class TeamReportingServiceTest {

    @Mock
    private AthleteRepository athleteRepository;

    @Mock
    private MedalProjectionService medalProjectionService;

    private TeamReportingService service;

    private Athlete juan;
    private Athlete maria;

    @BeforeEach
    void setUp() {
        service = new TeamReportingService(athleteRepository, medalProjectionService);
        juan = Athlete.builder().id(1L).firstName("Juan").lastName("Perez").build();
        maria = Athlete.builder().id(2L).firstName("Maria").lastName("Lopez").build();
    }

    @Test
    @DisplayName("Debe agrupar por (estilo, distancia, categoría) con el mejor tiempo por atleta")
    void assembleTeamReport_agrupaMejorTiempoPorAtleta() {
        when(athleteRepository.findAllByDeletedAtIsNull(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(juan, maria)));
        // Juan tiene dos proyecciones en la misma prueba: se queda con la más rápida.
        when(medalProjectionService.getProjectionsForAthlete(1L)).thenReturn(List.of(
                new MedalProjection("LIBRE", 100, "MAYOR", Classification.POR_ENCIMA_DEL_PODIO,
                        new BigDecimal("64.000"), new BigDecimal("-1.000")),
                new MedalProjection("LIBRE", 100, "MAYOR", Classification.POR_ENCIMA_DEL_PODIO,
                        new BigDecimal("66.000"), new BigDecimal("1.000"))));
        when(medalProjectionService.getProjectionsForAthlete(2L)).thenReturn(List.of(
                new MedalProjection("LIBRE", 100, "MAYOR", Classification.POR_ENCIMA_DEL_PODIO,
                        new BigDecimal("63.000"), new BigDecimal("-2.000"))));
        when(athleteRepository.findById(1L)).thenReturn(Optional.of(juan));
        when(athleteRepository.findById(2L)).thenReturn(Optional.of(maria));

        TeamReportResponse report = service.assembleTeamReport(null);

        assertEquals(1, report.entries().size());
        TeamReportResponse.TeamEntry entry = report.entries().get(0);
        assertEquals("LIBRE", entry.style());
        assertEquals(100, entry.distance());
        assertEquals("MAYOR", entry.category());
        assertEquals(2, entry.athleteCount());

        // Ordenado por mejor tiempo ascendente: María (63.000) antes que Juan (64.000).
        assertEquals("Maria Lopez", entry.athletes().get(0).athleteFullName());
        assertEquals(new BigDecimal("63.000"), entry.athletes().get(0).bestTimeSeconds());
        assertEquals("Juan Perez", entry.athletes().get(1).athleteFullName());
        assertEquals(new BigDecimal("64.000"), entry.athletes().get(1).bestTimeSeconds());
    }

    @Test
    @DisplayName("Debe excluir las proyecciones de otras categorías al filtrar por categoría")
    void assembleTeamReport_filtraPorCategoria() {
        when(athleteRepository.findAllByDeletedAtIsNull(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(juan)));
        when(medalProjectionService.getProjectionsForAthlete(1L)).thenReturn(List.of(
                new MedalProjection("LIBRE", 100, "MAYOR", Classification.POR_ENCIMA_DEL_PODIO,
                        new BigDecimal("64.000"), new BigDecimal("-1.000")),
                new MedalProjection("LIBRE", 100, "JUVENIL", Classification.POR_ENCIMA_DEL_PODIO,
                        new BigDecimal("70.000"), new BigDecimal("5.000"))));
        when(athleteRepository.findById(1L)).thenReturn(Optional.of(juan));

        TeamReportResponse report = service.assembleTeamReport("MAYOR");

        assertEquals(1, report.entries().size());
        TeamReportResponse.TeamEntry entry = report.entries().get(0);
        assertEquals("MAYOR", entry.category());
        assertEquals(1, entry.athleteCount());
        assertEquals("Juan Perez", entry.athletes().get(0).athleteFullName());
    }

    @Test
    @DisplayName("Debe devolver lista vacía cuando no hay deportistas")
    void assembleTeamReport_devuelveListaVacia_sinDatos() {
        when(athleteRepository.findAllByDeletedAtIsNull(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        TeamReportResponse report = service.assembleTeamReport(null);

        assertTrue(report.entries().isEmpty());
    }
}