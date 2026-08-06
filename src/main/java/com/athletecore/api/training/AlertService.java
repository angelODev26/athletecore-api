package com.athletecore.api.training;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.athletecore.api.athlete.Athlete;
import com.athletecore.api.athlete.AthleteRepository;
import com.athletecore.api.common.exception.ResourceNotFoundException;
import com.athletecore.api.config.TrainingProperties;
import com.athletecore.api.training.dto.AttendanceAlertResponse;

/**
 * Servicio de alertas por ausencias consecutivas.
 * Las alertas son una consulta derivada del historial de asistencia (no se
 * persisten): un deportista se marca con alerta cuando acumula N ausencias
 * consecutivas (umbral configurable) en sus sesiones más recientes con fecha
 * menor o igual a hoy. El estado PRESENTE o JUSTIFICADO rompe la racha.
 * La lógica usa Clock inyectado para ser determinista y testeable.
 */
@Service
public class AlertService {

    private final AttendanceRepository attendanceRepository;
    private final AthleteRepository athleteRepository;
    private final TrainingProperties trainingProperties;
    private final Clock clock;

    public AlertService(AttendanceRepository attendanceRepository,
                        AthleteRepository athleteRepository,
                        TrainingProperties trainingProperties,
                        Clock clock) {
        this.attendanceRepository = attendanceRepository;
        this.athleteRepository = athleteRepository;
        this.trainingProperties = trainingProperties;
        this.clock = clock;
    }

    /**
     * Obtiene las alertas activas de ausencias consecutivas.
     * @return Lista de alertas con el conteo de ausencias y el deportista
     */
    @Transactional(readOnly = true)
    public List<AttendanceAlertResponse> getActiveAlerts() {
        LocalDate today = LocalDate.now(clock);
        List<Attendance> records = attendanceRepository.findAllForAlertComputation(today);

        Map<Long, Athlete> athletesById = records.stream()
                .collect(Collectors.toMap(
                        attendance -> attendance.getAthlete().getId(),
                        Attendance::getAthlete,
                        (first, second) -> first,
                        java.util.LinkedHashMap::new));

        Map<Long, List<Attendance>> recordsByAthlete = records.stream()
                .collect(Collectors.groupingBy(attendance -> attendance.getAthlete().getId()));

        List<AttendanceAlertResponse> alerts = new ArrayList<>();
        recordsByAthlete.forEach((athleteId, athleteRecords) -> {
            int streak = computeConsecutiveAbsences(athleteRecords);
            if (streak >= trainingProperties.absenceThreshold()) {
                Attendance last = athleteRecords.get(athleteRecords.size() - 1);
                Athlete athlete = athletesById.get(athleteId);
                alerts.add(new AttendanceAlertResponse(
                        athleteId,
                        athlete.getFullName(),
                        streak,
                        last.getSession().getSessionDate()));
            }
        });

        alerts.sort(Comparator.comparingInt(AttendanceAlertResponse::consecutiveAbsenceCount).reversed()
                .thenComparing(AttendanceAlertResponse::athleteFullName));
        return alerts;
    }

    /**
     * Reconoce (acknowledge) la alerta de un deportista.
     * Las alertas son derivadas, por lo que el reconocimiento confirma la alerta
     * actual del deportista; la racha solo se limpia cuando se registra una
     * asistencia que la rompa (PRESENTE o JUSTIFICADO).
     * @param athleteId ID del deportista
     * @return AttendanceAlertResponse de la alerta reconocida
     * @throws ResourceNotFoundException si el deportista no existe o no tiene alerta activa
     */
    @Transactional(readOnly = true)
    public AttendanceAlertResponse acknowledgeAlert(Long athleteId) {
        Athlete athlete = athleteRepository.findById(athleteId)
                .orElseThrow(() -> new ResourceNotFoundException("Athlete", "id", athleteId));

        LocalDate today = LocalDate.now(clock);
        List<Attendance> records = attendanceRepository.findForAthleteAlertComputation(athleteId, today);
        int streak = computeConsecutiveAbsences(records);

        if (streak < trainingProperties.absenceThreshold()) {
            throw new ResourceNotFoundException("AttendanceAlert", "athleteId", athleteId);
        }

        Attendance last = records.get(records.size() - 1);
        return new AttendanceAlertResponse(
                athleteId,
                athlete.getFullName(),
                streak,
                last.getSession().getSessionDate());
    }

    /**
     * Calcula la racha actual de ausencias consecutivas a partir de los registros
     * ordenados por fecha de sesión ascendente. La racha se reinicia ante cualquier
     * estado distinto de AUSENTE (PRESENTE o JUSTIFICADO).
     * @param recordsAsc Lista de asistencias ordenada por fecha ascendente
     * @return Número de ausencias consecutivas que finaliza en el último registro
     */
    int computeConsecutiveAbsences(List<Attendance> recordsAsc) {
        int streak = 0;
        for (Attendance record : recordsAsc) {
            streak = record.getStatus() == AttendanceStatus.AUSENTE ? streak + 1 : 0;
        }
        return streak;
    }
}