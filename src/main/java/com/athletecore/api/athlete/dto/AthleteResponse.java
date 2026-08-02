package com.athletecore.api.athlete.dto;

import java.time.LocalDate;

import com.athletecore.api.athlete.Athlete;

/**
 * DTO de respuesta para deportistas.
 * Excluye campos internos y no expone entidades JPA directamente.
 */
public record AthleteResponse(
    Long id,
    String username,
    String email,
    String firstName,
    String lastName,
    String fullName,
    LocalDate birthDate,
    String photoUrl,
    boolean active
) {
    /**
     * Crea AthleteResponse desde una entidad Athlete.
     * @param athlete Entidad Athlete
     * @return AthleteResponse
     */
    public static AthleteResponse fromAthlete(Athlete athlete) {
        return new AthleteResponse(
            athlete.getId(),
            athlete.getUsername(),
            athlete.getEmail(),
            athlete.getFirstName(),
            athlete.getLastName(),
            athlete.getFullName(),
            athlete.getBirthDate(),
            athlete.getPhotoUrl(),
            athlete.isActive()
        );
    }
}
