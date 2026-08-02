package com.athletecore.api.athlete.dto;

import com.athletecore.api.athlete.AthleteProfile;

import java.math.BigDecimal;

/**
 * DTO de respuesta para perfil antropométrico.
 */
public record AthleteProfileResponse(
    Long id,
    BigDecimal weightKgs,
    BigDecimal heightCm,
    BigDecimal armSpanCm,
    Double bmi,
    String bmiCategory,
    String notes,
    boolean isComplete
) {
    /**
     * Crea AthleteProfileResponse desde una entidad AthleteProfile.
     */
    public static AthleteProfileResponse fromAthleteProfile(AthleteProfile profile) {
        Double bmi = profile.getWeightKgs() != null && profile.getHeightCm() != null
                ? profile.calculateBMI()
                : null;
        return new AthleteProfileResponse(
            profile.getId(),
            profile.getWeightKgs(),
            profile.getHeightCm(),
            profile.getArmSpanCm(),
            bmi,
            bmi != null ? profile.getBMICategory(bmi) : null,
            profile.getNotes(),
            profile.isComplete()
        );
    }
}
