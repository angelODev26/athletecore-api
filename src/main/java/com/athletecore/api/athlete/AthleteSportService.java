package com.athletecore.api.athlete;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.athletecore.api.athlete.dto.AssignSportsRequest;
import com.athletecore.api.common.exception.ResourceNotFoundException;

/**
 * Servicio para gestión de deportes de atletas.
 */
@Service
public class AthleteSportService {

    private final AthleteRepository athleteRepository;
    private final SportRepository sportRepository;

    public AthleteSportService(AthleteRepository athleteRepository, SportRepository sportRepository) {
        this.athleteRepository = athleteRepository;
        this.sportRepository = sportRepository;
    }

    /**
     * Asigna deportes a un atleta.
     * @param athleteId ID del atleta
     * @param request Solicitudes de deportes
     * @return Athlete con deportes asignados
     * @throws ResourceNotFoundException si atleta o deportes no existen
     */
    @Transactional
    public Athlete assignSportsToAthlete(Long athleteId, AssignSportsRequest request) {
        Athlete athlete = getAthlete(athleteId);

        // Verificar que los deportes existen y están activos
        Set<Sport> sports = new HashSet<>();
        for (Long sportId : request.sportIds()) {
            Sport sport = sportRepository.findById(sportId)
                .orElseThrow(() -> new ResourceNotFoundException("Sport", "id", sportId));

            sports.add(sport);
        }

        athlete.setSports(sports);
        return athleteRepository.save(athlete);
    }

    /**
     * Obtiene los deportes de un atleta.
     * @param athleteId ID del atleta
     * @return Lista de deportes del atleta
     * @throws ResourceNotFoundException si el atleta no existe
     */
    @Transactional(readOnly = true)
    public List<Sport> getAthleteSports(Long athleteId) {
        Athlete athlete = getAthlete(athleteId);
        return athlete.getSports() == null ? List.of() : athlete.getSports().stream()
            .filter(sport -> !sport.isDeleted())
            .collect(Collectors.toList());
    }

    /**
     * Obtiene IDs de deportes de un atleta.
     * @param athleteId ID del atleta
     * @return Lista de IDs de deportes
     */
    @Transactional(readOnly = true)
    public List<Long> getAthleteSportIds(Long athleteId) {
        Athlete athlete = getAthlete(athleteId);
        return athlete.getSports() == null ? List.of() : athlete.getSports().stream()
            .filter(sport -> !sport.isDeleted())
            .map(Sport::getId)
            .collect(Collectors.toList());
    }

    /**
     * Obtiene un atleta y lanza error si no existe.
     */
    private Athlete getAthlete(Long athleteId) {
        return athleteRepository.findById(athleteId)
            .filter(athlete -> !athlete.isDeleted())
            .orElseThrow(() -> new ResourceNotFoundException("Athlete", "id", athleteId));
    }
}
