package com.athletecore.api.athlete;

import com.athletecore.api.athlete.dto.AthleteProfileRequest;
import com.athletecore.api.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AthleteProfileServiceTest {

    @Mock
    private AthleteRepository athleteRepository;

    @Mock
    private AthleteProfileRepository athleteProfileRepository;

    @InjectMocks
    private AthleteProfileService profileService;

    private Athlete athlete;
    private AthleteProfileRequest profileRequest;

    @BeforeEach
    void setUp() {
        athlete = Athlete.builder()
            .id(1L)
            .username("usuario1")
            .email("usuario1@example.com")
            .firstName("Juan")
            .lastName("Perez")
            .build();

        profileRequest = new AthleteProfileRequest(
            new BigDecimal("70.5"), new BigDecimal("175.0"),
            new BigDecimal("180.0"), "Nota de prueba");
    }

    @Test
    @DisplayName("Debe crear un perfil nuevo cuando el atleta no tiene uno")
    void upsertProfile_Create() {
        // Arrange
        when(athleteRepository.findById(1L)).thenReturn(Optional.of(athlete));
        when(athleteProfileRepository.findByAthleteId(1L)).thenReturn(Optional.empty());
        when(athleteProfileRepository.save(any(AthleteProfile.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        AthleteProfile result = profileService.upsertProfile(1L, profileRequest);

        // Assert
        assertNotNull(result);
        assertEquals(athlete, result.getAthlete());
        assertEquals(new BigDecimal("70.5"), result.getWeightKgs());
        assertEquals(new BigDecimal("175.0"), result.getHeightCm());
        assertEquals(new BigDecimal("180.0"), result.getArmSpanCm());
        assertEquals("Nota de prueba", result.getNotes());
        verify(athleteProfileRepository, times(1)).save(any(AthleteProfile.class));
    }

    @Test
    @DisplayName("Debe actualizar el perfil existente del atleta")
    void upsertProfile_Update() {
        // Arrange
        AthleteProfile existing = AthleteProfile.builder()
            .id(5L)
            .athlete(athlete)
            .weightKgs(new BigDecimal("80.0"))
            .build();
        when(athleteRepository.findById(1L)).thenReturn(Optional.of(athlete));
        when(athleteProfileRepository.findByAthleteId(1L)).thenReturn(Optional.of(existing));
        when(athleteProfileRepository.save(existing)).thenReturn(existing);

        // Act
        AthleteProfile result = profileService.upsertProfile(1L, profileRequest);

        // Assert
        assertEquals(5L, result.getId());
        assertEquals(new BigDecimal("70.5"), result.getWeightKgs());
        assertEquals(new BigDecimal("175.0"), result.getHeightCm());
        assertEquals(new BigDecimal("180.0"), result.getArmSpanCm());
        assertEquals("Nota de prueba", result.getNotes());
        verify(athleteProfileRepository, times(1)).save(existing);
    }

    @Test
    @DisplayName("Debe lanzar ResourceNotFoundException si el atleta no existe")
    void upsertProfile_AthleteNotFound() {
        // Arrange
        when(athleteRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class,
            () -> profileService.upsertProfile(99L, profileRequest));
        verify(athleteProfileRepository, never()).save(any(AthleteProfile.class));
    }

    @Test
    @DisplayName("Debe lanzar ResourceNotFoundException si el atleta está eliminado")
    void upsertProfile_AthleteDeleted() {
        // Arrange
        athlete.setDeletedAt(Instant.now());
        when(athleteRepository.findById(1L)).thenReturn(Optional.of(athlete));

        // Act & Assert
        assertThrows(ResourceNotFoundException.class,
            () -> profileService.upsertProfile(1L, profileRequest));
    }

    @Test
    @DisplayName("Debe obtener el perfil del atleta")
    void getProfile_Success() {
        // Arrange
        AthleteProfile profile = AthleteProfile.builder()
            .id(5L)
            .athlete(athlete)
            .weightKgs(new BigDecimal("70.5"))
            .build();
        when(athleteRepository.findById(1L)).thenReturn(Optional.of(athlete));
        when(athleteProfileRepository.findByAthleteId(1L)).thenReturn(Optional.of(profile));

        // Act
        AthleteProfile result = profileService.getProfile(1L);

        // Assert
        assertNotNull(result);
        assertEquals(5L, result.getId());
        assertEquals(new BigDecimal("70.5"), result.getWeightKgs());
    }

    @Test
    @DisplayName("Debe lanzar ResourceNotFoundException si el atleta no existe al obtener perfil")
    void getProfile_AthleteNotFound() {
        // Arrange
        when(athleteRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> profileService.getProfile(99L));
        verify(athleteProfileRepository, never()).findByAthleteId(anyLong());
    }

    @Test
    @DisplayName("Debe lanzar ResourceNotFoundException si el atleta no tiene perfil")
    void getProfile_ProfileNotFound() {
        // Arrange
        when(athleteRepository.findById(1L)).thenReturn(Optional.of(athlete));
        when(athleteProfileRepository.findByAthleteId(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> profileService.getProfile(1L));
    }
}
