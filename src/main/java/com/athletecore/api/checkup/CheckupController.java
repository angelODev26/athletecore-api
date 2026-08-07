package com.athletecore.api.checkup;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.athletecore.api.checkup.dto.AddCheckupTimeRequest;
import com.athletecore.api.checkup.dto.CheckupDetailResponse;
import com.athletecore.api.checkup.dto.CheckupResponse;
import com.athletecore.api.checkup.dto.CheckupTimeResponse;
import com.athletecore.api.checkup.dto.CreateCheckupRequest;
import com.athletecore.api.checkup.dto.MedalProjectionResponse;
import com.athletecore.api.checkup.dto.TimeComparisonResponse;

import jakarta.validation.Valid;

/**
 * Controller REST del módulo checkup: chequeos mensuales, tiempos de prueba,
 * comparación contra la tabla nacional y proyección de medallería.
 * Agrupa los subrecursos bajo {@code /checkups} y {@code /athletes/{athleteId}}
 * en un solo controller (recomendación de diseño); la tabla nacional vive en
 * {@link NationalReferenceTimeController}.
 *
 * Seguridad:
 * - Lecturas (GET): autenticado, sin @PreAuthorize (lo cubre SecurityConfig con
 *   anyRequest().authenticated()).
 * - Escrituras de chequeos (POST/DELETE): ADMIN/COACH, mismo patrón que el
 *   módulo training (TrainingPlanController).
 */
@RestController
@RequestMapping("/api/v1")
public class CheckupController {

    private final CheckupService checkupService;
    private final TimeComparisonService timeComparisonService;
    private final MedalProjectionService medalProjectionService;

    public CheckupController(CheckupService checkupService,
                             TimeComparisonService timeComparisonService,
                             MedalProjectionService medalProjectionService) {
        this.checkupService = checkupService;
        this.timeComparisonService = timeComparisonService;
        this.medalProjectionService = medalProjectionService;
    }

    /**
     * Crea un chequeo mensual para un deportista.
     * POST /api/v1/athletes/{athleteId}/checkups
     */
    @PostMapping("/athletes/{athleteId}/checkups")
    @PreAuthorize("hasAnyRole('ADMIN', 'COACH')")
    public ResponseEntity<CheckupResponse> createCheckup(@PathVariable Long athleteId,
                                                          @Valid @RequestBody CreateCheckupRequest request) {
        Checkup checkup = checkupService.createCheckup(athleteId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(CheckupResponse.fromCheckup(checkup));
    }

    /**
     * Lista los chequeos activos de un deportista, con filtros opcionales
     * ?year=&month=.
     * GET /api/v1/athletes/{athleteId}/checkups
     */
    @GetMapping("/athletes/{athleteId}/checkups")
    public ResponseEntity<List<CheckupResponse>> getCheckupsByAthlete(@PathVariable Long athleteId,
                                                                       @RequestParam(required = false) Integer year,
                                                                       @RequestParam(required = false) Integer month) {
        List<CheckupResponse> responses = checkupService.getCheckupsByAthlete(athleteId, year, month).stream()
                .map(CheckupResponse::fromCheckup)
                .toList();
        return ResponseEntity.ok(responses);
    }

    /**
     * Obtiene el detalle de un chequeo con sus tiempos embebidos.
     * GET /api/v1/checkups/{id}
     */
    @GetMapping("/checkups/{id}")
    public ResponseEntity<CheckupDetailResponse> getCheckup(@PathVariable Long id) {
        Checkup checkup = checkupService.getCheckupById(id);
        List<CheckupTime> times = checkupService.getTimesByCheckup(id);
        return ResponseEntity.ok(CheckupDetailResponse.fromCheckup(checkup, times));
    }

    /**
     * Elimina lógicamente un chequeo (soft delete).
     * DELETE /api/v1/checkups/{id}
     */
    @DeleteMapping("/checkups/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COACH')")
    public ResponseEntity<Void> softDeleteCheckup(@PathVariable Long id) {
        checkupService.softDeleteCheckup(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Agrega un tiempo de prueba a un chequeo.
     * POST /api/v1/checkups/{checkupId}/times
     */
    @PostMapping("/checkups/{checkupId}/times")
    @PreAuthorize("hasAnyRole('ADMIN', 'COACH')")
    public ResponseEntity<CheckupTimeResponse> addCheckupTime(@PathVariable Long checkupId,
                                                              @Valid @RequestBody AddCheckupTimeRequest request) {
        CheckupTime checkupTime = checkupService.addCheckupTime(checkupId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(CheckupTimeResponse.fromCheckupTime(checkupTime));
    }

    /**
     * Compara todos los tiempos del chequeo contra la tabla nacional de
     * referencia (deltas absolutos y relativos por posición 1°, 2°, 3°).
     * GET /api/v1/checkups/{checkupId}/comparison
     */
    @GetMapping("/checkups/{checkupId}/comparison")
    public ResponseEntity<List<TimeComparisonResponse>> compareCheckup(@PathVariable Long checkupId) {
        return ResponseEntity.ok(timeComparisonService.compareCheckup(checkupId));
    }

    /**
     * Proyección de medallería del deportista (una entrada por style+distance
     * evaluado, con clasificación y diferencia contra el bronce).
     * GET /api/v1/athletes/{athleteId}/projections
     */
    @GetMapping("/athletes/{athleteId}/projections")
    public ResponseEntity<List<MedalProjectionResponse>> getProjections(@PathVariable Long athleteId) {
        List<MedalProjectionResponse> responses = medalProjectionService.getProjectionsForAthlete(athleteId).stream()
                .map(MedalProjectionResponse::fromMedalProjection)
                .toList();
        return ResponseEntity.ok(responses);
    }
}
