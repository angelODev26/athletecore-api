package com.athletecore.api.training;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.athletecore.api.training.dto.AttendanceAlertResponse;

/**
 * Controller REST para alertas de ausencias consecutivas.
 * La consulta de alertas activas está disponible para ADMIN/COACH; el
 * reconocimiento (acknowledge) es exclusivo de ADMIN.
 */
@RestController
@RequestMapping("/api/v1/alerts/attendance")
@PreAuthorize("hasAnyRole('ADMIN', 'COACH')")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    /**
     * Obtiene las alertas activas de ausencias consecutivas.
     * GET /api/v1/alerts/attendance
     */
    @GetMapping
    public ResponseEntity<List<AttendanceAlertResponse>> getActiveAlerts() {
        return ResponseEntity.ok(alertService.getActiveAlerts());
    }

    /**
     * Reconoce (acknowledge) la alerta de ausencias de un deportista (solo administradores).
     * POST /api/v1/alerts/attendance/{athleteId}/acknowledge
     */
    @PostMapping("/{athleteId}/acknowledge")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AttendanceAlertResponse> acknowledgeAlert(@PathVariable Long athleteId) {
        return ResponseEntity.ok(alertService.acknowledgeAlert(athleteId));
    }
}