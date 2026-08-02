package com.athletecore.api.athlete;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.athletecore.api.athlete.dto.SportRequest;
import com.athletecore.api.common.exception.DuplicateResourceException;

/**
 * Servicio para gestión del catálogo de deportes.
 */
@Service
public class SportService {

    private final SportRepository sportRepository;

    public SportService(SportRepository sportRepository) {
        this.sportRepository = sportRepository;
    }

    /**
     * Obtiene todos los deportes activos.
     * @return Lista de deportes activos
     */
    @Transactional(readOnly = true)
    public List<Sport> getAllSports() {
        return sportRepository.findAllByDeletedAtIsNull();
    }

    /**
     * Crea un nuevo deporte.
     * @param request Datos del deporte
     * @return Sport creado
     * @throws DuplicateResourceException si ya existe un deporte con ese nombre
     */
    @Transactional
    public Sport createSport(SportRequest request) {
        if (sportRepository.existsByName(request.name())) {
            throw new DuplicateResourceException("Sport", "name");
        }

        Sport sport = Sport.builder()
            .name(request.name())
            .description(request.description())
            .build();

        return sportRepository.save(sport);
    }
}
