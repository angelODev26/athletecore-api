package com.athletecore.api.training;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

import com.athletecore.api.athlete.Athlete;
import com.athletecore.api.athlete.AthleteRepository;
import com.athletecore.api.common.exception.ResourceNotFoundException;
import com.athletecore.api.common.exception.ValidationException;
import com.athletecore.api.training.dto.RegisterAttendanceRequest;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private TrainingSessionRepository sessionRepository;

    @Mock
    private AthleteRepository athleteRepository;

    @InjectMocks
    private AttendanceService attendanceService;

    private TrainingSession session;
    private Athlete athlete;
    private RegisterAttendanceRequest request;

    @BeforeEach
    void setUp() {
        session = TrainingSession.builder()
                .id(1L)
                .sessionDate(LocalDate.of(2026, 8, 10))
                .status(SessionStatus.PROGRAMADA)
                .build();
        athlete = Athlete.builder()
                .id(7L)
                .firstName("Juan")
                .lastName("Perez")
                .build();
        request = new RegisterAttendanceRequest(7L, AttendanceStatus.PRESENTE);
    }

    @Test
    @DisplayName("Debe crear un registro de asistencia nuevo")
    void registerAttendance_CreatesNew() {
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(athleteRepository.findById(7L)).thenReturn(Optional.of(athlete));
        when(attendanceRepository.findBySessionIdAndAthleteId(1L, 7L)).thenReturn(Optional.empty());
        when(attendanceRepository.save(any(Attendance.class))).thenAnswer(inv -> inv.getArgument(0));

        Attendance result = attendanceService.registerAttendance(1L, request);

        assertNotNull(result);
        assertEquals(AttendanceStatus.PRESENTE, result.getStatus());
        assertEquals(1L, result.getSession().getId());
        assertEquals(7L, result.getAthlete().getId());
        verify(attendanceRepository, times(1)).save(any(Attendance.class));
    }

    @Test
    @DisplayName("Debe actualizar (upsert) el registro existente al re-registrar asistencia")
    void registerAttendance_UpsertsExisting() {
        Attendance existing = Attendance.builder()
                .id(100L)
                .session(session)
                .athlete(athlete)
                .status(AttendanceStatus.AUSENTE)
                .build();
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(athleteRepository.findById(7L)).thenReturn(Optional.of(athlete));
        when(attendanceRepository.findBySessionIdAndAthleteId(1L, 7L)).thenReturn(Optional.of(existing));
        when(attendanceRepository.save(any(Attendance.class))).thenAnswer(inv -> inv.getArgument(0));

        Attendance result = attendanceService.registerAttendance(1L, new RegisterAttendanceRequest(7L, AttendanceStatus.PRESENTE));

        assertEquals(100L, result.getId());
        assertEquals(AttendanceStatus.PRESENTE, result.getStatus());
        verify(attendanceRepository, times(1)).save(existing);
    }

    @Test
    @DisplayName("Debe lanzar ResourceNotFoundException si la sesión no existe")
    void registerAttendance_SessionNotFound() {
        when(sessionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> attendanceService.registerAttendance(99L, request));
        verify(attendanceRepository, never()).save(any(Attendance.class));
    }

    @Test
    @DisplayName("Debe lanzar ResourceNotFoundException si el deportista no existe")
    void registerAttendance_AthleteNotFound() {
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(athleteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> attendanceService.registerAttendance(1L, new RegisterAttendanceRequest(99L, AttendanceStatus.PRESENTE)));
        verify(attendanceRepository, never()).save(any(Attendance.class));
    }

    @Test
    @DisplayName("Debe rechazar el registro de asistencia en una sesión cancelada")
    void registerAttendance_CancelledSession_Throws() {
        TrainingSession cancelled = TrainingSession.builder()
                .id(2L)
                .status(SessionStatus.CANCELADA)
                .build();
        when(sessionRepository.findById(2L)).thenReturn(Optional.of(cancelled));

        assertThrows(ValidationException.class, () -> attendanceService.registerAttendance(2L, request));
        verify(attendanceRepository, never()).save(any(Attendance.class));
    }

    @Test
    @DisplayName("Debe listar la asistencia de una sesión")
    void getAttendanceBySession_Success() {
        Attendance attendance = Attendance.builder().id(100L).session(session).athlete(athlete).status(AttendanceStatus.PRESENTE).build();
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(attendanceRepository.findBySessionId(1L)).thenReturn(List.of(attendance));

        List<Attendance> result = attendanceService.getAttendanceBySession(1L);

        assertEquals(1, result.size());
        assertEquals(AttendanceStatus.PRESENTE, result.get(0).getStatus());
    }

    @Test
    @DisplayName("Debe lanzar ResourceNotFoundException al listar asistencia de sesión inexistente")
    void getAttendanceBySession_NotFound() {
        when(sessionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> attendanceService.getAttendanceBySession(99L));
    }

    @Test
    @DisplayName("Debe listar el historial de asistencia de un deportista")
    void getAttendanceByAthlete_Success() {
        Attendance attendance = Attendance.builder().id(100L).session(session).athlete(athlete).status(AttendanceStatus.JUSTIFICADO).build();
        when(athleteRepository.findById(7L)).thenReturn(Optional.of(athlete));
        when(attendanceRepository.findByAthleteIdOrderBySessionSessionDateAsc(7L)).thenReturn(List.of(attendance));

        List<Attendance> result = attendanceService.getAttendanceByAthlete(7L);

        assertEquals(1, result.size());
        assertEquals(AttendanceStatus.JUSTIFICADO, result.get(0).getStatus());
    }

    @Test
    @DisplayName("Debe devolver lista vacía para un deportista sin asistencia")
    void getAttendanceByAthlete_Empty() {
        when(athleteRepository.findById(7L)).thenReturn(Optional.of(athlete));
        when(attendanceRepository.findByAthleteIdOrderBySessionSessionDateAsc(7L)).thenReturn(List.of());

        List<Attendance> result = attendanceService.getAttendanceByAthlete(7L);

        assertEquals(0, result.size());
    }

    @Test
    @DisplayName("Debe lanzar ResourceNotFoundException al listar historial de deportista inexistente")
    void getAttendanceByAthlete_NotFound() {
        when(athleteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> attendanceService.getAttendanceByAthlete(99L));
    }
}