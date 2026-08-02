package com.athletecore.api.athlete;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.athletecore.api.athlete.dto.AssignSportsRequest;
import com.athletecore.api.athlete.dto.AthleteProfileRequest;
import com.athletecore.api.athlete.dto.AthleteProfileResponse;
import com.athletecore.api.athlete.dto.AthleteResponse;
import com.athletecore.api.athlete.dto.CreateAthleteRequest;
import com.athletecore.api.athlete.dto.SportRequest;
import com.athletecore.api.athlete.dto.SportResponse;
import com.athletecore.api.athlete.dto.UpdateAthleteRequest;

import jakarta.validation.Valid;

/**
 * Controller REST para gestión de deportistas.
 * Provee endpoints CRUD completos para atletas, perfiles, deportes y disciplinas.
 */
@RestController
@RequestMapping("/api/v1/athletes")
public class AthleteController {

    private final AthleteRegistrationService athleteRegistrationService;
    private final AthleteSportService athleteSportService;
    private final AthleteProfileService athleteProfileService;
    private final SportService sportService;

    public AthleteController(AthleteRegistrationService athleteRegistrationService,
                             AthleteSportService athleteSportService,
                             AthleteProfileService athleteProfileService,
                             SportService sportService) {
        this.athleteRegistrationService = athleteRegistrationService;
        this.athleteSportService = athleteSportService;
        this.athleteProfileService = athleteProfileService;
        this.sportService = sportService;
    }

    /**
     * Registra un nuevo deportista.
     * POST /api/v1/athletes
     */
    @PostMapping
    public ResponseEntity<AthleteResponse> createAthlete(@Valid @RequestBody CreateAthleteRequest request) {
        Athlete athlete = athleteRegistrationService.registerAthlete(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(AthleteResponse.fromAthlete(athlete));
    }

    /**
     * Obtiene la lista paginada de deportistas activos.
     * GET /api/v1/athletes?page=0&size=20
     */
    @GetMapping
    public ResponseEntity<Page<AthleteResponse>> getAllAthletes(Pageable pageable) {
        Page<AthleteResponse> responses = athleteRegistrationService.getAllAthletes(pageable)
            .map(AthleteResponse::fromAthlete);
        return ResponseEntity.ok(responses);
    }

    /**
     * Obtiene un deportista por ID.
     * GET /api/v1/athletes/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<AthleteResponse> getAthlete(@PathVariable Long id) {
        Athlete athlete = athleteRegistrationService.getAthleteById(id);
        return ResponseEntity.ok(AthleteResponse.fromAthlete(athlete));
    }

    /**
     * Actualiza un deportista existente.
     * PUT /api/v1/athletes/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<AthleteResponse> updateAthlete(@PathVariable Long id,
                                                          @Valid @RequestBody UpdateAthleteRequest request) {
        Athlete athlete = athleteRegistrationService.updateAthlete(id, request);
        return ResponseEntity.ok(AthleteResponse.fromAthlete(athlete));
    }

    /**
     * Elimina lógicamente un deportista.
     * DELETE /api/v1/athletes/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> softDeleteAthlete(@PathVariable Long id) {
        athleteRegistrationService.softDeleteAthlete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Crea o actualiza el perfil antropométrico de un atleta.
     * POST /api/v1/athletes/{id}/profile
     */
    @PostMapping("/{id}/profile")
    public ResponseEntity<AthleteProfileResponse> updateAthleteProfile(@PathVariable Long id,
                                                                        @Valid @RequestBody AthleteProfileRequest request) {
        AthleteProfile profile = athleteProfileService.upsertProfile(id, request);
        return ResponseEntity.ok(AthleteProfileResponse.fromAthleteProfile(profile));
    }

    /**
     * Obtiene el perfil antropométrico de un atleta.
     * GET /api/v1/athletes/{id}/profile
     */
    @GetMapping("/{id}/profile")
    public ResponseEntity<AthleteProfileResponse> getAthleteProfile(@PathVariable Long id) {
        AthleteProfile profile = athleteProfileService.getProfile(id);
        return ResponseEntity.ok(AthleteProfileResponse.fromAthleteProfile(profile));
    }

    /**
     * Asigna deportes a un atleta.
     * POST /api/v1/athletes/{id}/sports
     */
    @PostMapping("/{id}/sports")
    public ResponseEntity<List<Long>> assignSports(@PathVariable Long id,
                                                    @Valid @RequestBody AssignSportsRequest request) {
        athleteSportService.assignSportsToAthlete(id, request);
        List<Long> sportIds = athleteSportService.getAthleteSportIds(id);
        return ResponseEntity.ok(sportIds);
    }

    /**
     * Obtiene los deportes de un atleta.
     * GET /api/v1/athletes/{id}/sports
     */
    @GetMapping("/{id}/sports")
    public ResponseEntity<List<Long>> getAthleteSports(@PathVariable Long id) {
        List<Long> sportIds = athleteSportService.getAthleteSportIds(id);
        return ResponseEntity.ok(sportIds);
    }

    /**
     * Obtiene la lista de todos los deportes activos.
     * GET /api/v1/athletes/sports
     */
    @GetMapping("/sports")
    public ResponseEntity<List<SportResponse>> getAllSports() {
        List<SportResponse> responses = sportService.getAllSports().stream()
            .map(SportResponse::fromSport)
            .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    /**
     * Crea un nuevo deporte (solo administradores).
     * POST /api/v1/athletes/sports
     */
    @PostMapping("/sports")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SportResponse> createSport(@Valid @RequestBody SportRequest request) {
        Sport savedSport = sportService.createSport(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(SportResponse.fromSport(savedSport));
    }
}
