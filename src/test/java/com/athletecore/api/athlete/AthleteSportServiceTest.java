package com.athletecore.api.athlete;

import com.athletecore.api.athlete.dto.AssignSportsRequest;
import com.athletecore.api.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AthleteSportServiceTest {

    @Mock
    private AthleteRepository athleteRepository;

    @Mock
    private SportRepository sportRepository;

    @InjectMocks
    private AthleteSportService athleteSportService;

    private Athlete athlete;
    private Sport swimming;
    private Sport athletics;

    @BeforeEach
    void setUp() {
        athlete = Athlete.builder()
            .id(1L)
            .username("usuario1")
            .email("usuario1@example.com")
            .firstName("Juan")
            .lastName("Perez")
            .build();

        swimming = Sport.builder().id(1L).name("Natación").build();
        athletics = Sport.builder().id(2L).name("Atletismo").build();
    }

    @Test
    @DisplayName("Debe asignar deportes a un atleta")
    void assignSportsToAthlete_Success() {
        // Arrange
        AssignSportsRequest request = new AssignSportsRequest(Set.of(1L, 2L));
        when(athleteRepository.findById(1L)).thenReturn(Optional.of(athlete));
        when(sportRepository.findById(1L)).thenReturn(Optional.of(swimming));
        when(sportRepository.findById(2L)).thenReturn(Optional.of(athletics));
        when(athleteRepository.save(any(Athlete.class))).thenReturn(athlete);

        // Act
        Athlete result = athleteSportService.assignSportsToAthlete(1L, request);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getSports().size());
        assertTrue(result.getSports().contains(swimming));
        assertTrue(result.getSports().contains(athletics));
        verify(athleteRepository, times(1)).save(athlete);
    }

    @Test
    @DisplayName("Debe lanzar ResourceNotFoundException si el atleta no existe")
    void assignSportsToAthlete_AthleteNotFound() {
        // Arrange
        AssignSportsRequest request = new AssignSportsRequest(Set.of(1L));
        when(athleteRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class,
            () -> athleteSportService.assignSportsToAthlete(99L, request));
        verify(athleteRepository, never()).save(any(Athlete.class));
    }

    @Test
    @DisplayName("Debe lanzar ResourceNotFoundException si el atleta está eliminado")
    void assignSportsToAthlete_AthleteDeleted() {
        // Arrange
        AssignSportsRequest request = new AssignSportsRequest(Set.of(1L));
        athlete.setDeletedAt(Instant.now());
        when(athleteRepository.findById(1L)).thenReturn(Optional.of(athlete));

        // Act & Assert
        assertThrows(ResourceNotFoundException.class,
            () -> athleteSportService.assignSportsToAthlete(1L, request));
    }

    @Test
    @DisplayName("Debe lanzar ResourceNotFoundException si un deporte no existe")
    void assignSportsToAthlete_SportNotFound() {
        // Arrange
        AssignSportsRequest request = new AssignSportsRequest(Set.of(1L, 99L));
        when(athleteRepository.findById(1L)).thenReturn(Optional.of(athlete));
        when(sportRepository.findById(1L)).thenReturn(Optional.of(swimming));
        when(sportRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class,
            () -> athleteSportService.assignSportsToAthlete(1L, request));
        verify(athleteRepository, never()).save(any(Athlete.class));
    }

    @Test
    @DisplayName("Debe devolver los deportes activos del atleta")
    void getAthleteSports_Success() {
        // Arrange
        swimming.setDeletedAt(Instant.now());
        athlete.setSports(new HashSet<>(Set.of(swimming, athletics)));
        when(athleteRepository.findById(1L)).thenReturn(Optional.of(athlete));

        // Act
        List<Sport> result = athleteSportService.getAthleteSports(1L);

        // Assert
        assertEquals(1, result.size());
        assertEquals("Atletismo", result.get(0).getName());
    }

    @Test
    @DisplayName("Debe devolver lista vacía cuando el atleta no tiene deportes")
    void getAthleteSports_Empty() {
        // Arrange
        athlete.setSports(null);
        when(athleteRepository.findById(1L)).thenReturn(Optional.of(athlete));

        // Act
        List<Sport> result = athleteSportService.getAthleteSports(1L);

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Debe devolver los IDs de deportes activos del atleta")
    void getAthleteSportIds_Success() {
        // Arrange
        swimming.setDeletedAt(Instant.now());
        athlete.setSports(new HashSet<>(Set.of(swimming, athletics)));
        when(athleteRepository.findById(1L)).thenReturn(Optional.of(athlete));

        // Act
        List<Long> result = athleteSportService.getAthleteSportIds(1L);

        // Assert
        assertEquals(List.of(2L), result);
    }

    @Test
    @DisplayName("Debe lanzar ResourceNotFoundException al obtener deportes de atleta inexistente")
    void getAthleteSports_AthleteNotFound() {
        // Arrange
        when(athleteRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> athleteSportService.getAthleteSports(99L));
    }
}
