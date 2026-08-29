package com.athletecore.api.report.dto;

import java.math.BigDecimal;
import java.util.List;

import com.athletecore.api.checkup.Classification;

/**
 * DTO de respuesta del reporte general de equipo/categoría: agrega por
 * (style, distance, category) los atletas participantes con su mejor tiempo y
 * clasificación de proyección, para comparar rendimiento entre deportistas.
 * Contrato de datos estructurados para el frontend.
 */
public record TeamReportResponse(
    String category,
    List<TeamEntry> entries
) {
    /**
     * Entrada agregada por (style, distance, category): incluye el número de
     * atletas evaluados y una entrada por atleta.
     */
    public record TeamEntry(
        String style,
        Integer distance,
        String category,
        int athleteCount,
        List<AthleteEntry> athletes
    ) {
    }

    /**
     * Rendimiento de un atleta en una prueba: su mejor tiempo alcanzado (el más
     * rápido) y la clasificación de proyección contra el bronce.
     */
    public record AthleteEntry(
        Long athleteId,
        String athleteFullName,
        BigDecimal bestTimeSeconds,
        String bestTimeFormatted,
        Classification classification,
        BigDecimal diffVsBronzeSeconds,
        String diffVsBronzeFormatted
    ) {
    }
}