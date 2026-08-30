package com.athletecore.api.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.athletecore.api.athlete.Athlete;
import com.athletecore.api.athlete.AthleteRepository;
import com.athletecore.api.common.exception.ResourceNotFoundException;
import com.athletecore.api.common.exception.ValidationException;
import com.athletecore.api.report.dto.GenerateReportRequest;
import com.athletecore.api.report.dto.IndividualReportResponse;
import com.athletecore.api.report.dto.ReportDetailResponse;
import com.athletecore.api.report.dto.ReportResponse;
import com.athletecore.api.report.dto.TeamReportResponse;

/**
 * Tests unitarios de ReportGenerationService: ciclo de vida PENDING → GENERATED/
 * FAILED, validación de request y filtros de listado.
 */
@ExtendWith(MockitoExtension.class)
class ReportGenerationServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-08-01T00:00:00Z"), ZoneOffset.UTC);

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private ReportExportRepository reportExportRepository;

    @Mock
    private AthleteRepository athleteRepository;

    @Mock
    private AthleteReportingService athleteReportingService;

    @Mock
    private TeamReportingService teamReportingService;

    @Mock
    private ExportService exportService;

    private ReportGenerationService service;

    @BeforeEach
    void setUp() {
        service = new ReportGenerationService(reportRepository, reportExportRepository,
                athleteRepository, athleteReportingService, teamReportingService, exportService, FIXED_CLOCK);
    }

    @Test
    @DisplayName("Debe lanzar ValidationException y no persistir si un reporte individual no tiene athleteId")
    void generateReport_lanza400_sinAthleteId_cuandoIndividual() {
        GenerateReportRequest request = new GenerateReportRequest(
                ReportType.INDIVIDUAL, null, null, null, null, "Reporte individual");

        assertThrows(ValidationException.class, () -> service.generateReport(request));
        verify(reportRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe lanzar ResourceNotFoundException y no persistir si el atleta no existe")
    void generateReport_lanza404_siAtletaInexistente() {
        GenerateReportRequest request = new GenerateReportRequest(
                ReportType.INDIVIDUAL, 999L, null, null, null, "Reporte individual");
        when(athleteRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.generateReport(request));
        verify(reportRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe cerrar un reporte individual en GENERATED y delegar el export")
    void generateReport_individual_exitoso() {
        GenerateReportRequest request = new GenerateReportRequest(
                ReportType.INDIVIDUAL, 7L, null, null, null, "Reporte individual");
        when(reportRepository.save(any(Report.class))).thenAnswer(inv -> inv.getArgument(0));
        when(reportExportRepository.findActiveByReportId(any())).thenReturn(List.of());
        when(athleteRepository.findById(7L)).thenReturn(Optional.of(Athlete.builder().id(7L).build()));
        when(athleteReportingService.assembleIndividualReport(7L))
                .thenReturn(new IndividualReportResponse(7L, "Juan Perez", List.of(), null, List.of()));

        ReportDetailResponse result = service.generateReport(request);

        assertEquals(ReportStatus.GENERATED, result.status());
        verify(exportService).exportIndividualReport(any(Report.class), any());
    }

    @Test
    @DisplayName("Debe marcar FAILED con errorMessage cuando el ensamblado lanza una excepción")
    void generateReport_error_ensamblado_marcaFailed() {
        GenerateReportRequest request = new GenerateReportRequest(
                ReportType.INDIVIDUAL, 7L, null, null, null, "Reporte individual");
        when(reportRepository.save(any(Report.class))).thenAnswer(inv -> inv.getArgument(0));
        when(reportExportRepository.findActiveByReportId(any())).thenReturn(List.of());
        when(athleteRepository.findById(7L)).thenReturn(Optional.of(Athlete.builder().id(7L).build()));
        when(athleteReportingService.assembleIndividualReport(7L))
                .thenThrow(new RuntimeException("Error de ensamblado"));

        ReportDetailResponse result = service.generateReport(request);

        assertEquals(ReportStatus.FAILED, result.status());
        assertNotNull(result.errorMessage());
        assertEquals("Error de ensamblado", result.errorMessage());
    }

    @Test
    @DisplayName("Debe cerrar un reporte general en GENERATED y delegar el export de equipo")
    void generateReport_general_exitoso() {
        GenerateReportRequest request = new GenerateReportRequest(
                ReportType.GENERAL, null, "MAYOR", null, null, "Reporte general");
        when(reportRepository.save(any(Report.class))).thenAnswer(inv -> inv.getArgument(0));
        when(reportExportRepository.findActiveByReportId(any())).thenReturn(List.of());
        when(teamReportingService.assembleTeamReport("MAYOR"))
                .thenReturn(new TeamReportResponse("MAYOR", List.of()));

        ReportDetailResponse result = service.generateReport(request);

        assertEquals(ReportStatus.GENERATED, result.status());
        verify(exportService).exportTeamReport(any(Report.class), any());
    }

    @Test
    @DisplayName("Debe filtrar reportes por tipo, deportista y estado")
    void listReports_filtraPorTipoAtletaYEstado() {
        Report individualGanado = Report.builder().id(1L).reportType(ReportType.INDIVIDUAL)
                .athleteId(7L).title("R1").status(ReportStatus.GENERATED).build();
        individualGanado.setCreatedAt(Instant.parse("2026-08-01T00:00:00Z"));
        Report generalFallido = Report.builder().id(2L).reportType(ReportType.GENERAL)
                .title("R2").status(ReportStatus.FAILED).build();
        generalFallido.setCreatedAt(Instant.parse("2026-07-01T00:00:00Z"));
        when(reportRepository.findAllByDeletedAtIsNull())
                .thenReturn(List.of(individualGanado, generalFallido));

        List<ReportResponse> result = service.listReports(ReportType.GENERAL, null, ReportStatus.FAILED);

        assertEquals(1, result.size());
        assertEquals("R2", result.get(0).title());
    }

    @Test
    @DisplayName("Debe marcar deletedAt y guardar al hacer soft delete")
    void softDeleteReport_marcaDeletedAt() {
        Report report = Report.builder().id(5L).reportType(ReportType.GENERAL)
                .title("R5").status(ReportStatus.GENERATED).build();
        when(reportRepository.findById(5L)).thenReturn(Optional.of(report));
        when(reportExportRepository.findActiveByReportId(5L)).thenReturn(List.of());

        service.softDeleteReport(5L);

        assertNotNull(report.getDeletedAt());
        verify(reportRepository).save(report);
    }

    @Test
    @DisplayName("Debe propagar el soft delete a los exports del reporte")
    void softDeleteReport_propagaAExports() {
        Report report = Report.builder().id(5L).reportType(ReportType.GENERAL)
                .title("R5").status(ReportStatus.GENERATED).build();
        ReportExport export = ReportExport.builder().id(1L).report(report)
                .fileName("reporte-general-5.pdf").content(new byte[]{1, 2, 3}).build();
        when(reportRepository.findById(5L)).thenReturn(Optional.of(report));
        when(reportExportRepository.findActiveByReportId(5L)).thenReturn(List.of(export));

        service.softDeleteReport(5L);

        assertNotNull(export.getDeletedAt());
        verify(reportExportRepository).saveAll(any());
    }
}