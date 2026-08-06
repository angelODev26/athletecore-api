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

import com.athletecore.api.training.dto.CreateCycleRequest;
import com.athletecore.api.training.dto.CreateTrainingPlanRequest;
import com.athletecore.api.training.dto.CycleResponse;
import com.athletecore.api.training.dto.TrainingPlanDetailResponse;
import com.athletecore.api.training.dto.TrainingPlanResponse;
import com.athletecore.api.training.dto.UpdateTrainingPlanRequest;

import jakarta.validation.Valid;

/**
 * Controller REST para planes anuales y jerarquía de ciclos.
 * El CRUD completo (crear, actualizar, eliminar planes/ciclos) es exclusivo de
 * ADMIN; la lectura está disponible para el staff (ADMIN/COACH).
 */
@RestController
@RequestMapping("/api/v1/training-plans")
@PreAuthorize("hasAnyRole('ADMIN', 'COACH')")
public class TrainingPlanController {

    private final TrainingPlanService trainingPlanService;

    public TrainingPlanController(TrainingPlanService trainingPlanService) {
        this.trainingPlanService = trainingPlanService;
    }

    /**
     * Crea un plan anual de entrenamiento.
     * POST /api/v1/training-plans
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TrainingPlanResponse> createPlan(@Valid @RequestBody CreateTrainingPlanRequest request) {
        TrainingPlan plan = trainingPlanService.createPlan(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(TrainingPlanResponse.fromPlan(plan));
    }

    /**
     * Obtiene la lista de planes anuales activos.
     * GET /api/v1/training-plans
     */
    @GetMapping
    public ResponseEntity<List<TrainingPlanResponse>> getAllPlans() {
        List<TrainingPlanResponse> responses = trainingPlanService.getAllPlans().stream()
                .map(TrainingPlanResponse::fromPlan)
                .toList();
        return ResponseEntity.ok(responses);
    }

    /**
     * Obtiene un plan con su jerarquía completa de ciclos.
     * GET /api/v1/training-plans/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<TrainingPlanDetailResponse> getPlan(@PathVariable Long id) {
        TrainingPlan plan = trainingPlanService.getPlanById(id);
        List<TrainingCycle> cycles = trainingPlanService.getCyclesByPlan(id);
        return ResponseEntity.ok(TrainingPlanDetailResponse.fromPlan(plan, cycles));
    }

    /**
     * Actualiza parcialmente un plan anual.
     * PUT /api/v1/training-plans/{id}
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TrainingPlanResponse> updatePlan(@PathVariable Long id,
                                                            @Valid @RequestBody UpdateTrainingPlanRequest request) {
        TrainingPlan plan = trainingPlanService.updatePlan(id, request);
        return ResponseEntity.ok(TrainingPlanResponse.fromPlan(plan));
    }

    /**
     * Elimina lógicamente un plan anual.
     * DELETE /api/v1/training-plans/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> softDeletePlan(@PathVariable Long id) {
        trainingPlanService.softDeletePlan(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Agrega un ciclo (mesociclo/microciclo) a un plan.
     * POST /api/v1/training-plans/{planId}/cycles
     */
    @PostMapping("/{planId}/cycles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CycleResponse> addCycle(@PathVariable Long planId,
                                                  @Valid @RequestBody CreateCycleRequest request) {
        TrainingCycle cycle = trainingPlanService.addCycle(planId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(CycleResponse.fromCycle(cycle));
    }

    /**
     * Obtiene los ciclos activos de un plan.
     * GET /api/v1/training-plans/{planId}/cycles
     */
    @GetMapping("/{planId}/cycles")
    public ResponseEntity<List<CycleResponse>> getCycles(@PathVariable Long planId) {
        List<CycleResponse> responses = trainingPlanService.getCyclesByPlan(planId).stream()
                .map(CycleResponse::fromCycle)
                .toList();
        return ResponseEntity.ok(responses);
    }
}