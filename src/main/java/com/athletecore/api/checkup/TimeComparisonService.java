package com.athletecore.api.checkup;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.athletecore.api.checkup.dto.TimeComparisonResponse;
import com.athletecore.api.common.exception.DuplicateResourceException;
import com.athletecore.api.common.exception.ResourceNotFoundException;

/**
 * Servicio de comparación de tiempos de prueba contra la tabla nacional de
 * referencia. Responsabilidad única: dado un tiempo de prueba (style, distance,
 * category), carga el triple activo (1°, 2°, 3°) y calcula diferencias absolutas
 * y relativas por posición. No usa Clock: la comparación es aritmética pura y
 * determinística.
 */
@Service
public class TimeComparisonService {

    private final CheckupRepository checkupRepository;
    private final CheckupTimeRepository checkupTimeRepository;
    private final NationalReferenceTimeRepository nationalReferenceTimeRepository;

    public TimeComparisonService(CheckupRepository checkupRepository,
                                 CheckupTimeRepository checkupTimeRepository,
                                 NationalReferenceTimeRepository nationalReferenceTimeRepository) {
        this.checkupRepository = checkupRepository;
        this.checkupTimeRepository = checkupTimeRepository;
        this.nationalReferenceTimeRepository = nationalReferenceTimeRepository;
    }

    /**
     * Compara todos los tiempos activos de un chequeo contra la tabla nacional.
     *
     * @param checkupId ID del chequeo
     * @return Lista de comparaciones, una por tiempo de prueba registrado
     * @throws ResourceNotFoundException si el chequeo no existe (404)
     * @throws DuplicateResourceException si algún (style, distance) del chequeo
     *         no tiene triple completo de referencia (409)
     */
    @Transactional(readOnly = true)
    public List<TimeComparisonResponse> compareCheckup(Long checkupId) {
        Checkup checkup = checkupRepository.findById(checkupId)
                .orElseThrow(() -> new ResourceNotFoundException("Checkup", "id", checkupId));
        List<CheckupTime> times = checkupTimeRepository.findActiveByCheckupId(checkupId);
        return times.stream()
                .map(time -> compareTrial(time, checkup.getCategory()))
                .toList();
    }

    /**
     * Compara un tiempo de prueba individual contra el triple de referencia.
     *
     * @param trial    Tiempo de prueba registrado
     * @param category Categoría de competición del chequeo
     * @return Comparación con deltas absolutos y relativos por posición
     */
    public TimeComparisonResponse compareTrial(CheckupTime trial, String category) {
        List<NationalReferenceTime> references = loadReferenceTriple(
                trial.getStyle(), trial.getDistance(), category);
        return TimeComparisonResponse.fromCheckupTimeAndReferences(trial, category, references);
    }

    /**
     * Carga el triple activo (1°, 2°, 3°) de referencias nacionales para un
     * (style, distance, category). La comparación requiere el triple completo:
     * si falta alguna posición se rechaza con 409 (convención del repo: usar
     * DuplicateResourceException como estado de conflicto de datos).
     *
     * @param style    Estilo de nado
     * @param distance Distancia en metros
     * @param category Categoría de competición
     * @return Triple de referencias ordenado por posición ascendente
     * @throws DuplicateResourceException si no existe el triple completo (409)
     */
    public List<NationalReferenceTime> loadReferenceTriple(String style, Integer distance, String category) {
        List<NationalReferenceTime> references = nationalReferenceTimeRepository
                .findActiveTripleByStyleDistanceCategory(style, distance, category);
        if (references.size() < 3) {
            throw new DuplicateResourceException(
                    "No existe el triple completo de tiempos de referencia nacional (1°, 2° y 3° puesto) para "
                            + style + " " + distance + "m en categoría " + category);
        }
        return references;
    }
}
