package com.athletecore.api.athlete;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.athletecore.api.athlete.dto.AthleteProfileRequest;
import com.athletecore.api.common.exception.ResourceNotFoundException;

/**
 * Servicio para gestión del perfil antropométrico de deportistas.
 */
@Service
public class AthleteProfileService {

    private final AthleteRepository athleteRepository;
    private final AthleteProfileRepository athleteProfileRepository;

    public AthleteProfileService(AthleteRepository athleteRepository,
                                 AthleteProfileRepository athleteProfileRepository) {
        this.athleteRepository = athleteRepository;
        this.athleteProfileRepository = athleteProfileRepository;
    }

    /**
     * Crea o actualiza el perfil antropométrico de un atleta.
     * @param athleteId ID del atleta
     * @param request Datos antropométricos
     * @return AthleteProfile creado o actualizado
     * @throws ResourceNotFoundException si el atleta no existe
     */
    @Transactional
    public AthleteProfile upsertProfile(Long athleteId, AthleteProfileRequest request) {
        Athlete athlete = athleteRepository.findById(athleteId)
            .filter(a -> !a.isDeleted())
            .orElseThrow(() -> new ResourceNotFoundException("Athlete", "id", athleteId));

        AthleteProfile profile = athleteProfileRepository.findByAthleteId(athleteId)
            .orElseGet(() -> {
                AthleteProfile newProfile = new AthleteProfile();
                newProfile.setAthlete(athlete);
                return newProfile;
            });

        profile.setWeightKgs(request.weightKgs());
        profile.setHeightCm(request.heightCm());
        profile.setArmSpanCm(request.armSpanCm());
        profile.setNotes(request.notes());

        return athleteProfileRepository.save(profile);
    }

    /**
     * Obtiene el perfil antropométrico de un atleta.
     * @param athleteId ID del atleta
     * @return AthleteProfile del atleta
     * @throws ResourceNotFoundException si el atleta o su perfil no existen
     */
    @Transactional(readOnly = true)
    public AthleteProfile getProfile(Long athleteId) {
        athleteRepository.findById(athleteId)
            .filter(a -> !a.isDeleted())
            .orElseThrow(() -> new ResourceNotFoundException("Athlete", "id", athleteId));

        return athleteProfileRepository.findByAthleteId(athleteId)
            .orElseThrow(() -> new ResourceNotFoundException("AthleteProfile", "athleteId", athleteId));
    }
}
