package com.athletecore.api.training;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.athletecore.api.athlete.Discipline;
import com.athletecore.api.athlete.DisciplineRepository;
import com.athletecore.api.common.exception.ResourceNotFoundException;
import com.athletecore.api.common.exception.ValidationException;
import com.athletecore.api.training.dto.CreateSessionRequest;
import com.athletecore.api.training.dto.UpdateSessionRequest;

@ExtendWith(MockitoExtension.class)
class TrainingSessionServiceTest {

    @Mock
    private TrainingSessionRepository sessionRepository;

    @Mock
    private TrainingCycleRepository cycleRepository;

    @Mock
    private DisciplineRepository disciplineRepository;

    @Mock
    private AttendanceRepository attendanceRepository;

    @InjectMocks
    private TrainingSessionService sessionService;

    private LocalDate sessionDate;
    private TrainingSession session;
    private CreateSessionRequest createRequest;

    @BeforeEach
    void setUp() {
        sessionDate = LocalDate.of(2026, 8, 10);
        session = TrainingSession.builder()
                .id(1L)
                .sessionDate(sessionDate)
                .status(SessionStatus.PROGRAMADA)
                .volume(3000)
                .intensity(70)
                .build();
        createRequest = new CreateSessionRequest(null, null, sessionDate, LocalTime.of(7, 0), null,
                3000, 70, 1000.0, "Sesión de fondo");
    }

    @Test
    @DisplayName("Debe registrar una sesión sin ciclo asignando estado PROGRAMADA por defecto")
    void createSession_Success_DefaultsProgramada() {
        when(sessionRepository.save(any(TrainingSession.class))).thenAnswer(inv -> inv.getArgument(0));

        TrainingSession result = sessionService.createSession(createRequest);

        assertNotNull(result);
        assertNull(result.getCycle());
        assertEquals(SessionStatus.PROGRAMADA, result.getStatus());
        assertEquals(3000, result.getVolume());
        assertEquals(70, result.getIntensity());
        verify(sessionRepository, times(1)).save(any(TrainingSession.class));
    }

    @Test
    @DisplayName("Debe registrar una sesión dentro de un ciclo con disciplina")
    void createSession_WithCycleAndDiscipline() {
        TrainingCycle cycle = TrainingCycle.builder().id(5L).build();
        Discipline discipline = Discipline.builder().id(3L).name("Estilo Libre").build();
        CreateSessionRequest request = new CreateSessionRequest(5L, 3L, sessionDate, LocalTime.of(7, 0),
                SessionStatus.PROGRAMADA, 3000, 70, 1000.0, null);

        when(cycleRepository.findById(5L)).thenReturn(Optional.of(cycle));
        when(disciplineRepository.findById(3L)).thenReturn(Optional.of(discipline));
        when(sessionRepository.save(any(TrainingSession.class))).thenAnswer(inv -> inv.getArgument(0));

        TrainingSession result = sessionService.createSession(request);

        assertEquals(5L, result.getCycle().getId());
        assertEquals(3L, result.getDiscipline().getId());
    }

    @Test
    @DisplayName("Debe lanzar ResourceNotFoundException si el ciclo no existe")
    void createSession_CycleNotFound() {
        CreateSessionRequest request = new CreateSessionRequest(99L, null, sessionDate, null,
                SessionStatus.PROGRAMADA, 1000, 50, null, null);
        when(cycleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> sessionService.createSession(request));
        verify(sessionRepository, never()).save(any(TrainingSession.class));
    }

    @Test
    @DisplayName("Debe lanzar ResourceNotFoundException si la disciplina no existe")
    void createSession_DisciplineNotFound() {
        CreateSessionRequest request = new CreateSessionRequest(null, 99L, sessionDate, null,
                SessionStatus.PROGRAMADA, 1000, 50, null, null);
        when(disciplineRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> sessionService.createSession(request));
        verify(sessionRepository, never()).save(any(TrainingSession.class));
    }

    @Test
    @DisplayName("Debe registrar una sesión a través del endpoint anidado de ciclo")
    void createSessionForCycle_Success() {
        TrainingCycle cycle = TrainingCycle.builder().id(5L).build();
        CreateSessionRequest request = new CreateSessionRequest(null, null, sessionDate, null,
                SessionStatus.EJECUTADA, 2000, 60, null, null);

        when(cycleRepository.findById(5L)).thenReturn(Optional.of(cycle));
        when(sessionRepository.save(any(TrainingSession.class))).thenAnswer(inv -> inv.getArgument(0));

        TrainingSession result = sessionService.createSessionForCycle(5L, request);

        assertEquals(5L, result.getCycle().getId());
        assertEquals(SessionStatus.EJECUTADA, result.getStatus());
    }

    @Test
    @DisplayName("Debe rechazar la sesión si el ciclo del cuerpo no coincide con la ruta")
    void createSessionForCycle_MismatchedCycle_Throws() {
        CreateSessionRequest request = new CreateSessionRequest(6L, null, sessionDate, null, null, 100, 50, null, null);

        assertThrows(ValidationException.class, () -> sessionService.createSessionForCycle(5L, request));
        verify(sessionRepository, never()).save(any(TrainingSession.class));
    }

    @Test
    @DisplayName("Debe listar las sesiones de un ciclo ordenadas por fecha")
    void getSessionsByCycle_Success() {
        when(cycleRepository.findById(5L)).thenReturn(Optional.of(TrainingCycle.builder().id(5L).build()));
        when(sessionRepository.findByCycleIdOrderBySessionDateAsc(5L)).thenReturn(List.of(session));

        List<TrainingSession> result = sessionService.getSessionsByCycle(5L);

        assertEquals(1, result.size());
        assertEquals(sessionDate, result.get(0).getSessionDate());
    }

    @Test
    @DisplayName("Debe lanzar ResourceNotFoundException al listar sesiones de un ciclo inexistente")
    void getSessionsByCycle_CycleNotFound() {
        when(cycleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> sessionService.getSessionsByCycle(99L));
    }

    @Test
    @DisplayName("Debe devolver lista vacía para un ciclo sin sesiones")
    void getSessionsByCycle_Empty() {
        when(cycleRepository.findById(5L)).thenReturn(Optional.of(TrainingCycle.builder().id(5L).build()));
        when(sessionRepository.findByCycleIdOrderBySessionDateAsc(5L)).thenReturn(List.of());

        List<TrainingSession> result = sessionService.getSessionsByCycle(5L);

        assertEquals(0, result.size());
    }

    @Test
    @DisplayName("Debe obtener una sesión por ID")
    void getSessionById_Success() {
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));

        TrainingSession result = sessionService.getSessionById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    @DisplayName("Debe lanzar ResourceNotFoundException si la sesión no existe")
    void getSessionById_NotFound() {
        when(sessionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> sessionService.getSessionById(99L));
    }

    @Test
    @DisplayName("Debe actualizar parcialmente una sesión")
    void updateSession_Success() {
        UpdateSessionRequest request = new UpdateSessionRequest(null, null, null, SessionStatus.EJECUTADA,
                null, 80, null, "Nota nueva");
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(sessionRepository.save(any(TrainingSession.class))).thenAnswer(inv -> inv.getArgument(0));

        TrainingSession result = sessionService.updateSession(1L, request);

        assertEquals(SessionStatus.EJECUTADA, result.getStatus());
        assertEquals(80, result.getIntensity());
        assertEquals(3000, result.getVolume());
        assertEquals("Nota nueva", result.getObservations());
        verify(sessionRepository, times(1)).save(session);
    }

    @Test
    @DisplayName("Debe lanzar ResourceNotFoundException al actualizar una sesión inexistente")
    void updateSession_NotFound() {
        when(sessionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> sessionService.updateSession(99L, new UpdateSessionRequest(null, null, null, null, null, null, null, null)));
        verify(sessionRepository, never()).save(any(TrainingSession.class));
    }

    @Test
    @DisplayName("Debe cambiar el estado de una sesión")
    void changeStatus_Success() {
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(sessionRepository.save(any(TrainingSession.class))).thenAnswer(inv -> inv.getArgument(0));

        TrainingSession result = sessionService.changeStatus(1L, SessionStatus.EJECUTADA);

        assertEquals(SessionStatus.EJECUTADA, result.getStatus());
    }

    @Test
    @DisplayName("Debe lanzar ResourceNotFoundException al cambiar el estado de una sesión inexistente")
    void changeStatus_NotFound() {
        when(sessionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> sessionService.changeStatus(99L, SessionStatus.EJECUTADA));
    }

    @Test
    @DisplayName("Debe eliminar lógicamente una sesión (soft delete) y propagar a sus asistencias")
    void softDeleteSession_Success() {
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(sessionRepository.save(any(TrainingSession.class))).thenAnswer(inv -> inv.getArgument(0));
        when(attendanceRepository.softDeleteBySessionIds(any(), any())).thenReturn(2);

        sessionService.softDeleteSession(1L);

        assertNotNull(session.getDeletedAt());
        verify(sessionRepository, times(1)).save(session);
        verify(sessionRepository, never()).delete(any(TrainingSession.class));
        verify(attendanceRepository, times(1)).softDeleteBySessionIds(List.of(1L), session.getDeletedAt());
    }

    @Test
    @DisplayName("Debe lanzar ResourceNotFoundException al eliminar una sesión inexistente")
    void softDeleteSession_NotFound() {
        when(sessionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> sessionService.softDeleteSession(99L));
        verify(sessionRepository, never()).save(any(TrainingSession.class));
        verify(attendanceRepository, never()).softDeleteBySessionIds(any(), any());
    }
}