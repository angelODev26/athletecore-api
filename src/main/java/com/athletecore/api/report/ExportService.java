package com.athletecore.api.report;

import java.io.ByteArrayOutputStream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.athletecore.api.checkup.dto.MedalProjectionResponse;
import com.athletecore.api.common.exception.ResourceNotFoundException;
import com.athletecore.api.report.dto.IndividualReportResponse;
import com.athletecore.api.report.dto.IndividualReportResponse.TimeEvolutionEntry;
import com.athletecore.api.report.dto.TeamReportResponse;
import com.athletecore.api.report.dto.TeamReportResponse.TeamEntry;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

/**
 * Servicio de exportación de reportes a PDF (OpenPDF, decisión D1 del design).
 * Renderiza el contenido del reporte y persiste el artefacto binario en
 * {@link ReportExport} (BYTEA autocontenido, decisión D4). Operación de solo
 * lectura sobre los módulos fuente: no muta datos de athlete/training/checkup.
 */
@Service
public class ExportService {

    private static final String CONTENT_TYPE_PDF = "application/pdf";
    private static final String FORMAT = "PDF";

    private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
    private static final Font SECTION_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
    private static final Font BODY_FONT = FontFactory.getFont(FontFactory.HELVETICA, 10);

    private final ReportExportRepository reportExportRepository;

    public ExportService(ReportExportRepository reportExportRepository) {
        this.reportExportRepository = reportExportRepository;
    }

    /**
     * Exporta un reporte individual a PDF y persiste el ReportExport.
     *
     * @param report Reporte padre
     * @param data   Contenido del reporte individual
     * @return ReportExport persistido
     */
    @Transactional
    public ReportExport exportIndividualReport(Report report, IndividualReportResponse data) {
        byte[] pdf = renderIndividualPdf(report, data);
        return persist(report, pdf, "reporte-individual-" + report.getId() + ".pdf");
    }

    /**
     * Exporta un reporte general a PDF y persiste el ReportExport.
     *
     * @param report Reporte padre
     * @param data   Contenido del reporte general
     * @return ReportExport persistido
     */
    @Transactional
    public ReportExport exportTeamReport(Report report, TeamReportResponse data) {
        byte[] pdf = renderTeamPdf(report, data);
        return persist(report, pdf, "reporte-general-" + report.getId() + ".pdf");
    }

    /**
     * Obtiene el export más reciente de un reporte para su descarga.
     *
     * @param reportId ID del reporte
     * @return ReportExport más reciente
     * @throws ResourceNotFoundException si el reporte no tiene exports (404)
     */
    @Transactional(readOnly = true)
    public ReportExport getLatestExport(Long reportId) {
        return reportExportRepository.findActiveByReportId(reportId).stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("ReportExport", "reportId", reportId));
    }

    private ReportExport persist(Report report, byte[] pdf, String fileName) {
        return reportExportRepository.save(ReportExport.builder()
                .report(report)
                .format(FORMAT)
                .fileName(fileName)
                .fileSizeBytes((long) pdf.length)
                .contentType(CONTENT_TYPE_PDF)
                .content(pdf)
                .build());
    }

    private byte[] renderIndividualPdf(Report report, IndividualReportResponse data) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, out);
        document.open();

        document.add(new Paragraph(report.getTitle(), TITLE_FONT));
        document.add(new Paragraph("Deportista: " + data.athleteFullName(), SECTION_FONT));
        document.add(new Paragraph(" "));

        document.add(new Paragraph("Resumen de asistencia", SECTION_FONT));
        document.add(new Paragraph(
                "Sesiones totales: " + data.attendance().totalSessions()
                        + " | Presentes: " + data.attendance().presentCount()
                        + " | Ausentes: " + data.attendance().absentCount()
                        + " | Justificadas: " + data.attendance().justifiedCount()
                        + " | Racha actual de ausencias: " + data.attendance().currentAbsenceStreak(),
                BODY_FONT));
        document.add(new Paragraph(" "));

        document.add(new Paragraph("Evolución de tiempos", SECTION_FONT));
        document.add(buildTimeEvolutionTable(data));
        document.add(new Paragraph(" "));

        document.add(new Paragraph("Proyección de medallería", SECTION_FONT));
        document.add(buildProjectionTable(data.projections()));

        document.close();
        return out.toByteArray();
    }

    private byte[] renderTeamPdf(Report report, TeamReportResponse data) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, out);
        document.open();

        document.add(new Paragraph(report.getTitle(), TITLE_FONT));
        document.add(new Paragraph("Categoría: " + (data.category() == null ? "Todas" : data.category()),
                SECTION_FONT));
        document.add(new Paragraph(" "));

        document.add(new Paragraph("Comparativo de rendimiento", SECTION_FONT));
        document.add(buildTeamTable(data));

        document.close();
        return out.toByteArray();
    }

    private PdfPTable buildTimeEvolutionTable(IndividualReportResponse data) {
        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        addHeaderCells(table, "Año", "Mes", "Estilo", "Distancia (m)", "Tiempo");
        for (TimeEvolutionEntry entry : data.timeEvolution()) {
            table.addCell(String.valueOf(entry.year()));
            table.addCell(String.valueOf(entry.month()));
            table.addCell(entry.style());
            table.addCell(String.valueOf(entry.distance()));
            table.addCell(entry.timeFormatted());
        }
        return table;
    }

    private PdfPTable buildProjectionTable(java.util.List<MedalProjectionResponse> projections) {
        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        addHeaderCells(table, "Estilo", "Distancia (m)", "Categoría", "Clasificación", "Tiempo", "Dif. bronce");
        for (MedalProjectionResponse projection : projections) {
            table.addCell(projection.style());
            table.addCell(String.valueOf(projection.distance()));
            table.addCell(projection.category());
            table.addCell(projection.classification().name());
            table.addCell(projection.timeFormatted());
            table.addCell(projection.diffVsBronzeFormatted());
        }
        return table;
    }

    private PdfPTable buildTeamTable(TeamReportResponse data) {
        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        addHeaderCells(table, "Estilo", "Distancia (m)", "Atleta", "Mejor tiempo", "Clasificación");
        for (TeamEntry entry : data.entries()) {
            for (TeamReportResponse.AthleteEntry athlete : entry.athletes()) {
                table.addCell(entry.style());
                table.addCell(String.valueOf(entry.distance()));
                table.addCell(athlete.athleteFullName());
                table.addCell(athlete.bestTimeFormatted());
                table.addCell(athlete.classification().name());
            }
        }
        return table;
    }

    private void addHeaderCells(PdfPTable table, String... headers) {
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, SECTION_FONT));
            cell.setGrayFill(0.9f);
            table.addCell(cell);
        }
    }
}