package com.athletecore.api.training;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.athletecore.api.training.dto.AttendanceResponse;
import com.athletecore.api.training.dto.RegisterAttendanceRequest;

import jakarta.validation.Valid;

/**
 * Controller REST para control de asistencia.
 * Registrar asistencia está disponible para ADMIN/COACH; las consultas de
 * historial están disponibles para el staff autenticado.
 */
@RestController
@RequestMapping("/api/v1")
@PreAuthorize("hasAnyRole('ADMIN', 'COACH')")
public class AttendanceController {

    private final AttendanceService attendanceService;

    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    /**
     * Registra (upsert) la asistencia de un deportista a una sesión.
     * POST /api/v1/training-sessions/{sessionId}/attendance
     */
    @PostMapping("/training-sessions/{sessionId}/attendance")
    public ResponseEntity<AttendanceResponse> registerAttendance(@PathVariable Long sessionId,
                                                                 @Valid @RequestBody RegisterAttendanceRequest request) {
        Attendance attendance = attendanceService.registerAttendance(sessionId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(AttendanceResponse.fromAttendance(attendance));
    }

    /**
     * Obtiene la lista de asistencia de una sesión.
     * GET /api/v1/training-sessions/{sessionId}/attendance
     */
    @GetMapping("/training-sessions/{sessionId}/attendance")
    public ResponseEntity<List<AttendanceResponse>> getAttendanceBySession(@PathVariable Long sessionId) {
        List<AttendanceResponse> responses = attendanceService.getAttendanceBySession(sessionId).stream()
                .map(AttendanceResponse::fromAttendance)
                .toList();
        return ResponseEntity.ok(responses);
    }

    /**
     * Obtiene el historial de asistencia de un deportista ordenado por fecha de sesión.
     * GET /api/v1/athletes/{athleteId}/attendance
     */
    @GetMapping("/athletes/{athleteId}/attendance")
    public ResponseEntity<List<AttendanceResponse>> getAttendanceByAthlete(@PathVariable Long athleteId) {
        List<AttendanceResponse> responses = attendanceService.getAttendanceByAthlete(athleteId).stream()
                .map(AttendanceResponse::fromAttendance)
                .toList();
        return ResponseEntity.ok(responses);
    }
}