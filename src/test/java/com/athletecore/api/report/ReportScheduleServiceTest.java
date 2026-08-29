package com.athletecore.api.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.athletecore.api.common.exception.ValidationException;
import com.athletecore.api.report.dto.CreateReportScheduleRequest;
import com.athletecore.api.report.dto.GenerateReportRequest;
import com.athletecore.api.report.dto.UpdateReportScheduleRequest;

/**
 * Tests unitarios de ReportScheduleService: validación de cron, cálculo de
 * next_run_at, soft delete y ejecución de schedules vencidos.
 */
@ExtendWith(MockitoExtension.class)
class ReportScheduleServiceTest {

    private static final String VALID_CRON = "0 0 6 * * *";
    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-08-01T00:00:00Z"), ZoneOffset.UTC);

    @Mock
    private ReportScheduleRepository scheduleRepository;

    @Mock
    private ReportGenerationService reportGenerationService;

    private ReportScheduleService service;

    @BeforeEach
    void setUp() {
        service = new ReportScheduleService(scheduleRepository, reportGenerationService, FIXED_CLOCK);
    }

    @Test
    @DisplayName("Debe calcular nextRunAt no nulo y guardar con cron válido")
    void createSchedule_cronValido_calculaNextRunAt() {
        CreateReportScheduleRequest request = new CreateReportScheduleRequest(
                ReportType.GENERAL, null, "MAYOR", VALID_CRON, null, Boolean.TRUE);
        when(scheduleRepository.save(any(ReportSchedule.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ReportSchedule schedule = service.createSchedule(request);

        assertNotNull(schedule.getNextRunAt());
        assertTrue(schedule.getActive());
        assertTrue(schedule.getNextRunAt().isAfter(Instant.now(FIXED_CLOCK)));
    }

    @Test
    @DisplayName("Debe lanzar ValidationException con cron inválido y no guardar")
    void createSchedule_cronInvalido_lanza400() {
        CreateReportScheduleRequest request = new CreateReportScheduleRequest(
                ReportType.GENERAL, null, null, "not-a-cron", null, Boolean.TRUE);

        assertThrows(ValidationException.class, () -> service.createSchedule(request));
        verify(scheduleRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe actualizar campos y recalcular nextRunAt")
    void updateSchedule_actualizaYRecalcula() {
        ReportSchedule existing = ReportSchedule.builder().id(1L)
                .reportType(ReportType.GENERAL).cronExpression("0 0 5 * * *")
                .active(Boolean.FALSE).build();
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(scheduleRepository.save(any(ReportSchedule.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        UpdateReportScheduleRequest request = new UpdateReportScheduleRequest(
                ReportType.INDIVIDUAL, 7L, "MAYOR", VALID_CRON, null, Boolean.TRUE);

        ReportSchedule updated = service.updateSchedule(1L, request);

        assertEquals(ReportType.INDIVIDUAL, updated.getReportType());
        assertEquals(7L, updated.getAthleteId());
        assertEquals(VALID_CRON, updated.getCronExpression());
        assertTrue(updated.getActive());
        assertNotNull(updated.getNextRunAt());
    }

    @Test
    @DisplayName("Debe marcar deletedAt en soft delete")
    void softDeleteSchedule_marcaDeletedAt() {
        ReportSchedule schedule = ReportSchedule.builder().id(1L)
                .reportType(ReportType.GENERAL).cronExpression(VALID_CRON).build();
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(schedule));

        service.softDeleteSchedule(1L);

        assertNotNull(schedule.getDeletedAt());
        verify(scheduleRepository).save(schedule);
    }

    @Test
    @DisplayName("Debe ejecutar los schedules vencidos y actualizar lastRunAt/nextRunAt sin desactivarlos")
    void runDueSchedules_generaYActualiza() {
        ReportSchedule due = ReportSchedule.builder().id(1L)
                .reportType(ReportType.GENERAL).cronExpression(VALID_CRON)
                .active(Boolean.TRUE)
                .nextRunAt(Instant.parse("2026-07-31T00:00:00Z")).build();
        when(scheduleRepository.findDueActiveSchedules(any(Instant.class)))
                .thenReturn(List.of(due));

        service.runDueSchedules();

        verify(reportGenerationService).generateReport(any(GenerateReportRequest.class));
        assertNotNull(due.getLastRunAt());
        assertNotNull(due.getNextRunAt());
        assertTrue(due.getActive());
        verify(scheduleRepository).save(due);
    }
}