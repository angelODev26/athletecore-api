package com.athletecore.api.checkup;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.athletecore.api.athlete.Athlete;
import com.athletecore.api.athlete.AthleteRepository;
import com.athletecore.api.checkup.dto.AddCheckupTimeRequest;
import com.athletecore.api.checkup.dto.CreateCheckupRequest;
import com.athletecore.api.common.exception.DuplicateResourceException;
import com.athletecore.api.common.exception.ResourceNotFoundException;

/**
 * Servicio de chequeos mensuales de rendimiento.
 * Responsabilidad única: registro, lectura y soft delete de chequeos y de sus
 * tiempos de prueba. Las unicidades (athlete, year, month, category) y
 * (checkup, style, distance) se validan contra las filas activas y lanzan 409.
 */
@Service
public class CheckupService {

    private final CheckupRepository checkupRepository;
    private final CheckupTimeRepository checkupTimeRepository;
    private final AthleteRepository athleteRepository;

    public CheckupService(CheckupRepository checkupRepository,
                          CheckupTimeRepository checkupTimeRepository,
                          AthleteRepository athleteRepository) {
        this.checkupRepository = checkupRepository;
        this.checkupTimeRepository = checkupTimeRepository;
        this.athleteRepository = athleteRepository;
    }

    /**
     * Crea un chequeo mensual para un deportista.
     * El athleteId del path es la fuente de verdad; el campo athleteId del
     * request se conserva por contrato del DTO pero no se usa para la FK.
     *
     * @param athleteId ID del deportista (path)
     * @param request   Datos del chequeo (year, month, category, notes)
     * @return Checkup persistido
     * @throws ResourceNotFoundException si el deportista no existe (404)
     * @throws DuplicateResourceException si ya existe un chequeo activo para
     *         (athlete, year, month, category) (409)
     */
    @Transactional
    public Checkup createCheckup(Long athleteId, CreateCheckupRequest request) {
        Athlete athlete = athleteRepository.findById(athleteId)
                .orElseThrow(() -> new ResourceNotFoundException("Athlete", "id", athleteId));

        boolean duplicate = checkupRepository.existsActiveByAthleteIdAndYearMonthAndCategory(
                athleteId, request.year(), request.month(), request.category());
        if (duplicate) {
            throw new DuplicateResourceException("Checkup", "athleteId/year/month/category");
        }

        Checkup checkup = Checkup.builder()
                .athlete(athlete)
                .year(request.year())
                .month(request.month())
                .category(request.category())
                .notes(request.notes())
                .build();
        return checkupRepository.save(checkup);
    }

    /**
     * Obtiene un chequeo activo por ID.
     *
     * @param id ID del chequeo
     * @return Checkup activo
     * @throws ResourceNotFoundException si no existe o está soft-deleted (404)
     */
    @Transactional(readOnly = true)
    public Checkup getCheckupById(Long id) {
        return checkupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Checkup", "id", id));
    }

    /**
     * Lista los chequeos activos de un deportista. Si se proveen year y month
     * juntos se filtran; si falta alguno, se devuelven todos los activos del
     * deportista (los filtros parciales no se aplican para no ocultar datos).
     *
     * @param athleteId ID del deportista
     * @param year      Año de filtro (opcional)
     * @param month     Mes de filtro (opcional)
     * @return Lista de chequeos activos, del más reciente al más antiguo
     * @throws ResourceNotFoundException si el deportista no existe (404)
     */
    @Transactional(readOnly = true)
    public List<Checkup> getCheckupsByAthlete(Long athleteId, Integer year, Integer month) {
        athleteRepository.findById(athleteId)
                .orElseThrow(() -> new ResourceNotFoundException("Athlete", "id", athleteId));

        if (year != null && month != null) {
            return checkupRepository.findActiveByAthleteIdAndYearMonth(athleteId, year, month);
        }
        return checkupRepository.findActiveByAthleteId(athleteId);
    }

    /**
     * Obtiene los tiempos de prueba activos de un chequeo, ordenados por estilo
     * y distancia. La existencia del chequeo la valida el llamador.
     *
     * @param checkupId ID del chequeo
     * @return Lista de tiempos activos del chequeo
     */
    @Transactional(readOnly = true)
    public List<CheckupTime> getTimesByCheckup(Long checkupId) {
        return checkupTimeRepository.findActiveByCheckupId(checkupId);
    }

    /**
     * Agrega un tiempo de prueba a un chequeo existente.
     *
     * @param checkupId ID del chequeo padre
     * @param request   Datos del tiempo (style, distance, timeSeconds)
     * @return CheckupTime persistido
     * @throws ResourceNotFoundException si el chequeo no existe (404)
     * @throws DuplicateResourceException si ya existe un tiempo activo para
     *         (checkup, style, distance) (409)
     */
    @Transactional
    public CheckupTime addCheckupTime(Long checkupId, AddCheckupTimeRequest request) {
        Checkup checkup = getCheckupById(checkupId);

        boolean duplicate = checkupTimeRepository.existsActiveByCheckupIdAndStyleAndDistance(
                checkupId, request.style(), request.distance());
        if (duplicate) {
            throw new DuplicateResourceException("CheckupTime", "checkupId/style/distance");
        }

        CheckupTime checkupTime = CheckupTime.builder()
                .checkup(checkup)
                .style(request.style())
                .distance(request.distance())
                .timeSeconds(request.timeSeconds())
                .build();
        return checkupTimeRepository.save(checkupTime);
    }

    /**
     * Elimina lógicamente un chequeo (soft delete) y propaga la baja a sus
     * tiempos de prueba con la misma marca temporal, de forma atómica en la
     * misma transacción.
     *
     * @param id ID del chequeo
     * @throws ResourceNotFoundException si el chequeo no existe (404)
     */
    @Transactional
    public void softDeleteCheckup(Long id) {
        Checkup checkup = getCheckupById(id);
        Instant deletedAt = Instant.now();

        List<CheckupTime> times = checkupTimeRepository.findActiveByCheckupId(id);
        if (!times.isEmpty()) {
            times.forEach(time -> time.setDeletedAt(deletedAt));
            checkupTimeRepository.saveAll(times);
        }

        checkup.setDeletedAt(deletedAt);
        checkupRepository.save(checkup);
    }
}
