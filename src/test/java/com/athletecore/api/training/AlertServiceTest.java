package com.athletecore.api.training;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.athletecore.api.athlete.Athlete;
import com.athletecore.api.athlete.AthleteRepository;
import com.athletecore.api.common.exception.ResourceNotFoundException;
import com.athletecore.api.config.TrainingProperties;
import com.athletecore.api.training.dto.AttendanceAlertResponse;

@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    private static final int THRESHOLD = 3;
    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-08-03T12:00:00Z"), ZoneId.of("UTC"));
    private static final LocalDate TODAY = LocalDate.of(2026, 8, 3);

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private AthleteRepository athleteRepository;

    private AlertService alertService;

    private Athlete athlete;
    private long sessionSeq;

    @BeforeEach
    void setUp() {
        alertService = new AlertService(attendanceRepository, athleteRepository,
                new TrainingProperties(THRESHOLD), FIXED_CLOCK);
        athlete = Athlete.builder().id(7L).firstName("Juan").lastName("Perez").build();
        sessionSeq = 0L;
    }

    private Attendance attendance(Athlete athlete, AttendanceStatus status, LocalDate date) {
        sessionSeq++;
        return Attendance.builder()
                .id(sessionSeq)
                .athlete(athlete)
                .session(TrainingSession.builder().id(sessionSeq).sessionDate(date).build())
                .status(status)
                .build();
    }

    @Test
    @DisplayName("Debe marcar al deportista cuando alcanza el umbral de ausencias consecutivas")
    void getActiveAlerts_ThresholdReached() {
        List<Attendance> records = List.of(
                attendance(athlete, AttendanceStatus.AUSENTE, LocalDate.of(2026, 7, 20)),
                attendance(athlete, AttendanceStatus.AUSENTE, LocalDate.of(2026, 7, 27)),
                attendance(athlete, AttendanceStatus.AUSENTE, LocalDate.of(2026, 8, 3)));
        when(attendanceRepository.findAllForAlertComputation(TODAY)).thenReturn(records);

        List<AttendanceAlertResponse> alerts = alertService.getActiveAlerts();

        assertEquals(1, alerts.size());
        assertEquals(7L, alerts.get(0).athleteId());
        assertEquals("Juan Perez", alerts.get(0).athleteFullName());
        assertEquals(3, alerts.get(0).consecutiveAbsenceCount());
        assertEquals(LocalDate.of(2026, 8, 3), alerts.get(0).lastAbsenceDate());
    }

    @Test
    @DisplayName("No debe marcar al deportista con menos ausencias que el umbral")
    void getActiveAlerts_BelowThreshold() {
        List<Attendance> records = List.of(
                attendance(athlete, AttendanceStatus.AUSENTE, LocalDate.of(2026, 7, 27)),
                attendance(athlete, AttendanceStatus.AUSENTE, LocalDate.of(2026, 8, 3)));
        when(attendanceRepository.findAllForAlertComputation(TODAY)).thenReturn(records);

        List<AttendanceAlertResponse> alerts = alertService.getActiveAlerts();

        assertEquals(0, alerts.size());
    }

    @Test
    @DisplayName("Una asistencia PRESENTE rompe la racha de ausencias")
    void getActiveAlerts_StreakBrokenByPresente() {
        List<Attendance> records = List.of(
                attendance(athlete, AttendanceStatus.AUSENTE, LocalDate.of(2026, 7, 20)),
                attendance(athlete, AttendanceStatus.PRESENTE, LocalDate.of(2026, 7, 27)),
                attendance(athlete, AttendanceStatus.AUSENTE, LocalDate.of(2026, 8, 3)));
        when(attendanceRepository.findAllForAlertComputation(TODAY)).thenReturn(records);

        List<AttendanceAlertResponse> alerts = alertService.getActiveAlerts();

        assertEquals(0, alerts.size());
    }

    @Test
    @DisplayName("Una ausencia justificada también rompe la racha")
    void getActiveAlerts_StreakBrokenByJustificado() {
        List<Attendance> records = List.of(
                attendance(athlete, AttendanceStatus.AUSENTE, LocalDate.of(2026, 7, 20)),
                attendance(athlete, AttendanceStatus.JUSTIFICADO, LocalDate.of(2026, 7, 27)),
                attendance(athlete, AttendanceStatus.AUSENTE, LocalDate.of(2026, 8, 3)));
        when(attendanceRepository.findAllForAlertComputation(TODAY)).thenReturn(records);

        List<AttendanceAlertResponse> alerts = alertService.getActiveAlerts();

        assertEquals(0, alerts.size());
    }

    @Test
    @DisplayName("No debe considerar sesiones con fecha futura al Clock en el cálculo")
    void getActiveAlerts_IgnoresFutureSessions() {
        // La sesión del 2026-08-10 es futura respecto al Clock fijo y no debe contar
        List<Attendance> records = List.of(
                attendance(athlete, AttendanceStatus.AUSENTE, LocalDate.of(2026, 7, 27)),
                attendance(athlete, AttendanceStatus.AUSENTE, LocalDate.of(2026, 8, 3)));
        when(attendanceRepository.findAllForAlertComputation(TODAY)).thenReturn(records);

        List<AttendanceAlertResponse> alerts = alertService.getActiveAlerts();

        assertEquals(0, alerts.size());
    }

    @Test
    @DisplayName("Debe devolver lista vacía cuando no hay alertas activas")
    void getActiveAlerts_NoAlerts() {
        when(attendanceRepository.findAllForAlertComputation(TODAY)).thenReturn(List.of());

        List<AttendanceAlertResponse> alerts = alertService.getActiveAlerts();

        assertEquals(0, alerts.size());
    }

    @Test
    @DisplayName("Debe reconocer la alerta de un deportista con umbral alcanzado")
    void acknowledgeAlert_Success() {
        List<Attendance> records = List.of(
                attendance(athlete, AttendanceStatus.AUSENTE, LocalDate.of(2026, 7, 20)),
                attendance(athlete, AttendanceStatus.AUSENTE, LocalDate.of(2026, 7, 27)),
                attendance(athlete, AttendanceStatus.AUSENTE, LocalDate.of(2026, 8, 3)));
        when(athleteRepository.findById(7L)).thenReturn(java.util.Optional.of(athlete));
        when(attendanceRepository.findForAthleteAlertComputation(7L, TODAY)).thenReturn(records);

        AttendanceAlertResponse alert = alertService.acknowledgeAlert(7L);

        assertEquals(7L, alert.athleteId());
        assertEquals(3, alert.consecutiveAbsenceCount());
    }

    @Test
    @DisplayName("Debe lanzar ResourceNotFoundException al reconocer un deportista sin alerta")
    void acknowledgeAlert_NoAlert_Throws() {
        List<Attendance> records = List.of(
                attendance(athlete, AttendanceStatus.AUSENTE, LocalDate.of(2026, 8, 3)));
        when(athleteRepository.findById(7L)).thenReturn(java.util.Optional.of(athlete));
        when(attendanceRepository.findForAthleteAlertComputation(7L, TODAY)).thenReturn(records);

        assertThrows(ResourceNotFoundException.class, () -> alertService.acknowledgeAlert(7L));
    }

    @Test
    @DisplayName("Debe lanzar ResourceNotFoundException al reconocer un deportista inexistente")
    void acknowledgeAlert_AthleteNotFound() {
        when(athleteRepository.findById(99L)).thenReturn(java.util.Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> alertService.acknowledgeAlert(99L));
    }
}