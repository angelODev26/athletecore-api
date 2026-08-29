package com.athletecore.api.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.athletecore.api.common.exception.ResourceNotFoundException;
import com.athletecore.api.report.dto.IndividualReportResponse;
import com.athletecore.api.report.dto.IndividualReportResponse.AttendanceSummary;
import com.athletecore.api.report.dto.TeamReportResponse;

/**
 * Tests unitarios de ExportService: renderizado de PDFs (OpenPDF) y persistencia
 * de ReportExport con contentType/format/nombre de archivo correctos.
 */
@ExtendWith(MockitoExtension.class)
class ExportServiceTest {

    @Mock
    private ReportExportRepository reportExportRepository;

    private ExportService service;

    @BeforeEach
    void setUp() {
        service = new ExportService(reportExportRepository);
    }

    @Test
    @DisplayName("Debe generar un PDF no vacío y persistir el export individual con metadatos correctos")
    void exportIndividualReport_generaPdfYPersiste() {
        Report report = Report.builder().id(10L).title("Reporte individual")
                .status(ReportStatus.GENERATED).build();
        IndividualReportResponse data = new IndividualReportResponse(
                1L, "Juan Perez", List.of(),
                new AttendanceSummary(2, 1, 1, 0, 0), List.of());
        when(reportExportRepository.save(any(ReportExport.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ReportExport export = service.exportIndividualReport(report, data);

        assertTrue(export.getContent().length > 0);
        assertEquals("PDF", export.getFormat());
        assertEquals("application/pdf", export.getContentType());
        assertTrue(export.getFileName().contains("10"));
        assertEquals((long) export.getContent().length, export.getFileSizeBytes());
    }

    @Test
    @DisplayName("Debe generar un PDF no vacío y persistir el export general")
    void exportTeamReport_generaPdfYPersiste() {
        Report report = Report.builder().id(20L).title("Reporte general")
                .status(ReportStatus.GENERATED).build();
        TeamReportResponse data = new TeamReportResponse("MAYOR", List.of());
        when(reportExportRepository.save(any(ReportExport.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ReportExport export = service.exportTeamReport(report, data);

        assertTrue(export.getContent().length > 0);
        assertEquals("PDF", export.getFormat());
        assertEquals("application/pdf", export.getContentType());
        assertTrue(export.getFileName().contains("20"));
    }

    @Test
    @DisplayName("Debe lanzar ResourceNotFoundException cuando el reporte no tiene exports")
    void getLatestExport_lanza404_sinExports() {
        when(reportExportRepository.findActiveByReportId(5L)).thenReturn(List.of());

        assertThrows(ResourceNotFoundException.class, () -> service.getLatestExport(5L));
    }
}