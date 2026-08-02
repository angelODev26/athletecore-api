package com.athletecore.api.athlete;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.athletecore.api.athlete.dto.CreateAthleteRequest;
import com.athletecore.api.athlete.dto.UpdateAthleteRequest;
import com.athletecore.api.common.exception.DuplicateResourceException;
import com.athletecore.api.common.exception.ResourceNotFoundException;

/**
 * Servicio para registro y gestión básica de deportistas.
 * Maneja creación, actualización y búsqueda de atletas.
 */
@Service
public class AthleteRegistrationService {

    private final AthleteRepository athleteRepository;

    public AthleteRegistrationService(AthleteRepository athleteRepository) {
        this.athleteRepository = athleteRepository;
    }

    /**
     * Registra un nuevo deportista.
     * @param request Datos del deportista a registrar
     * @return Athlete creado
     * @throws DuplicateResourceException si username o email ya existen
     */
    @Transactional
    public Athlete registerAthlete(CreateAthleteRequest request) {
        // Verificar duplicación de username
        if (athleteRepository.existsByUsername(request.username())) {
            throw new DuplicateResourceException("Athlete", "username");
        }

        // Verificar duplicación de email
        if (athleteRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Athlete", "email");
        }

        // Crear nuevo atleta
        Athlete athlete = Athlete.builder()
            .username(request.username())
            .email(request.email())
            .firstName(request.firstName())
            .lastName(request.lastName())
            .photoUrl(request.photoUrl())
            .build();

        return athleteRepository.save(athlete);
    }

    /**
     * Obtiene los deportistas activos de forma paginada.
     * @param pageable Parámetros de paginación (page, size, sort)
     * @return Página de atletas activos
     */
    @Transactional(readOnly = true)
    public Page<Athlete> getAllAthletes(Pageable pageable) {
        return athleteRepository.findAllByDeletedAtIsNull(pageable);
    }

    /**
     * Obtiene un deportista por ID.
     * @param id ID del atleta
     * @return Athlete encontrado
     * @throws ResourceNotFoundException si el atleta no existe o está eliminado
     */
    @Transactional(readOnly = true)
    public Athlete getAthleteById(Long id) {
        return athleteRepository.findById(id)
            .filter(athlete -> !athlete.isDeleted())
            .orElseThrow(() -> new ResourceNotFoundException("Athlete", "id", id));
    }

    /**
     * Actualiza un deportista existente.
     * @param id ID del atleta
     * @param request Datos de actualización
     * @return Athlete actualizado
     * @throws ResourceNotFoundException si el atleta no existe
     */
    @Transactional
    public Athlete updateAthlete(Long id, UpdateAthleteRequest request) {
        Athlete athlete = getAthleteById(id);

        if (request.firstName() != null) {
            athlete.setFirstName(request.firstName());
        }
        if (request.lastName() != null) {
            athlete.setLastName(request.lastName());
        }
        if (request.birthDate() != null) {
            athlete.setBirthDate(request.birthDate());
        }
        if (request.photoUrl() != null) {
            athlete.setPhotoUrl(request.photoUrl());
        }

        return athleteRepository.save(athlete);
    }

    /**
     * Elimina lógicamente un deportista (soft delete).
     * Se marca deletedAt directamente en lugar de llamar delete() para evitar
     * que Hibernate procese cascadas de asociaciones al hacer flush.
     * @param id ID del atleta
     * @throws ResourceNotFoundException si el atleta no existe
     */
    @Transactional
    public void softDeleteAthlete(Long id) {
        Athlete athlete = getAthleteById(id);
        athlete.setDeletedAt(Instant.now());
        athleteRepository.save(athlete);
    }
}
