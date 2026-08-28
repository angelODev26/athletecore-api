package com.athletecore.api.checkup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyShort;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.athletecore.api.checkup.dto.NationalReferenceTimeRequest;
import com.athletecore.api.common.exception.DuplicateResourceException;
import com.athletecore.api.common.exception.ResourceNotFoundException;
import com.athletecore.api.common.exception.ValidationException;

/**
 * Tests unitarios de NationalReferenceTimeService: CRUD administrativo de la
 * tabla nacional de referencia con validación de posición (1-3) y unicidad
 * entre filas activas.
 */
@ExtendWith(MockitoExtension.class)
class NationalReferenceTimeServiceTest {

    @Mock
    private NationalReferenceTimeRepository nationalReferenceTimeRepository;

    @InjectMocks
    private NationalReferenceTimeService service;

    private NationalReferenceTime reference;
    private NationalReferenceTimeRequest validRequest;

    @BeforeEach
    void setUp() {
        reference = NationalReferenceTime.builder()
                .id(1L)
                .style("LIBRE")
                .distance(100)
                .category("MAYOR")
                .position((short) 1)
                .timeSeconds(new BigDecimal("62.000"))
                .build();
        validRequest = new NationalReferenceTimeRequest("LIBRE", 100, "MAYOR", (short) 1,
                new BigDecimal("62.000"));
    }

    @Test
    @DisplayName("Debe crear y persistir una referencia cuando los datos son válidos y no está duplicada")
    void createReference_devuelveReferencia_cuandoDatosValidosYNoDuplicada() {
        when(nationalReferenceTimeRepository.existsActiveByStyleDistanceCategoryAndPosition(
                "LIBRE", 100, "MAYOR", (short) 1)).thenReturn(false);
        when(nationalReferenceTimeRepository.save(any(NationalReferenceTime.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        NationalReferenceTime result = service.createReference(validRequest);

        assertNotNull(result);
        assertEquals("LIBRE", result.getStyle());
        assertEquals(100, result.getDistance());
        assertEquals("MAYOR", result.getCategory());
        assertEquals((short) 1, result.getPosition());
        assertEquals(new BigDecimal("62.000"), result.getTimeSeconds());
        verify(nationalReferenceTimeRepository, times(1)).save(any(NationalReferenceTime.class));
    }

    @Test
    @DisplayName("Debe lanzar ValidationException cuando la posición no está entre 1 y 3")
    void createReference_lanza400_cuandoPositionInvalida() {
        NationalReferenceTimeRequest request = new NationalReferenceTimeRequest(
                "LIBRE", 100, "MAYOR", (short) 4, new BigDecimal("62.000"));

        ValidationException ex = assertThrows(ValidationException.class,
                () -> service.createReference(request));

        assertEquals("La posición debe ser 1, 2 o 3", ex.getMessage());
        verify(nationalReferenceTimeRepository, never())
                .existsActiveByStyleDistanceCategoryAndPosition(anyString(), anyInt(), anyString(), anyShort());
        verify(nationalReferenceTimeRepository, never()).save(any(NationalReferenceTime.class));
    }

    @Test
    @DisplayName("Debe lanzar DuplicateResourceException si ya existe referencia para (estilo, distancia, categoría, posición)")
    void createReference_lanza409_cuandoYaExisteReferenciaParaStyleDistanceCategoryPosition() {
        when(nationalReferenceTimeRepository.existsActiveByStyleDistanceCategoryAndPosition(
                "LIBRE", 100, "MAYOR", (short) 1)).thenReturn(true);

        DuplicateResourceException ex = assertThrows(DuplicateResourceException.class,
                () -> service.createReference(validRequest));

        assertEquals("NationalReferenceTime with style/distance/category/position already exists", ex.getMessage());
        verify(nationalReferenceTimeRepository, never()).save(any(NationalReferenceTime.class));
    }

    @Test
    @DisplayName("Debe devolver todas las referencias activas")
    void getAllReferences_devuelveListaCompleta_cuandoHayReferencias() {
        NationalReferenceTime second = NationalReferenceTime.builder()
                .id(2L).style("ESPALDA").distance(100).category("MAYOR")
                .position((short) 1).timeSeconds(new BigDecimal("63.000")).build();
        when(nationalReferenceTimeRepository.findAllByDeletedAtIsNull()).thenReturn(List.of(reference, second));

        List<NationalReferenceTime> result = service.getAllReferences();

        assertEquals(2, result.size());
        verify(nationalReferenceTimeRepository, times(1)).findAllByDeletedAtIsNull();
    }

    @Test
    @DisplayName("Debe devolver la referencia existente al consultar por ID")
    void getReferenceById_devuelveReferencia_cuandoExiste() {
        when(nationalReferenceTimeRepository.findById(1L)).thenReturn(Optional.of(reference));

        NationalReferenceTime result = service.getReferenceById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("LIBRE", result.getStyle());
    }

    @Test
    @DisplayName("Debe lanzar ResourceNotFoundException al consultar una referencia inexistente")
    void getReferenceById_lanza404_cuandoNoExiste() {
        when(nationalReferenceTimeRepository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> service.getReferenceById(99L));

        assertEquals("NationalReferenceTime not found with id: '99'", ex.getMessage());
    }

    @Test
    @DisplayName("Debe actualizar los campos sin validar unicidad cuando el tuplet no cambia")
    void updateReference_actualizaCampos_cuandoDatosValidosYTupletSinCambio() {
        NationalReferenceTimeRequest request = new NationalReferenceTimeRequest(
                "LIBRE", 100, "MAYOR", (short) 1, new BigDecimal("61.500"));
        when(nationalReferenceTimeRepository.findById(1L)).thenReturn(Optional.of(reference));
        when(nationalReferenceTimeRepository.save(reference)).thenReturn(reference);

        NationalReferenceTime result = service.updateReference(1L, request);

        assertEquals(new BigDecimal("61.500"), result.getTimeSeconds());
        assertEquals("LIBRE", result.getStyle());
        verify(nationalReferenceTimeRepository, never())
                .existsActiveByStyleDistanceCategoryAndPosition(anyString(), anyInt(), anyString(), anyShort());
        verify(nationalReferenceTimeRepository, times(1)).save(reference);
    }

    @Test
    @DisplayName("Debe lanzar DuplicateResourceException si el tuplet cambia y ya existe otra referencia")
    void updateReference_lanza409_cuandoTupletCambiaYYaExisteOtraReferencia() {
        NationalReferenceTimeRequest request = new NationalReferenceTimeRequest(
                "LIBRE", 100, "MAYOR", (short) 2, new BigDecimal("63.500"));
        when(nationalReferenceTimeRepository.findById(1L)).thenReturn(Optional.of(reference));
        when(nationalReferenceTimeRepository.existsActiveByStyleDistanceCategoryAndPosition(
                "LIBRE", 100, "MAYOR", (short) 2)).thenReturn(true);

        DuplicateResourceException ex = assertThrows(DuplicateResourceException.class,
                () -> service.updateReference(1L, request));

        assertEquals("NationalReferenceTime with style/distance/category/position already exists", ex.getMessage());
        verify(nationalReferenceTimeRepository, never()).save(any(NationalReferenceTime.class));
    }

    @Test
    @DisplayName("Debe lanzar ValidationException al actualizar con posición inválida")
    void updateReference_lanza400_cuandoPositionInvalida() {
        NationalReferenceTimeRequest request = new NationalReferenceTimeRequest(
                "LIBRE", 100, "MAYOR", (short) 0, new BigDecimal("62.000"));

        ValidationException ex = assertThrows(ValidationException.class,
                () -> service.updateReference(1L, request));

        assertEquals("La posición debe ser 1, 2 o 3", ex.getMessage());
        verify(nationalReferenceTimeRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("Debe lanzar ResourceNotFoundException al actualizar una referencia inexistente")
    void updateReference_lanza404_cuandoReferenciaNoExiste() {
        when(nationalReferenceTimeRepository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> service.updateReference(99L, validRequest));

        assertEquals("NationalReferenceTime not found with id: '99'", ex.getMessage());
    }

    @Test
    @DisplayName("Debe marcar deleted_at y persistir al eliminar una referencia existente")
    void softDeleteReference_marcaDeletedAt_cuandoReferenciaExiste() {
        when(nationalReferenceTimeRepository.findById(1L)).thenReturn(Optional.of(reference));
        when(nationalReferenceTimeRepository.save(reference)).thenReturn(reference);

        service.softDeleteReference(1L);

        assertNotNull(reference.getDeletedAt());
        verify(nationalReferenceTimeRepository, times(1)).save(reference);
    }

    @Test
    @DisplayName("Debe lanzar ResourceNotFoundException al eliminar una referencia inexistente")
    void softDeleteReference_lanza404_cuandoReferenciaNoExiste() {
        when(nationalReferenceTimeRepository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> service.softDeleteReference(99L));

        assertEquals("NationalReferenceTime not found with id: '99'", ex.getMessage());
        verify(nationalReferenceTimeRepository, never()).save(any(NationalReferenceTime.class));
    }
}
