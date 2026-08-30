package com.athletecore.api.checkup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.athletecore.api.athlete.AthleteRepository;
import com.athletecore.api.common.exception.DuplicateResourceException;
import com.athletecore.api.common.exception.ResourceNotFoundException;

/**
 * Servicio de proyección de medallería por deportista.
 * Responsabilidad única: recorrer los tiempos de prueba activos de un atleta,
 * obtener el triple de referencia por (style, distance, category) vía
 * TimeComparisonService, clasificar con ClassificationService y construir
 * records de dominio MedalProjection. Es una operación de solo lectura
 * ({@code @Transactional(readOnly = true)}): no persiste nada (decisión D5).
 */
@Service
public class MedalProjectionService {

    private static final short BRONZE_POSITION = 3;

    private final AthleteRepository athleteRepository;
    private final CheckupRepository checkupRepository;
    private final CheckupTimeRepository checkupTimeRepository;
    private final TimeComparisonService timeComparisonService;
    private final ClassificationService classificationService;

    public MedalProjectionService(AthleteRepository athleteRepository,
                                  CheckupRepository checkupRepository,
                                  CheckupTimeRepository checkupTimeRepository,
                                  TimeComparisonService timeComparisonService,
                                  ClassificationService classificationService) {
        this.athleteRepository = athleteRepository;
        this.checkupRepository = checkupRepository;
        this.checkupTimeRepository = checkupTimeRepository;
        this.timeComparisonService = timeComparisonService;
        this.classificationService = classificationService;
    }

    /**
     * Calcula la proyección de medallería de un deportista: una entrada por
     * (style, distance) evaluado, con su clasificación y diferencia contra el
     * bronce. Si el atleta no tiene chequeos activos devuelve lista vacía.
     * Los triples de referencia se cachean por (style, distance, category)
     * dentro de la misma consulta para minimizar round-trips.
     *
     * @param athleteId ID del deportista
     * @return Lista de proyecciones calculadas (no persistidas)
     * @throws ResourceNotFoundException si el deportista no existe (404)
     * @throws DuplicateResourceException si algún (style, distance) no tiene
     *         triple completo de referencia (409)
     */
    @Transactional(readOnly = true)
    public List<MedalProjection> getProjectionsForAthlete(Long athleteId) {
        athleteRepository.findById(athleteId)
                .orElseThrow(() -> new ResourceNotFoundException("Athlete", "id", athleteId));

        List<Checkup> checkups = checkupRepository.findActiveByAthleteId(athleteId);
        if (checkups.isEmpty()) {
            return List.of();
        }

        Map<String, List<NationalReferenceTime>> referenceCache = new HashMap<>();
        List<MedalProjection> projections = new ArrayList<>();
        for (Checkup checkup : checkups) {
            List<CheckupTime> times = checkupTimeRepository.findActiveByCheckupId(checkup.getId());
            for (CheckupTime time : times) {
                projections.add(buildProjection(checkup, time, referenceCache));
            }
        }
        return projections;
    }

    /**
     * Calcula las proyecciones de medallería de todos los deportistas con
     * chequeos activos en una sola pasada, evitando N+1 respecto a la versión
     * por atleta (consume el módulo reportes). Carga todos los chequeos y
     * tiempos activos en dos consultas y reutiliza la cache de triples de
     * referencia de forma global.
     *
     * @return Mapa de athleteId → lista de proyecciones (no persistidas)
     */
    @Transactional(readOnly = true)
    public Map<Long, List<MedalProjection>> getProjectionsForAllAthletes() {
        List<Checkup> checkups = checkupRepository.findAllActive();
        Map<Long, List<CheckupTime>> timesByCheckup = checkupTimeRepository.findAllActive().stream()
                .collect(Collectors.groupingBy(time -> time.getCheckup().getId()));

        Map<String, List<NationalReferenceTime>> referenceCache = new HashMap<>();
        Map<Long, List<MedalProjection>> projectionsByAthlete = new LinkedHashMap<>();

        for (Checkup checkup : checkups) {
            Long athleteId = checkup.getAthlete().getId();
            List<CheckupTime> times = timesByCheckup.getOrDefault(checkup.getId(), List.of());
            for (CheckupTime time : times) {
                MedalProjection projection = buildProjection(checkup, time, referenceCache);
                projectionsByAthlete.computeIfAbsent(athleteId, k -> new ArrayList<>()).add(projection);
            }
        }
        return projectionsByAthlete;
    }

    private MedalProjection buildProjection(Checkup checkup, CheckupTime time,
                                            Map<String, List<NationalReferenceTime>> referenceCache) {
        String cacheKey = time.getStyle() + "|" + time.getDistance() + "|" + checkup.getCategory();
        List<NationalReferenceTime> references = referenceCache.computeIfAbsent(cacheKey,
                key -> timeComparisonService.loadReferenceTriple(
                        time.getStyle(), time.getDistance(), checkup.getCategory()));

        NationalReferenceTime bronze = references.stream()
                .filter(reference -> reference.getPosition() == BRONZE_POSITION)
                .findFirst()
                .orElseThrow(() -> new DuplicateResourceException(
                        "No existe referencia nacional para el 3° puesto de " + cacheKey));

        Classification classification =
                classificationService.classify(time.getTimeSeconds(), bronze.getTimeSeconds());
        return new MedalProjection(
                time.getStyle(),
                time.getDistance(),
                checkup.getCategory(),
                classification,
                time.getTimeSeconds(),
                time.getTimeSeconds().subtract(bronze.getTimeSeconds()));
    }
}
