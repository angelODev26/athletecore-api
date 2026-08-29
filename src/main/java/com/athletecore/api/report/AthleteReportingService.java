package com.athletecore.api.report;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.athletecore.api.athlete.Athlete;
import com.athletecore.api.athlete.AthleteRepository;
import com.athletecore.api.checkup.Checkup;
import com.athletecore.api.checkup.CheckupService;
import com.athletecore.api.checkup.CheckupTime;
import com.athletecore.api.checkup.MedalProjectionService;
import com.athletecore.api.checkup.TimeFormatter;
import com.athletecore.api.checkup.dto.MedalProjectionResponse;
import com.athletecore.api.common.exception.ResourceNotFoundException;
import com.athletecore.api.report.dto.IndividualReportResponse;
import com.athletecore.api.report.dto.IndividualReportResponse.AttendanceSummary;
import com.athletecore.api.report.dto.IndividualReportResponse.TimeEvolutionEntry;
import com.athletecore.api.training.Attendance;
import com.athletecore.api.training.AttendanceService;
import com.athletecore.api.training.AttendanceStatus;

/**
 * Servicio de reporte individual por deportista. Ensambla, en modo de solo
 * lectura, la evolución de tiempos de prueba, el resumen de asistencia y la
 * proyección de medallería de un atleta, leyendo exclusivamente a través de los
 * servicios públicos de los módulos checkup y training (frontera de módulo,
 * decisión D5 del design). No persiste ni muta ningún dato de esos módulos.
 */
@Service
public class AthleteReportingService {

    private final AthleteRepository athleteRepository;
    private final CheckupService checkupService;
    private final AttendanceService attendanceService;
    private final MedalProjectionService medalProjectionService;

    public AthleteReportingService(AthleteRepository athleteRepository,
                                   CheckupService checkupService,
                                   AttendanceService attendanceService,
                                   MedalProjectionService medalProjectionService) {
        this.athleteRepository = athleteRepository;
        this.checkupService = checkupService;
        this.attendanceService = attendanceService;
        this.medalProjectionService = medalProjectionService;
    }

    /**
     * Ensambla el reporte individual del deportista.
     *
     * @param athleteId ID del deportista
     * @return Reporte individual con evolución, asistencia y proyección
     * @throws ResourceNotFoundException si el deportista no existe (404)
     */
    @Transactional(readOnly = true)
    public IndividualReportResponse assembleIndividualReport(Long athleteId) {
        Athlete athlete = athleteRepository.findById(athleteId)
                .orElseThrow(() -> new ResourceNotFoundException("Athlete", "id", athleteId));

        List<TimeEvolutionEntry> timeEvolution = buildTimeEvolution(athleteId);
        AttendanceSummary attendance = buildAttendanceSummary(athleteId);
        List<MedalProjectionResponse> projections = medalProjectionService
                .getProjectionsForAthlete(athleteId).stream()
                .map(MedalProjectionResponse::fromMedalProjection)
                .toList();

        return new IndividualReportResponse(
                athleteId,
                athlete.getFullName(),
                timeEvolution,
                attendance,
                projections
        );
    }

    private List<TimeEvolutionEntry> buildTimeEvolution(Long athleteId) {
        List<Checkup> checkups = checkupService.getCheckupsByAthlete(athleteId, null, null);
        List<TimeEvolutionEntry> entries = new ArrayList<>();
        for (Checkup checkup : checkups) {
            for (CheckupTime time : checkupService.getTimesByCheckup(checkup.getId())) {
                entries.add(new TimeEvolutionEntry(
                        checkup.getYear(),
                        checkup.getMonth(),
                        time.getStyle(),
                        time.getDistance(),
                        time.getTimeSeconds(),
                        TimeFormatter.toFormatted(time.getTimeSeconds())
                ));
            }
        }
        entries.sort(Comparator.comparingInt(TimeEvolutionEntry::year)
                .thenComparingInt(TimeEvolutionEntry::month));
        return entries;
    }

    private AttendanceSummary buildAttendanceSummary(Long athleteId) {
        List<Attendance> records = attendanceService.getAttendanceByAthlete(athleteId);
        long present = 0;
        long absent = 0;
        long justified = 0;
        int streak = 0;
        for (Attendance record : records) {
            if (record.getStatus() == AttendanceStatus.AUSENTE) {
                absent++;
                streak++;
            } else if (record.getStatus() == AttendanceStatus.PRESENTE) {
                present++;
                streak = 0;
            } else if (record.getStatus() == AttendanceStatus.JUSTIFICADO) {
                justified++;
                streak = 0;
            } else {
                streak = 0;
            }
        }
        long total = present + absent + justified;
        return new AttendanceSummary(total, present, absent, justified, streak);
    }
}