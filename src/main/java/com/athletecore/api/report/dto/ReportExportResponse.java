package com.athletecore.api.report.dto;

import java.time.Instant;

import com.athletecore.api.report.ReportExport;

/**
 * DTO de respuesta para un export de reporte (metadata SIN el binario).
 * El contenido binario (content) solo se sirve en el endpoint de descarga,
 * nunca en listados de detalle.
 */
public record ReportExportResponse(
    Long id,
    Long reportId,
    String format,
    String fileName,
    Long fileSizeBytes,
    String contentType,
    Instant createdAt
) {
    /**
     * Crea ReportExportResponse desde una entidad ReportExport.
     *
     * @param export Entidad ReportExport
     * @return ReportExportResponse
     */
    public static ReportExportResponse fromExport(ReportExport export) {
        return new ReportExportResponse(
            export.getId(),
            export.getReport().getId(),
            export.getFormat(),
            export.getFileName(),
            export.getFileSizeBytes(),
            export.getContentType(),
            export.getCreatedAt()
        );
    }
}