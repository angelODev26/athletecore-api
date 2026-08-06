package com.athletecore.api.training;

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

import com.athletecore.api.training.dto.CreateSessionRequest;
import com.athletecore.api.training.dto.SessionDetailResponse;
import com.athletecore.api.training.dto.SessionResponse;
import com.athletecore.api.training.dto.UpdateSessionRequest;
import com.athletecore.api.training.dto.UpdateSessionStatusRequest;

import jakarta.validation.Valid;

/**
 * Controller REST para sesiones de entrenamiento.
 * Crear, actualizar y cambiar estado de sesiones está disponible para
 * ADMIN/COACH; el borrado es exclusivo de ADMIN.
 */
@RestController
@RequestMapping("/api/v1")
@PreAuthorize("hasAnyRole('ADMIN', 'COACH')")
public class TrainingSessionController {

    private final TrainingSessionService trainingSessionService;
    private final AttendanceService attendanceService;

    public TrainingSessionController(TrainingSessionService trainingSessionService,
                                     AttendanceService attendanceService) {
        this.trainingSessionService = trainingSessionService;
        this.attendanceService = attendanceService;
    }

    /**
     * Registra una sesión dentro de un ciclo.
     * POST /api/v1/training-cycles/{cycleId}/sessions
     */
    @PostMapping("/training-cycles/{cycleId}/sessions")
    public ResponseEntity<SessionResponse> createSessionInCycle(@PathVariable Long cycleId,
                                                                @Valid @RequestBody CreateSessionRequest request) {
        TrainingSession session = trainingSessionService.createSessionForCycle(cycleId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(SessionResponse.fromSession(session));
    }

    /**
     * Obtiene las sesiones de un ciclo ordenadas por fecha.
     * GET /api/v1/training-cycles/{cycleId}/sessions
     */
    @GetMapping("/training-cycles/{cycleId}/sessions")
    public ResponseEntity<List<SessionResponse>> getSessionsByCycle(@PathVariable Long cycleId) {
        List<SessionResponse> responses = trainingSessionService.getSessionsByCycle(cycleId).stream()
                .map(SessionResponse::fromSession)
                .toList();
        return ResponseEntity.ok(responses);
    }

    /**
     * Registra una sesión sin asociarla necesariamente a un ciclo.
     * POST /api/v1/training-sessions
     */
    @PostMapping("/training-sessions")
    public ResponseEntity<SessionResponse> createSession(@Valid @RequestBody CreateSessionRequest request) {
        TrainingSession session = trainingSessionService.createSession(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(SessionResponse.fromSession(session));
    }

    /**
     * Obtiene el detalle de una sesión con su listado de asistencia.
     * GET /api/v1/training-sessions/{id}
     */
    @GetMapping("/training-sessions/{id}")
    public ResponseEntity<SessionDetailResponse> getSession(@PathVariable Long id) {
        TrainingSession session = trainingSessionService.getSessionById(id);
        List<Attendance> attendance = attendanceService.getAttendanceBySession(id);
        return ResponseEntity.ok(SessionDetailResponse.fromSession(session, attendance));
    }

    /**
     * Actualiza parcialmente una sesión.
     * PUT /api/v1/training-sessions/{id}
     */
    @PutMapping("/training-sessions/{id}")
    public ResponseEntity<SessionResponse> updateSession(@PathVariable Long id,
                                                         @Valid @RequestBody UpdateSessionRequest request) {
        TrainingSession session = trainingSessionService.updateSession(id, request);
        return ResponseEntity.ok(SessionResponse.fromSession(session));
    }

    /**
     * Cambia el estado de una sesión.
     * PUT /api/v1/training-sessions/{id}/status
     */
    @PutMapping("/training-sessions/{id}/status")
    public ResponseEntity<SessionResponse> changeStatus(@PathVariable Long id,
                                                        @Valid @RequestBody UpdateSessionStatusRequest request) {
        TrainingSession session = trainingSessionService.changeStatus(id, request.status());
        return ResponseEntity.ok(SessionResponse.fromSession(session));
    }

    /**
     * Elimina lógicamente una sesión (solo administradores).
     * DELETE /api/v1/training-sessions/{id}
     */
    @DeleteMapping("/training-sessions/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> softDeleteSession(@PathVariable Long id) {
        trainingSessionService.softDeleteSession(id);
        return ResponseEntity.noContent().build();
    }
}