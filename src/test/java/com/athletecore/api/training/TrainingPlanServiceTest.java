package com.athletecore.api.training;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.athletecore.api.common.exception.ResourceNotFoundException;
import com.athletecore.api.common.exception.ValidationException;
import com.athletecore.api.training.dto.CreateCycleRequest;
import com.athletecore.api.training.dto.CreateTrainingPlanRequest;
import com.athletecore.api.training.dto.UpdateTrainingPlanRequest;

@ExtendWith(MockitoExtension.class)
class TrainingPlanServiceTest {

    @Mock
    private TrainingPlanRepository planRepository;

    @Mock
    private TrainingCycleRepository cycleRepository;

    @Mock
    private TrainingSessionRepository sessionRepository;

    @Mock
    private AttendanceRepository attendanceRepository;

    @InjectMocks
    private TrainingPlanService planService;

    private LocalDate planStart;
    private LocalDate planEnd;
    private TrainingPlan plan;
    private CreateTrainingPlanRequest createRequest;

    @BeforeEach
    void setUp() {
        planStart = LocalDate.of(2026, 1, 5);
        planEnd = LocalDate.of(2026, 12, 20);
        plan = TrainingPlan.builder()
                .id(1L)
                .name("Plan anual 2026")
                .startDate(planStart)
                .endDate(planEnd)
                .description("Plan base")
                .build();
        createRequest = new CreateTrainingPlanRequest("Plan anual 2026", planStart, planEnd, "Plan base");
    }

    @Test
    @DisplayName("Debe crear un plan anual exitosamente")
    void createPlan_Success() {
        when(planRepository.save(any(TrainingPlan.class))).thenAnswer(inv -> inv.getArgument(0));

        TrainingPlan result = planService.createPlan(createRequest);

        assertNotNull(result);
        assertEquals("Plan anual 2026", result.getName());
        assertEquals(planStart, result.getStartDate());
        assertEquals(planEnd, result.getEndDate());
        verify(planRepository, times(1)).save(any(TrainingPlan.class));
    }

    @Test
    @DisplayName("Debe rechazar la creación cuando la fecha de fin es anterior a la de inicio")
    void createPlan_EndDateBeforeStartDate_Throws() {
        CreateTrainingPlanRequest invalid = new CreateTrainingPlanRequest(
                "Inválido", LocalDate.of(2026, 6, 1), LocalDate.of(2026, 5, 1), null);

        assertThrows(ValidationException.class, () -> planService.createPlan(invalid));
        verify(planRepository, never()).save(any(TrainingPlan.class));
    }

    @Test
    @DisplayName("Debe devolver la lista de planes activos")
    void getAllPlans_ReturnsList() {
        when(planRepository.findAllByDeletedAtIsNullOrderByStartDateAsc()).thenReturn(List.of(plan));

        List<TrainingPlan> result = planService.getAllPlans();

        assertEquals(1, result.size());
        assertEquals("Plan anual 2026", result.get(0).getName());
    }

    @Test
    @DisplayName("Debe obtener un plan por ID")
    void getPlanById_Success() {
        when(planRepository.findById(1L)).thenReturn(Optional.of(plan));

        TrainingPlan result = planService.getPlanById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    @DisplayName("Debe lanzar ResourceNotFoundException si el plan no existe")
    void getPlanById_NotFound() {
        when(planRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> planService.getPlanById(99L));
    }

    @Test
    @DisplayName("Debe obtener los ciclos de un plan existente")
    void getCyclesByPlan_Success() {
        when(planRepository.findById(1L)).thenReturn(Optional.of(plan));
        when(cycleRepository.findByPlanIdOrderByOrderIndexAscStartDateAsc(1L)).thenReturn(List.of());

        List<TrainingCycle> result = planService.getCyclesByPlan(1L);

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    @DisplayName("Debe lanzar ResourceNotFoundException al listar ciclos de un plan inexistente")
    void getCyclesByPlan_PlanNotFound() {
        when(planRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> planService.getCyclesByPlan(99L));
    }

    @Test
    @DisplayName("Debe actualizar parcialmente un plan")
    void updatePlan_Success() {
        UpdateTrainingPlanRequest request = new UpdateTrainingPlanRequest("Plan 2026 actualizado", null, null, "Nueva desc");
        when(planRepository.findById(1L)).thenReturn(Optional.of(plan));
        when(planRepository.save(any(TrainingPlan.class))).thenAnswer(inv -> inv.getArgument(0));

        TrainingPlan result = planService.updatePlan(1L, request);

        assertEquals("Plan 2026 actualizado", result.getName());
        assertEquals("Nueva desc", result.getDescription());
        // Las fechas no enviadas se conservan
        assertEquals(planStart, result.getStartDate());
        verify(planRepository, times(1)).save(plan);
    }

    @Test
    @DisplayName("Debe lanzar ResourceNotFoundException al actualizar un plan inexistente")
    void updatePlan_NotFound() {
        when(planRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> planService.updatePlan(99L, new UpdateTrainingPlanRequest("x", null, null, null)));
        verify(planRepository, never()).save(any(TrainingPlan.class));
    }

    @Test
    @DisplayName("Debe eliminar lógicamente un plan (soft delete) sin ciclos asociados")
    void softDeletePlan_Success() {
        when(planRepository.findById(1L)).thenReturn(Optional.of(plan));
        when(planRepository.save(any(TrainingPlan.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cycleRepository.findIdsByPlanId(1L)).thenReturn(List.of());

        planService.softDeletePlan(1L);

        assertNotNull(plan.getDeletedAt());
        verify(planRepository, times(1)).save(plan);
        verify(planRepository, never()).delete(any(TrainingPlan.class));
        verify(cycleRepository, never()).softDeleteByPlanId(any(), any());
    }

    @Test
    @DisplayName("Debe lanzar ResourceNotFoundException al eliminar un plan inexistente")
    void softDeletePlan_NotFound() {
        when(planRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> planService.softDeletePlan(99L));
    }

    @Test
    @DisplayName("Debe propagar el soft delete a ciclos, sesiones y asistencias en cascada")
    void softDeletePlan_CascadesToCyclesSessionsAndAttendance() {
        when(planRepository.findById(1L)).thenReturn(Optional.of(plan));
        when(planRepository.save(any(TrainingPlan.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cycleRepository.findIdsByPlanId(1L)).thenReturn(List.of(10L, 20L));
        when(sessionRepository.findActiveIdsByCycleIds(List.of(10L, 20L))).thenReturn(List.of(100L, 200L));
        when(cycleRepository.softDeleteByPlanId(any(), any())).thenReturn(2);
        when(sessionRepository.softDeleteByCycleIds(any(), any())).thenReturn(2);
        when(attendanceRepository.softDeleteBySessionIds(any(), any())).thenReturn(5);

        planService.softDeletePlan(1L);

        assertNotNull(plan.getDeletedAt());
        verify(cycleRepository, times(1)).softDeleteByPlanId(eq(1L), eq(plan.getDeletedAt()));
        verify(sessionRepository, times(1)).softDeleteByCycleIds(eq(List.of(10L, 20L)), eq(plan.getDeletedAt()));
        verify(attendanceRepository, times(1)).softDeleteBySessionIds(eq(List.of(100L, 200L)), eq(plan.getDeletedAt()));
    }

    @Test
    @DisplayName("No debe propagar a sesiones si el plan no tiene ciclos")
    void softDeletePlan_NoCycles_StopsAfterPlanDelete() {
        when(planRepository.findById(1L)).thenReturn(Optional.of(plan));
        when(planRepository.save(any(TrainingPlan.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cycleRepository.findIdsByPlanId(1L)).thenReturn(List.of());

        planService.softDeletePlan(1L);

        verify(cycleRepository, never()).softDeleteByPlanId(any(), any());
        verify(sessionRepository, never()).findActiveIdsByCycleIds(any());
        verify(attendanceRepository, never()).softDeleteBySessionIds(any(), any());
    }

    @Test
    @DisplayName("No debe propagar a asistencias si los ciclos no tienen sesiones")
    void softDeletePlan_NoSessions_StopsAfterCyclesDelete() {
        when(planRepository.findById(1L)).thenReturn(Optional.of(plan));
        when(planRepository.save(any(TrainingPlan.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cycleRepository.findIdsByPlanId(1L)).thenReturn(List.of(10L));
        when(cycleRepository.softDeleteByPlanId(any(), any())).thenReturn(1);
        when(sessionRepository.findActiveIdsByCycleIds(List.of(10L))).thenReturn(List.of());

        planService.softDeletePlan(1L);

        verify(cycleRepository, times(1)).softDeleteByPlanId(eq(1L), any());
        verify(sessionRepository, never()).softDeleteByCycleIds(any(), any());
        verify(attendanceRepository, never()).softDeleteBySessionIds(any(), any());
    }

    @Test
    @DisplayName("Debe agregar un mesociclo a un plan")
    void addCycle_Mesocycle_Success() {
        CreateCycleRequest request = new CreateCycleRequest(
                CycleType.MESOCICLO, "Mesociclo 1", LocalDate.of(2026, 1, 5), LocalDate.of(2026, 3, 31), 1, null);
        when(planRepository.findById(1L)).thenReturn(Optional.of(plan));
        when(cycleRepository.save(any(TrainingCycle.class))).thenAnswer(inv -> inv.getArgument(0));

        TrainingCycle result = planService.addCycle(1L, request);

        assertNotNull(result);
        assertEquals(CycleType.MESOCICLO, result.getType());
        assertEquals(1, result.getOrderIndex());
        assertEquals(plan, result.getPlan());
        verify(cycleRepository, times(1)).save(any(TrainingCycle.class));
    }

    @Test
    @DisplayName("Debe lanzar ResourceNotFoundException al agregar un ciclo a un plan inexistente")
    void addCycle_PlanNotFound() {
        CreateCycleRequest request = new CreateCycleRequest(
                CycleType.MESOCICLO, "Ciclo", LocalDate.of(2026, 1, 5), LocalDate.of(2026, 3, 31), 1, null);
        when(planRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> planService.addCycle(99L, request));
        verify(cycleRepository, never()).save(any(TrainingCycle.class));
    }

    @Test
    @DisplayName("Debe rechazar un microciclo sin mesociclo padre")
    void addCycle_MicrocycleRequiresParent_Throws() {
        CreateCycleRequest request = new CreateCycleRequest(
                CycleType.MICROCICLO, "Micro", LocalDate.of(2026, 1, 5), LocalDate.of(2026, 1, 12), 1, null);
        when(planRepository.findById(1L)).thenReturn(Optional.of(plan));

        assertThrows(ValidationException.class, () -> planService.addCycle(1L, request));
        verify(cycleRepository, never()).save(any(TrainingCycle.class));
    }

    @Test
    @DisplayName("Debe agregar un microciclo bajo un mesociclo")
    void addCycle_MicrocycleUnderMesocycle_Success() {
        LocalDate parentStart = LocalDate.of(2026, 1, 5);
        LocalDate parentEnd = LocalDate.of(2026, 3, 31);
        TrainingCycle parent = TrainingCycle.builder()
                .id(10L)
                .plan(plan)
                .type(CycleType.MESOCICLO)
                .name("Mesociclo 1")
                .startDate(parentStart)
                .endDate(parentEnd)
                .build();

        CreateCycleRequest request = new CreateCycleRequest(
                CycleType.MICROCICLO, "Micro 1", LocalDate.of(2026, 1, 5), LocalDate.of(2026, 1, 31), 1, 10L);

        when(planRepository.findById(1L)).thenReturn(Optional.of(plan));
        when(cycleRepository.findById(10L)).thenReturn(Optional.of(parent));
        when(cycleRepository.save(any(TrainingCycle.class))).thenAnswer(inv -> inv.getArgument(0));

        TrainingCycle result = planService.addCycle(1L, request);

        assertNotNull(result);
        assertEquals(CycleType.MICROCICLO, result.getType());
        assertEquals(parent, result.getParent());
    }

    @Test
    @DisplayName("Debe rechazar un microciclo bajo otro microciclo")
    void addCycle_MicrocycleUnderMicrocycle_Throws() {
        TrainingCycle parent = TrainingCycle.builder()
                .id(10L)
                .plan(plan)
                .type(CycleType.MICROCICLO)
                .name("Micro padre")
                .startDate(planStart)
                .endDate(planEnd)
                .build();
        CreateCycleRequest request = new CreateCycleRequest(
                CycleType.MICROCICLO, "Micro hij.", LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28), 1, 10L);

        when(planRepository.findById(1L)).thenReturn(Optional.of(plan));
        when(cycleRepository.findById(10L)).thenReturn(Optional.of(parent));

        assertThrows(ValidationException.class, () -> planService.addCycle(1L, request));
        verify(cycleRepository, never()).save(any(TrainingCycle.class));
    }

    @Test
    @DisplayName("Debe rechazar un ciclo con fechas fuera del rango del plan")
    void addCycle_DatesOutsidePlan_Throws() {
        CreateCycleRequest request = new CreateCycleRequest(
                CycleType.MESOCICLO, "Fuera", LocalDate.of(2027, 1, 1), LocalDate.of(2027, 3, 1), 1, null);

        when(planRepository.findById(1L)).thenReturn(Optional.of(plan));

        assertThrows(ValidationException.class, () -> planService.addCycle(1L, request));
        verify(cycleRepository, never()).save(any(TrainingCycle.class));
    }

    @Test
    @DisplayName("Debe rechazar un mesociclo con padre indicado")
    void addCycle_MesocycleWithParent_Throws() {
        CreateCycleRequest request = new CreateCycleRequest(
                CycleType.MESOCICLO, "Ciclo", planStart, planEnd, 1, 10L);

        when(planRepository.findById(1L)).thenReturn(Optional.of(plan));

        assertThrows(ValidationException.class, () -> planService.addCycle(1L, request));
        verify(cycleRepository, never()).save(any(TrainingCycle.class));
    }

    @Test
    @DisplayName("Debe rechazar un ciclo con nombre duplicado dentro del plan")
    void addCycle_DuplicateName_Throws() {
        CreateCycleRequest request = new CreateCycleRequest(
                CycleType.MESOCICLO, "Mesociclo 1", planStart, planEnd, 5, null);
        when(planRepository.findById(1L)).thenReturn(Optional.of(plan));
        when(cycleRepository.existsByPlanIdAndName(1L, "Mesociclo 1")).thenReturn(true);

        assertThrows(ValidationException.class, () -> planService.addCycle(1L, request));
        verify(cycleRepository, never()).save(any(TrainingCycle.class));
    }

    @Test
    @DisplayName("Debe rechazar un mesociclo raíz con order_index ya existente en el plan")
    void addCycle_MesocycleDuplicateOrderIndex_Throws() {
        CreateCycleRequest request = new CreateCycleRequest(
                CycleType.MESOCICLO, "Mesociclo 2", planStart, planEnd, 1, null);
        when(planRepository.findById(1L)).thenReturn(Optional.of(plan));
        when(cycleRepository.existsByPlanIdAndName(1L, "Mesociclo 2")).thenReturn(false);
        when(cycleRepository.existsByPlanIdAndParentAndOrderIndex(1L, null, 1)).thenReturn(true);

        assertThrows(ValidationException.class, () -> planService.addCycle(1L, request));
        verify(cycleRepository, never()).save(any(TrainingCycle.class));
    }

    @Test
    @DisplayName("Debe rechazar un microciclo con order_index ya existente bajo el mismo mesociclo padre")
    void addCycle_MicrocycleDuplicateOrderIndex_Throws() {
        TrainingCycle parent = TrainingCycle.builder()
                .id(10L)
                .plan(plan)
                .type(CycleType.MESOCICLO)
                .name("Mesociclo 1")
                .startDate(planStart)
                .endDate(planEnd)
                .build();
        CreateCycleRequest request = new CreateCycleRequest(
                CycleType.MICROCICLO, "Micro 2", planStart, planEnd, 2, 10L);

        when(planRepository.findById(1L)).thenReturn(Optional.of(plan));
        when(cycleRepository.findById(10L)).thenReturn(Optional.of(parent));
        when(cycleRepository.existsByPlanIdAndName(1L, "Micro 2")).thenReturn(false);
        when(cycleRepository.existsByPlanIdAndParentAndOrderIndex(1L, 10L, 2)).thenReturn(true);

        assertThrows(ValidationException.class, () -> planService.addCycle(1L, request));
        verify(cycleRepository, never()).save(any(TrainingCycle.class));
    }

    @Test
    @DisplayName("Debe admitir order_index null sin validar unicidad")
    void addCycle_NullOrderIndex_SkipsUniqueness() {
        CreateCycleRequest request = new CreateCycleRequest(
                CycleType.MESOCICLO, "Sin orden", planStart, planEnd, null, null);
        when(planRepository.findById(1L)).thenReturn(Optional.of(plan));
        when(cycleRepository.existsByPlanIdAndName(1L, "Sin orden")).thenReturn(false);
        when(cycleRepository.save(any(TrainingCycle.class))).thenAnswer(inv -> inv.getArgument(0));

        TrainingCycle result = planService.addCycle(1L, request);

        assertNotNull(result);
        verify(cycleRepository, never()).existsByPlanIdAndParentAndOrderIndex(any(), any(), any());
    }
}