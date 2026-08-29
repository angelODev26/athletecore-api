package com.athletecore.api.report.dto;

import java.math.BigDecimal;
import java.util.List;

import com.athletecore.api.checkup.dto.MedalProjectionResponse;

/**
 * DTO de respuesta del reporte individual por deportista: consolida la evolución
 * de tiempos de prueba, el resumen de asistencia y la proyección de medallería.
 * Contrato de datos estructurados para el frontend (la renderización gráfica es
 * responsabilidad del cliente). Reutiliza MedalProjectionResponse del módulo
 * checkup para la sección de proyección.
 */
public record IndividualReportResponse(
    Long athleteId,
    String athleteFullName,
    List<TimeEvolutionEntry> timeEvolution,
    AttendanceSummary attendance,
    List<MedalProjectionResponse> projections
) {
    /**
     * Entrada de evolución de tiempos: un tiempo de prueba en un chequeo de un
     * (year/month) dado, por estilo y distancia.
     */
    public record TimeEvolutionEntry(
        Integer year,
        Integer month,
        String style,
        Integer distance,
        BigDecimal timeSeconds,
        String timeFormatted
    ) {
    }

    /**
     * Resumen de asistencia del deportista: total de sesiones y desglose por
     * estado, junto con la racha actual de ausencias consecutivas.
     */
    public record AttendanceSummary(
        long totalSessions,
        long presentCount,
        long absentCount,
        long justifiedCount,
        int currentAbsenceStreak
    ) {
    }
}