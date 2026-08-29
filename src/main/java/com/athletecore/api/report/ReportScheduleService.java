package com.athletecore.api.report;

import java.time.Clock;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.athletecore.api.common.exception.ResourceNotFoundException;
import com.athletecore.api.common.exception.ValidationException;
import com.athletecore.api.report.dto.CreateReportScheduleRequest;
import com.athletecore.api.report.dto.GenerateReportRequest;
import com.athletecore.api.report.dto.ReportScheduleResponse;
import com.athletecore.api.report.dto.UpdateReportScheduleRequest;

/**
 * Servicio de programación automática de reportes (decisión D2 del design).
 * CRUD de {@link ReportSchedule} y ejecución desatendida vía {@code @Scheduled},
 * usando Spring {@link CronExpression} para validar y calcular la próxima
 * ejecución. Usa {@link Clock} inyectado para una lógica determinística (D7).
 */
@Service
public class ReportScheduleService {

    private final ReportScheduleRepository scheduleRepository;
    private final ReportGenerationService reportGenerationService;
    private final Clock clock;

    public ReportScheduleService(ReportScheduleRepository scheduleRepository,
                                 ReportGenerationService reportGenerationService,
                                 Clock clock) {
        this.scheduleRepository = scheduleRepository;
        this.reportGenerationService = reportGenerationService;
        this.clock = clock;
    }

    /**
     * Crea una programación, validando la expresión cron y calculando la
     * próxima ejecución a partir del instante actual.
     *
     * @param request Datos de la programación
     * @return ReportSchedule persistido
     * @throws ValidationException si la expresión cron es inválida (400)
     */
    @Transactional
    public ReportSchedule createSchedule(CreateReportScheduleRequest request) {
        validateCron(request.cronExpression());
        ReportSchedule schedule = ReportSchedule.builder()
                .reportType(request.reportType())
                .athleteId(request.athleteId())
                .category(request.category())
                .cronExpression(request.cronExpression())
                .timezone(request.timezone())
                .active(request.active() == null ? Boolean.TRUE : request.active())
                .nextRunAt(computeNextRun(request.cronExpression(), Instant.now(clock)))
                .build();
        return scheduleRepository.save(schedule);
    }

    /**
     * Actualiza una programación completa (PUT), recalculando la próxima
     * ejecución.
     *
     * @param id      ID de la programación
     * @param request Datos actualizados
     * @return ReportSchedule actualizado
     * @throws ResourceNotFoundException si no existe (404)
     * @throws ValidationException si la expresión cron es inválida (400)
     */
    @Transactional
    public ReportSchedule updateSchedule(Long id, UpdateReportScheduleRequest request) {
        validateCron(request.cronExpression());
        ReportSchedule schedule = getScheduleById(id);
        schedule.setReportType(request.reportType());
        schedule.setAthleteId(request.athleteId());
        schedule.setCategory(request.category());
        schedule.setCronExpression(request.cronExpression());
        schedule.setTimezone(request.timezone());
        schedule.setActive(request.active() == null ? Boolean.TRUE : request.active());
        schedule.setNextRunAt(computeNextRun(request.cronExpression(), Instant.now(clock)));
        return scheduleRepository.save(schedule);
    }

    /**
     * Obtiene una programación activa por ID.
     *
     * @param id ID de la programación
     * @return ReportSchedule
     * @throws ResourceNotFoundException si no existe (404)
     */
    @Transactional(readOnly = true)
    public ReportSchedule getSchedule(Long id) {
        return getScheduleById(id);
    }

    /**
     * Lista todas las programaciones activas (no eliminadas).
     *
     * @return Lista de ReportScheduleResponse
     */
    @Transactional(readOnly = true)
    public List<ReportScheduleResponse> listSchedules() {
        return scheduleRepository.findAllByDeletedAtIsNull().stream()
                .map(ReportScheduleResponse::fromSchedule)
                .toList();
    }

    /**
     * Elimina lógicamente una programación (soft delete).
     *
     * @param id ID de la programación
     * @throws ResourceNotFoundException si no existe (404)
     */
    @Transactional
    public void softDeleteSchedule(Long id) {
        ReportSchedule schedule = getScheduleById(id);
        schedule.setDeletedAt(Instant.now(clock));
        scheduleRepository.save(schedule);
    }

    /**
     * Ejecuta las programaciones vencidas: dispara la generación del reporte
     * para cada schedule activo con next_run_at menor o igual a ahora, y
     * actualiza last_run_at/next_run_at. Ejecutado por el scheduler con un
     * intervalo configurable ({@code report.scheduling.poll-interval-ms}).
     */
    @Scheduled(fixedDelayString = "${report.scheduling.poll-interval-ms}")
    @Transactional
    public void runDueSchedules() {
        Instant now = Instant.now(clock);
        List<ReportSchedule> due = scheduleRepository.findDueActiveSchedules(now);
        for (ReportSchedule schedule : due) {
            reportGenerationService.generateReport(toGenerateRequest(schedule));
            schedule.setLastRunAt(now);
            schedule.setNextRunAt(computeNextRun(schedule.getCronExpression(), now));
            scheduleRepository.save(schedule);
        }
    }

    private GenerateReportRequest toGenerateRequest(ReportSchedule schedule) {
        return new GenerateReportRequest(
                schedule.getReportType(),
                schedule.getAthleteId(),
                schedule.getCategory(),
                null,
                null,
                buildTitle(schedule)
        );
    }

    private String buildTitle(ReportSchedule schedule) {
        String base = schedule.getReportType() == ReportType.INDIVIDUAL
                ? "Reporte individual programado"
                : "Reporte general programado";
        return schedule.getCategory() == null ? base : base + " (" + schedule.getCategory() + ")";
    }

    private void validateCron(String cron) {
        try {
            CronExpression.parse(cron);
        } catch (IllegalArgumentException ex) {
            throw new ValidationException("Expresión cron inválida: " + cron);
        }
    }

    private Instant computeNextRun(String cron, Instant after) {
        CronExpression cronExpression = CronExpression.parse(cron);
        ZonedDateTime afterZoned = after.atZone(clock.getZone());
        ZonedDateTime next = cronExpression.next(afterZoned);
        if (next == null) {
            throw new ValidationException("La expresión cron no produce una próxima ejecución: " + cron);
        }
        return next.toInstant();
    }

    private ReportSchedule getScheduleById(Long id) {
        return scheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ReportSchedule", "id", id));
    }
}