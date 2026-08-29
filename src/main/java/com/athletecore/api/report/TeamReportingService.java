package com.athletecore.api.report;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.athletecore.api.athlete.Athlete;
import com.athletecore.api.athlete.AthleteRepository;
import com.athletecore.api.checkup.MedalProjection;
import com.athletecore.api.checkup.MedalProjectionService;
import com.athletecore.api.checkup.TimeFormatter;
import com.athletecore.api.report.dto.TeamReportResponse;
import com.athletecore.api.report.dto.TeamReportResponse.AthleteEntry;
import com.athletecore.api.report.dto.TeamReportResponse.TeamEntry;

/**
 * Servicio de reporte general de equipo/categoría. Agrega por
 * (style, distance, category) el rendimiento de los deportistas (mejor tiempo y
 * clasificación de proyección) para comparar el equipo, leyendo a través de los
 * servicios públicos de los módulos checkup y athlete (frontera de módulo,
 * decisión D5). Operación de solo lectura.
 */
@Service
public class TeamReportingService {

    private final AthleteRepository athleteRepository;
    private final MedalProjectionService medalProjectionService;

    public TeamReportingService(AthleteRepository athleteRepository,
                                MedalProjectionService medalProjectionService) {
        this.athleteRepository = athleteRepository;
        this.medalProjectionService = medalProjectionService;
    }

    /**
     * Ensambla el reporte general del equipo. Si se indica una categoría, solo
     * se agregan las proyecciones de esa categoría; en caso contrario se agregan
     * todas.
     *
     * @param category Categoría de competición (opcional)
     * @return Reporte general agregado por (style, distance, category)
     */
    @Transactional(readOnly = true)
    public TeamReportResponse assembleTeamReport(String category) {
        List<Athlete> athletes = athleteRepository.findAllByDeletedAtIsNull(Pageable.unpaged()).getContent();

        // Clave de agrupación "style|distance|category" -> (athleteId -> proyecciones)
        Map<String, Map<Long, List<MedalProjection>>> grouped = new LinkedHashMap<>();

        for (Athlete athlete : athletes) {
            List<MedalProjection> projections = medalProjectionService.getProjectionsForAthlete(athlete.getId());
            for (MedalProjection projection : projections) {
                if (category != null && !category.equals(projection.category())) {
                    continue;
                }
                String key = projection.style() + "|" + projection.distance() + "|" + projection.category();
                grouped.computeIfAbsent(key, k -> new LinkedHashMap<>())
                        .computeIfAbsent(athlete.getId(), k -> new ArrayList<>())
                        .add(projection);
            }
        }

        List<TeamEntry> entries = new ArrayList<>();
        grouped.forEach((key, byAthlete) -> {
            String[] parts = key.split("\\|");
            String style = parts[0];
            Integer distance = Integer.valueOf(parts[1]);
            String groupCategory = parts[2];

            List<AthleteEntry> athleteEntries = new ArrayList<>();
            byAthlete.forEach((athleteId, projections) -> {
                MedalProjection best = projections.stream()
                        .min(Comparator.comparing(MedalProjection::timeSeconds))
                        .orElseThrow();
                Athlete athlete = athleteRepository.findById(athleteId).orElseThrow();
                athleteEntries.add(new AthleteEntry(
                        athleteId,
                        athlete.getFullName(),
                        best.timeSeconds(),
                        TimeFormatter.toFormatted(best.timeSeconds()),
                        best.classification(),
                        best.diffVsBronzeSeconds(),
                        TimeFormatter.toSignedFormatted(best.diffVsBronzeSeconds())
                ));
            });
            athleteEntries.sort(Comparator.comparing(AthleteEntry::bestTimeSeconds)
                    .thenComparing(AthleteEntry::athleteFullName));

            entries.add(new TeamEntry(style, distance, groupCategory, athleteEntries.size(), athleteEntries));
        });
        entries.sort(Comparator.comparing(TeamEntry::style)
                .thenComparingInt(TeamEntry::distance));

        return new TeamReportResponse(category, entries);
    }
}