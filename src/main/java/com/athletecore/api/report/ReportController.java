package com.athletecore.api.report;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.athletecore.api.report.dto.GenerateReportRequest;
import com.athletecore.api.report.dto.IndividualReportResponse;
import com.athletecore.api.report.dto.ReportDetailResponse;
import com.athletecore.api.report.dto.ReportExportResponse;
import com.athletecore.api.report.dto.ReportResponse;
import com.athletecore.api.report.dto.TeamReportResponse;

import jakarta.validation.Valid;

/**
 * Controller REST del módulo reportes (REPORTS_API): generación de reportes
 * individuales y generales, consulta de contenido estructurado, exportación a
 * PDF y descarga.
 *
 * Seguridad:
 * - Lecturas (GET): autenticado, sin @PreAuthorize (lo cubre SecurityConfig con
 *   anyRequest().authenticated()).
 * - Escrituras (POST/DELETE): ADMIN/COACH, mismo patrón que el módulo checkup.
 */
@RestController
@RequestMapping("/api/v1")
public class ReportController {

    private final ReportGenerationService reportGenerationService;
    private final AthleteReportingService athleteReportingService;
    private final TeamReportingService teamReportingService;
    private final ExportService exportService;

    public ReportController(ReportGenerationService reportGenerationService,
                            AthleteReportingService athleteReportingService,
                            TeamReportingService teamReportingService,
                            ExportService exportService) {
        this.reportGenerationService = reportGenerationService;
        this.athleteReportingService = athleteReportingService;
        this.teamReportingService = teamReportingService;
        this.exportService = exportService;
    }

    /**
     * Genera un reporte (individual o general) y lo exporta a PDF.
     * POST /api/v1/reports
     */
    @PostMapping("/reports")
    @PreAuthorize("hasAnyRole('ADMIN', 'COACH')")
    public ResponseEntity<ReportDetailResponse> generateReport(
            @Valid @RequestBody GenerateReportRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reportGenerationService.generateReport(request));
    }

    /**
     * Lista reportes con filtros opcionales por tipo, deportista y estado.
     * GET /api/v1/reports
     */
    @GetMapping("/reports")
    public ResponseEntity<List<ReportResponse>> listReports(
            @RequestParam(required = false) ReportType type,
            @RequestParam(required = false) Long athleteId,
            @RequestParam(required = false) ReportStatus status) {
        return ResponseEntity.ok(reportGenerationService.listReports(type, athleteId, status));
    }

    /**
     * Obtiene el detalle de un reporte con sus exports asociados.
     * GET /api/v1/reports/{id}
     */
    @GetMapping("/reports/{id}")
    public ResponseEntity<ReportDetailResponse> getReport(@PathVariable Long id) {
        return ResponseEntity.ok(reportGenerationService.getReportDetail(id));
    }

    /**
     * Elimina lógicamente un reporte (soft delete).
     * DELETE /api/v1/reports/{id}
     */
    @DeleteMapping("/reports/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COACH')")
    public ResponseEntity<Void> softDeleteReport(@PathVariable Long id) {
        reportGenerationService.softDeleteReport(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Descarga el PDF del export más reciente de un reporte.
     * GET /api/v1/reports/{id}/export
     */
    @GetMapping("/reports/{reportId}/export")
    public ResponseEntity<byte[]> downloadExport(@PathVariable Long reportId) {
        ReportExport export = exportService.getLatestExport(reportId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(export.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + export.getFileName() + "\"")
                .body(export.getContent());
    }

    /**
     * Lista los exports de un reporte.
     * GET /api/v1/reports/{id}/exports
     */
    @GetMapping("/reports/{id}/exports")
    public ResponseEntity<List<ReportExportResponse>> listExports(@PathVariable Long id) {
        return ResponseEntity.ok(reportGenerationService.getReportDetail(id).exports());
    }

    /**
     * Reporte individual estructurado del deportista (evolución de tiempos,
     * asistencia y proyección) para el frontend.
     * GET /api/v1/athletes/{athleteId}/report
     */
    @GetMapping("/athletes/{athleteId}/report")
    public ResponseEntity<IndividualReportResponse> getIndividualReport(@PathVariable Long athleteId) {
        return ResponseEntity.ok(athleteReportingService.assembleIndividualReport(athleteId));
    }

    /**
     * Reporte general estructurado del equipo/categoría (comparativo).
     * GET /api/v1/reports/team
     */
    @GetMapping("/reports/team")
    public ResponseEntity<TeamReportResponse> getTeamReport(
            @RequestParam(required = false) String category) {
        return ResponseEntity.ok(teamReportingService.assembleTeamReport(category));
    }
}