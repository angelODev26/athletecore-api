package com.athletecore.api.athlete;

import com.athletecore.api.athlete.dto.CreateAthleteRequest;
import com.athletecore.api.athlete.dto.UpdateAthleteRequest;
import com.athletecore.api.common.exception.DuplicateResourceException;
import com.athletecore.api.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AthleteRegistrationServiceTest {

    @Mock
    private AthleteRepository athleteRepository;

    @InjectMocks
    private AthleteRegistrationService athleteService;

    private CreateAthleteRequest createAthleteRequest;
    private Athlete newAthlete;

    @BeforeEach
    void setUp() {
        createAthleteRequest = new CreateAthleteRequest(
            "usuario1", "usuario1@example.com", "Juan", "Perez", "http://foto.png");

        newAthlete = Athlete.builder()
            .id(1L)
            .username("usuario1")
            .email("usuario1@example.com")
            .firstName("Juan")
            .lastName("Perez")
            .photoUrl("http://foto.png")
            .build();
    }

    @Test
    @DisplayName("Debe registrar un deportista exitosamente")
    void registerAthlete_Success() {
        // Arrange
        when(athleteRepository.existsByUsername("usuario1")).thenReturn(false);
        when(athleteRepository.existsByEmail("usuario1@example.com")).thenReturn(false);
        when(athleteRepository.save(any(Athlete.class))).thenReturn(newAthlete);

        // Act
        Athlete result = athleteService.registerAthlete(createAthleteRequest);

        // Assert
        assertNotNull(result);
        assertEquals("usuario1", result.getUsername());
        assertEquals("usuario1@example.com", result.getEmail());
        assertEquals("Juan", result.getFirstName());
        assertEquals("Perez", result.getLastName());
        assertEquals("http://foto.png", result.getPhotoUrl());
        verify(athleteRepository, times(1)).save(any(Athlete.class));
    }

    @Test
    @DisplayName("Debe lanzar DuplicateResourceException cuando el username ya existe")
    void registerAthlete_DuplicateUsername() {
        // Arrange
        when(athleteRepository.existsByUsername("usuario1")).thenReturn(true);

        // Act & Assert
        assertThrows(DuplicateResourceException.class,
            () -> athleteService.registerAthlete(createAthleteRequest));
        verify(athleteRepository, never()).save(any(Athlete.class));
    }

    @Test
    @DisplayName("Debe lanzar DuplicateResourceException cuando el email ya existe")
    void registerAthlete_DuplicateEmail() {
        // Arrange
        when(athleteRepository.existsByUsername("usuario1")).thenReturn(false);
        when(athleteRepository.existsByEmail("usuario1@example.com")).thenReturn(true);

        // Act & Assert
        assertThrows(DuplicateResourceException.class,
            () -> athleteService.registerAthlete(createAthleteRequest));
        verify(athleteRepository, never()).save(any(Athlete.class));
    }

    @Test
    @DisplayName("Debe devolver página de deportistas activos")
    void getAllAthletes_ReturnsPage() {
        // Arrange
        PageRequest pageable = PageRequest.of(0, 10);
        Page<Athlete> page = new PageImpl<>(List.of(newAthlete));
        when(athleteRepository.findAllByDeletedAtIsNull(pageable)).thenReturn(page);

        // Act
        Page<Athlete> result = athleteService.getAllAthletes(pageable);

        // Assert
        assertEquals(1, result.getTotalElements());
        assertEquals("usuario1", result.getContent().get(0).getUsername());
    }

    @Test
    @DisplayName("Debe obtener un deportista por ID")
    void getAthleteById_Success() {
        // Arrange
        when(athleteRepository.findById(1L)).thenReturn(Optional.of(newAthlete));

        // Act
        Athlete result = athleteService.getAthleteById(1L);

        // Assert
        assertNotNull(result);
        assertEquals("usuario1", result.getUsername());
    }

    @Test
    @DisplayName("Debe lanzar ResourceNotFoundException si el deportista no existe")
    void getAthleteById_NotFound() {
        // Arrange
        when(athleteRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> athleteService.getAthleteById(99L));
    }

    @Test
    @DisplayName("Debe lanzar ResourceNotFoundException si el deportista está eliminado")
    void getAthleteById_Deleted() {
        // Arrange
        newAthlete.setDeletedAt(Instant.now());
        when(athleteRepository.findById(1L)).thenReturn(Optional.of(newAthlete));

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> athleteService.getAthleteById(1L));
    }

    @Test
    @DisplayName("Debe actualizar los campos enviados en la solicitud")
    void updateAthlete_Success() {
        // Arrange
        UpdateAthleteRequest request = new UpdateAthleteRequest(
            "Pedro", null, LocalDate.of(2000, 1, 15), null);
        when(athleteRepository.findById(1L)).thenReturn(Optional.of(newAthlete));
        when(athleteRepository.save(any(Athlete.class))).thenReturn(newAthlete);

        // Act
        Athlete result = athleteService.updateAthlete(1L, request);

        // Assert
        assertEquals("Pedro", result.getFirstName());
        assertEquals("Perez", result.getLastName());
        assertEquals(LocalDate.of(2000, 1, 15), result.getBirthDate());
        assertEquals("http://foto.png", result.getPhotoUrl());
        verify(athleteRepository, times(1)).save(any(Athlete.class));
    }

    @Test
    @DisplayName("Debe lanzar ResourceNotFoundException al actualizar un deportista inexistente")
    void updateAthlete_NotFound() {
        // Arrange
        UpdateAthleteRequest request = new UpdateAthleteRequest(null, null, null, null);
        when(athleteRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> athleteService.updateAthlete(99L, request));
        verify(athleteRepository, never()).save(any(Athlete.class));
    }

    @Test
    @DisplayName("Debe eliminar lógicamente un deportista (soft delete)")
    void softDeleteAthlete_Success() {
        // Arrange
        when(athleteRepository.findById(1L)).thenReturn(Optional.of(newAthlete));
        when(athleteRepository.save(any(Athlete.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        athleteService.softDeleteAthlete(1L);

        // Assert
        assertNotNull(newAthlete.getDeletedAt());
        verify(athleteRepository, times(1)).save(newAthlete);
        verify(athleteRepository, never()).delete(any(Athlete.class));
    }

    @Test
    @DisplayName("Debe lanzar ResourceNotFoundException al eliminar un deportista inexistente")
    void softDeleteAthlete_NotFound() {
        // Arrange
        when(athleteRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> athleteService.softDeleteAthlete(99L));
        verify(athleteRepository, never()).save(any(Athlete.class));
    }
}
