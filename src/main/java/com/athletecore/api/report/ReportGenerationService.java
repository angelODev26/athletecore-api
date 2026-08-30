package com.athletecore.api.report;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.athletecore.api.athlete.AthleteRepository;
import com.athletecore.api.common.exception.ResourceNotFoundException;
import com.athletecore.api.common.exception.ValidationException;
import com.athletecore.api.report.dto.GenerateReportRequest;
import com.athletecore.api.report.dto.ReportDetailResponse;
import com.athletecore.api.report.dto.ReportExportResponse;
import com.athletecore.api.report.dto.ReportResponse;

/**
 * Servicio que gestiona el ciclo de vida del reporte: creación (PENDING),
 * ensamblado + export (PENDING → GENERATED / FAILED), lectura y soft delete
 * (decisión D9 del design). Orquesta {@link AthleteReportingService},
 * {@link TeamReportingService} y {@link ExportService}. Usa {@link Clock}
 * inyectado para las marcas temporales deterministas (decisión D7).
 */
@Service
public class ReportGenerationService {

    private final ReportRepository reportRepository;
    private final ReportExportRepository reportExportRepository;
    private final AthleteRepository athleteRepository;
    private final AthleteReportingService athleteReportingService;
    private final TeamReportingService teamReportingService;
    private final ExportService exportService;
    private final Clock clock;

    public ReportGenerationService(ReportRepository reportRepository,
                                   ReportExportRepository reportExportRepository,
                                   AthleteRepository athleteRepository,
                                   AthleteReportingService athleteReportingService,
                                   TeamReportingService teamReportingService,
                                   ExportService exportService,
                                   Clock clock) {
        this.reportRepository = reportRepository;
        this.reportExportRepository = reportExportRepository;
        this.athleteRepository = athleteRepository;
        this.athleteReportingService = athleteReportingService;
        this.teamReportingService = teamReportingService;
        this.exportService = exportService;
        this.clock = clock;
    }

    /**
     * Genera un reporte: persiste el Report en PENDING, ensambla los datos, lo
     * exporta a PDF y cierra en GENERATED; ante un error de ensamblado lo marca
     * FAILED con el mensaje correspondiente.
     *
     * @param request Datos del reporte a generar
     * @return ReportDetailResponse con el estado y exports resultantes
     * @throws ValidationException si es INDIVIDUAL sin athleteId (400)
     */
    @Transactional
    public ReportDetailResponse generateReport(GenerateReportRequest request) {
        validateRequest(request);

        Report report = reportRepository.save(Report.builder()
                .reportType(request.reportType())
                .athleteId(request.athleteId())
                .category(request.category())
                .year(request.year())
                .month(request.month())
                .title(request.title())
                .status(ReportStatus.PENDING)
                .build());

        try {
            if (request.reportType() == ReportType.INDIVIDUAL) {
                exportService.exportIndividualReport(report,
                        athleteReportingService.assembleIndividualReport(request.athleteId()));
            } else {
                exportService.exportTeamReport(report,
                        teamReportingService.assembleTeamReport(request.category()));
            }
            report.setStatus(ReportStatus.GENERATED);
        } catch (RuntimeException ex) {
            report.setStatus(ReportStatus.FAILED);
            report.setErrorMessage(ex.getMessage());
        }
        report = reportRepository.save(report);

        return toDetail(report);
    }

    /**
     * Obtiene el detalle de un reporte con sus exports asociados.
     *
     * @param id ID del reporte
     * @return ReportDetailResponse
     * @throws ResourceNotFoundException si no existe (404)
     */
    @Transactional(readOnly = true)
    public ReportDetailResponse getReportDetail(Long id) {
        Report report = getReportById(id);
        return toDetail(report);
    }

    /**
     * Lista reportes con filtros opcionales por tipo, deportista y estado.
     *
     * @param type      Tipo de reporte (opcional)
     * @param athleteId Deportista (opcional)
     * @param status    Estado (opcional)
     * @return Lista de ReportResponse, del más reciente al más antiguo
     */
    @Transactional(readOnly = true)
    public List<ReportResponse> listReports(ReportType type, Long athleteId, ReportStatus status) {
        return reportRepository.findAllByDeletedAtIsNull().stream()
                .filter(r -> type == null || r.getReportType() == type)
                .filter(r -> athleteId == null || athleteId.equals(r.getAthleteId()))
                .filter(r -> status == null || r.getStatus() == status)
                .sorted(Comparator.comparing(Report::getCreatedAt).reversed())
                .map(ReportResponse::fromReport)
                .toList();
    }

    /**
     * Elimina lógicamente un reporte (soft delete).
     *
     * @param id ID del reporte
     * @throws ResourceNotFoundException si no existe (404)
     */
    @Transactional
    public void softDeleteReport(Long id) {
        Report report = getReportById(id);
        Instant deletedAt = Instant.now(clock);

        List<ReportExport> exports = reportExportRepository.findActiveByReportId(id);
        if (!exports.isEmpty()) {
            exports.forEach(e -> e.setDeletedAt(deletedAt));
            reportExportRepository.saveAll(exports);
        }

        report.setDeletedAt(deletedAt);
        reportRepository.save(report);
    }

    private void validateRequest(GenerateReportRequest request) {
        if (request.reportType() == ReportType.INDIVIDUAL) {
            if (request.athleteId() == null) {
                throw new ValidationException("El reporte individual requiere un athleteId");
            }
            athleteRepository.findById(request.athleteId())
                    .orElseThrow(() -> new ResourceNotFoundException("Athlete", "id", request.athleteId()));
        }
    }

    private Report getReportById(Long id) {
        return reportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report", "id", id));
    }

    private ReportDetailResponse toDetail(Report report) {
        List<ReportExportResponse> exports = reportExportRepository.findActiveByReportId(report.getId())
                .stream()
                .map(ReportExportResponse::fromExport)
                .toList();
        return ReportDetailResponse.fromReport(report, exports);
    }
}