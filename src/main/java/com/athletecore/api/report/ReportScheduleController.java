package com.athletecore.api.report;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.athletecore.api.report.dto.CreateReportScheduleRequest;
import com.athletecore.api.report.dto.ReportScheduleResponse;
import com.athletecore.api.report.dto.UpdateReportScheduleRequest;

import jakarta.validation.Valid;

/**
 * Controller REST de programación automática de reportes.
 * Las escrituras (POST/PUT/DELETE) están restringidas a ADMIN; las lecturas
 * (GET) quedan autenticadas por anyRequest().authenticated().
 */
@RestController
@RequestMapping("/api/v1/report-schedules")
public class ReportScheduleController {

    private final ReportScheduleService reportScheduleService;

    public ReportScheduleController(ReportScheduleService reportScheduleService) {
        this.reportScheduleService = reportScheduleService;
    }

    /**
     * Crea una programación automática de reportes.
     * POST /api/v1/report-schedules
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReportScheduleResponse> createSchedule(
            @Valid @RequestBody CreateReportScheduleRequest request) {
        ReportSchedule schedule = reportScheduleService.createSchedule(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ReportScheduleResponse.fromSchedule(schedule));
    }

    /**
     * Lista todas las programaciones activas.
     * GET /api/v1/report-schedules
     */
    @GetMapping
    public ResponseEntity<List<ReportScheduleResponse>> listSchedules() {
        return ResponseEntity.ok(reportScheduleService.listSchedules());
    }

    /**
     * Obtiene una programación por ID.
     * GET /api/v1/report-schedules/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ReportScheduleResponse> getSchedule(@PathVariable Long id) {
        return ResponseEntity.ok(
                ReportScheduleResponse.fromSchedule(reportScheduleService.getSchedule(id)));
    }

    /**
     * Actualiza una programación (PUT).
     * PUT /api/v1/report-schedules/{id}
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReportScheduleResponse> updateSchedule(
            @PathVariable Long id,
            @Valid @RequestBody UpdateReportScheduleRequest request) {
        ReportSchedule schedule = reportScheduleService.updateSchedule(id, request);
        return ResponseEntity.ok(ReportScheduleResponse.fromSchedule(schedule));
    }

    /**
     * Elimina lógicamente una programación (soft delete).
     * DELETE /api/v1/report-schedules/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> softDeleteSchedule(@PathVariable Long id) {
        reportScheduleService.softDeleteSchedule(id);
        return ResponseEntity.noContent().build();
    }
}