package com.athletecore.api.checkup;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.athletecore.api.checkup.dto.NationalReferenceTimeRequest;
import com.athletecore.api.common.exception.DuplicateResourceException;
import com.athletecore.api.common.exception.ResourceNotFoundException;
import com.athletecore.api.common.exception.ValidationException;

/**
 * Servicio de la tabla nacional de tiempos de referencia (CRUD administrativo).
 * Responsabilidad única: gestionar las filas (style, distance, category,
 * position) con validación de unicidad solo entre activas y posición 1-3.
 * La restricción de rol ADMIN se aplica en el controller (@PreAuthorize); aquí
 * solo se validan reglas de dominio.
 */
@Service
public class NationalReferenceTimeService {

    private final NationalReferenceTimeRepository nationalReferenceTimeRepository;

    public NationalReferenceTimeService(NationalReferenceTimeRepository nationalReferenceTimeRepository) {
        this.nationalReferenceTimeRepository = nationalReferenceTimeRepository;
    }

    /**
     * Crea una referencia nacional.
     *
     * @param request Datos de la referencia (style, distance, category, position, timeSeconds)
     * @return NationalReferenceTime persistida
     * @throws ValidationException si position no está entre 1 y 3 (defensa en
     *         profundidad; el DTO ya valida 400)
     * @throws DuplicateResourceException si ya existe activa para
     *         (style, distance, category, position) (409)
     */
    @Transactional
    public NationalReferenceTime createReference(NationalReferenceTimeRequest request) {
        validatePosition(request.position());

        if (nationalReferenceTimeRepository.existsActiveByStyleDistanceCategoryAndPosition(
                request.style(), request.distance(), request.category(), request.position())) {
            throw new DuplicateResourceException("NationalReferenceTime", "style/distance/category/position");
        }

        NationalReferenceTime reference = NationalReferenceTime.builder()
                .style(request.style())
                .distance(request.distance())
                .category(request.category())
                .position(request.position())
                .timeSeconds(request.timeSeconds())
                .build();
        return nationalReferenceTimeRepository.save(reference);
    }

    /**
     * Lista todas las referencias nacionales activas.
     *
     * @return Lista de referencias activas
     */
    @Transactional(readOnly = true)
    public List<NationalReferenceTime> getAllReferences() {
        return nationalReferenceTimeRepository.findAllByDeletedAtIsNull();
    }

    /**
     * Obtiene una referencia nacional activa por ID.
     *
     * @param id ID de la referencia
     * @return NationalReferenceTime activa
     * @throws ResourceNotFoundException si no existe o está soft-deleted (404)
     */
    @Transactional(readOnly = true)
    public NationalReferenceTime getReferenceById(Long id) {
        return nationalReferenceTimeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("NationalReferenceTime", "id", id));
    }

    /**
     * Actualiza una referencia nacional. Si el nuevo tuple (style, distance,
     * category, position) es idéntico al actual no se valida unicidad contra la
     * propia fila; si cambió, se valida contra las demás activas.
     *
     * @param id      ID de la referencia a actualizar
     * @param request Nuevos datos de la referencia
     * @return NationalReferenceTime actualizada
     * @throws ResourceNotFoundException si la referencia no existe (404)
     * @throws ValidationException si position no está entre 1 y 3
     * @throws DuplicateResourceException si el nuevo tuple colisiona con otra
     *         referencia activa (409)
     */
    @Transactional
    public NationalReferenceTime updateReference(Long id, NationalReferenceTimeRequest request) {
        validatePosition(request.position());
        NationalReferenceTime reference = getReferenceById(id);

        boolean tupleChanged = !reference.getStyle().equals(request.style())
                || !reference.getDistance().equals(request.distance())
                || !reference.getCategory().equals(request.category())
                || !reference.getPosition().equals(request.position());
        if (tupleChanged && nationalReferenceTimeRepository.existsActiveByStyleDistanceCategoryAndPosition(
                request.style(), request.distance(), request.category(), request.position())) {
            throw new DuplicateResourceException("NationalReferenceTime", "style/distance/category/position");
        }

        reference.setStyle(request.style());
        reference.setDistance(request.distance());
        reference.setCategory(request.category());
        reference.setPosition(request.position());
        reference.setTimeSeconds(request.timeSeconds());
        return nationalReferenceTimeRepository.save(reference);
    }

    /**
     * Elimina lógicamente una referencia nacional (soft delete).
     *
     * @param id ID de la referencia
     * @throws ResourceNotFoundException si la referencia no existe (404)
     */
    @Transactional
    public void softDeleteReference(Long id) {
        NationalReferenceTime reference = getReferenceById(id);
        reference.setDeletedAt(Instant.now());
        nationalReferenceTimeRepository.save(reference);
    }

    private void validatePosition(Short position) {
        if (position == null || position < 1 || position > 3) {
            throw new ValidationException("La posición debe ser 1, 2 o 3");
        }
    }
}
