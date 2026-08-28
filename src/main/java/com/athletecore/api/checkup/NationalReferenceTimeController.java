package com.athletecore.api.checkup;

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

import com.athletecore.api.checkup.dto.NationalReferenceTimeListItemResponse;
import com.athletecore.api.checkup.dto.NationalReferenceTimeRequest;
import com.athletecore.api.checkup.dto.NationalReferenceTimeResponse;

import jakarta.validation.Valid;

/**
 * Controller REST de la tabla nacional de tiempos de referencia.
 * El CRUD administrativo (POST/PUT/DELETE) es exclusivo de ADMIN vía
 * @PreAuthorize; la lectura (GET) está disponible para cualquier usuario
 * autenticado (no requiere @PreAuthorize, lo cubre SecurityConfig).
 */
@RestController
@RequestMapping("/api/v1/national-reference-times")
public class NationalReferenceTimeController {

    private final NationalReferenceTimeService nationalReferenceTimeService;

    public NationalReferenceTimeController(NationalReferenceTimeService nationalReferenceTimeService) {
        this.nationalReferenceTimeService = nationalReferenceTimeService;
    }

    /**
     * Crea un tiempo nacional de referencia (solo ADMIN).
     * POST /api/v1/national-reference-times
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<NationalReferenceTimeResponse> createReference(
            @Valid @RequestBody NationalReferenceTimeRequest request) {
        NationalReferenceTime reference = nationalReferenceTimeService.createReference(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(NationalReferenceTimeResponse.fromNationalReferenceTime(reference));
    }

    /**
     * Lista todos los tiempos de referencia activos (autenticado).
     * GET /api/v1/national-reference-times
     */
    @GetMapping
    public ResponseEntity<List<NationalReferenceTimeListItemResponse>> getAllReferences() {
        List<NationalReferenceTimeListItemResponse> responses =
                nationalReferenceTimeService.getAllReferences().stream()
                        .map(NationalReferenceTimeListItemResponse::fromNationalReferenceTime)
                        .toList();
        return ResponseEntity.ok(responses);
    }

    /**
     * Obtiene el detalle de un tiempo de referencia (autenticado).
     * GET /api/v1/national-reference-times/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<NationalReferenceTimeResponse> getReference(@PathVariable Long id) {
        NationalReferenceTime reference = nationalReferenceTimeService.getReferenceById(id);
        return ResponseEntity.ok(NationalReferenceTimeResponse.fromNationalReferenceTime(reference));
    }

    /**
     * Actualiza un tiempo nacional de referencia (solo ADMIN).
     * PUT /api/v1/national-reference-times/{id}
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<NationalReferenceTimeResponse> updateReference(
            @PathVariable Long id, @Valid @RequestBody NationalReferenceTimeRequest request) {
        NationalReferenceTime reference = nationalReferenceTimeService.updateReference(id, request);
        return ResponseEntity.ok(NationalReferenceTimeResponse.fromNationalReferenceTime(reference));
    }

    /**
     * Elimina lógicamente un tiempo nacional de referencia (solo ADMIN).
     * DELETE /api/v1/national-reference-times/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> softDeleteReference(@PathVariable Long id) {
        nationalReferenceTimeService.softDeleteReference(id);
        return ResponseEntity.noContent().build();
    }
}
