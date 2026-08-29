package com.athletecore.api.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
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
import com.athletecore.api.checkup.Checkup;
import com.athletecore.api.checkup.CheckupService;
import com.athletecore.api.checkup.CheckupTime;
import com.athletecore.api.checkup.Classification;
import com.athletecore.api.checkup.MedalProjection;
import com.athletecore.api.checkup.MedalProjectionService;
import com.athletecore.api.common.exception.ResourceNotFoundException;
import com.athletecore.api.report.dto.IndividualReportResponse;
import com.athletecore.api.training.Attendance;
import com.athletecore.api.training.AttendanceService;
import com.athletecore.api.training.AttendanceStatus;

/**
 * Tests unitarios de AthleteReportingService: ensamblado del reporte individual
 * (evolución de tiempos, resumen de asistencia y proyección de medallería) en
 * modo de solo lectura, respetando la frontera de módulo.
 */
@ExtendWith(MockitoExtension.class)
class AthleteReportingServiceTest {

    private static final Long ATHLETE_ID = 7L;

    @Mock
    private AthleteRepository athleteRepository;

    @Mock
    private CheckupService checkupService;

    @Mock
    private AttendanceService attendanceService;

    @Mock
    private MedalProjectionService medalProjectionService;

    private AthleteReportingService service;

    private Athlete athlete;

    @BeforeEach
    void setUp() {
        service = new AthleteReportingService(athleteRepository, checkupService,
                attendanceService, medalProjectionService);
        athlete = Athlete.builder().id(ATHLETE_ID).firstName("Juan").lastName("Perez").build();
    }

    private Attendance attendance(AttendanceStatus status) {
        return Attendance.builder().id(1L).athlete(athlete).status(status).build();
    }

    @Test
    @DisplayName("Debe lanzar ResourceNotFoundException cuando el deportista no existe")
    void assembleIndividualReport_lanza404_cuandoAtletaNoExiste() {
        when(athleteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.assembleIndividualReport(99L));
    }

    @Test
    @DisplayName("Debe devolver evolución y proyección vacías cuando el atleta no tiene checkups")
    void assembleIndividualReport_devuelveSeccionesVacias_cuandoAtletaSinCheckups() {
        when(athleteRepository.findById(ATHLETE_ID)).thenReturn(Optional.of(athlete));
        when(checkupService.getCheckupsByAthlete(ATHLETE_ID, null, null)).thenReturn(List.of());
        when(attendanceService.getAttendanceByAthlete(ATHLETE_ID)).thenReturn(List.of());
        when(medalProjectionService.getProjectionsForAthlete(ATHLETE_ID)).thenReturn(List.of());

        IndividualReportResponse report = service.assembleIndividualReport(ATHLETE_ID);

        assertEquals(ATHLETE_ID, report.athleteId());
        assertEquals("Juan Perez", report.athleteFullName());
        assertTrue(report.timeEvolution().isEmpty());
        assertEquals(0, report.projections().size());
        assertEquals(0, report.attendance().totalSessions());
        assertEquals(0, report.attendance().presentCount());
        assertEquals(0, report.attendance().absentCount());
        assertEquals(0, report.attendance().justifiedCount());
        assertEquals(0, report.attendance().currentAbsenceStreak());
    }

    @Test
    @DisplayName("Debe construir el reporte con nombre completo, evolución ordenada y resumen de asistencia")
    void assembleIndividualReport_construyeReporteCompleto_cuandoAtletaConDatos() {
        Checkup julio = Checkup.builder().id(1L).year(2026).month(7).category("MAYOR").build();
        Checkup marzo = Checkup.builder().id(2L).year(2026).month(3).category("MAYOR").build();
        when(athleteRepository.findById(ATHLETE_ID)).thenReturn(Optional.of(athlete));
        when(checkupService.getCheckupsByAthlete(ATHLETE_ID, null, null))
                .thenReturn(List.of(julio, marzo));
        when(checkupService.getTimesByCheckup(1L)).thenReturn(List.of(
                CheckupTime.builder().id(11L).style("LIBRE").distance(100)
                        .timeSeconds(new BigDecimal("65.250")).build()));
        when(checkupService.getTimesByCheckup(2L)).thenReturn(List.of(
                CheckupTime.builder().id(12L).style("LIBRE").distance(100)
                        .timeSeconds(new BigDecimal("67.100")).build()));
        when(attendanceService.getAttendanceByAthlete(ATHLETE_ID)).thenReturn(List.of(
                attendance(AttendanceStatus.PRESENTE),
                attendance(AttendanceStatus.AUSENTE),
                attendance(AttendanceStatus.JUSTIFICADO)));
        when(medalProjectionService.getProjectionsForAthlete(ATHLETE_ID)).thenReturn(List.of(
                new MedalProjection("LIBRE", 100, "MAYOR", Classification.POR_ENCIMA_DEL_PODIO,
                        new BigDecimal("64.000"), new BigDecimal("-1.000"))));

        IndividualReportResponse report = service.assembleIndividualReport(ATHLETE_ID);

        assertEquals("Juan Perez", report.athleteFullName());

        // Evolución ordenada por (year, month) ascendente: marzo antes que julio.
        assertEquals(2, report.timeEvolution().size());
        assertEquals(3, report.timeEvolution().get(0).month());
        assertEquals(new BigDecimal("67.100"), report.timeEvolution().get(0).timeSeconds());
        assertEquals(7, report.timeEvolution().get(1).month());
        assertEquals(new BigDecimal("65.250"), report.timeEvolution().get(1).timeSeconds());

        // Resumen de asistencia: 3 sesiones, 1 presenta, 1 ausente, 1 justificada.
        assertEquals(3, report.attendance().totalSessions());
        assertEquals(1, report.attendance().presentCount());
        assertEquals(1, report.attendance().absentCount());
        assertEquals(1, report.attendance().justifiedCount());

        assertEquals(1, report.projections().size());
        assertEquals("LIBRE", report.projections().get(0).style());
    }

    @Test
    @DisplayName("Debe calcular la racha de ausencias consecutivas al final de la secuencia")
    void assembleIndividualReport_calculaRachaFinal_correcta() {
        when(athleteRepository.findById(ATHLETE_ID)).thenReturn(Optional.of(athlete));
        when(checkupService.getCheckupsByAthlete(ATHLETE_ID, null, null)).thenReturn(List.of());
        // Secuencia: AUSENTE, AUSENTE, JUSTIFICADO, AUSENTE -> racha final 1.
        when(attendanceService.getAttendanceByAthlete(ATHLETE_ID)).thenReturn(List.of(
                attendance(AttendanceStatus.AUSENTE),
                attendance(AttendanceStatus.AUSENTE),
                attendance(AttendanceStatus.JUSTIFICADO),
                attendance(AttendanceStatus.AUSENTE)));
        when(medalProjectionService.getProjectionsForAthlete(ATHLETE_ID)).thenReturn(List.of());

        IndividualReportResponse report = service.assembleIndividualReport(ATHLETE_ID);

        assertEquals(4, report.attendance().totalSessions());
        assertEquals(0, report.attendance().presentCount());
        assertEquals(3, report.attendance().absentCount());
        assertEquals(1, report.attendance().justifiedCount());
        assertEquals(1, report.attendance().currentAbsenceStreak());
    }
}