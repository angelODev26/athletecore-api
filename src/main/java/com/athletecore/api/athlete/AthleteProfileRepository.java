package com.athletecore.api.athlete;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio para entidad AthleteProfile.
 */
@Repository
public interface AthleteProfileRepository extends JpaRepository<AthleteProfile, Long> {

    /**
     * Busca un perfil de atleta por el ID del atleta.
     * @param athleteId ID del atleta
     * @return Optional con el perfil si existe
     */
    Optional<AthleteProfile> findByAthleteId(Long athleteId);

    /**
     * Verifica si existe un perfil para un atleta dado.
     * @param athleteId ID del atleta
     * @return true si existe el perfil
     */
    boolean existsByAthleteId(Long athleteId);
}
