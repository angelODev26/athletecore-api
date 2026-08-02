package com.athletecore.api.athlete;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio para entidad Athlete.
 * Proporciona métodos de acceso a datos para deportistas.
 */
@Repository
public interface AthleteRepository extends JpaRepository<Athlete, Long> {

    /**
     * Busca un deportista por username.
     * @param username Nombre de usuario
     * @return Optional con el atleta si existe
     */
    Optional<Athlete> findByUsername(String username);

    /**
     * Busca un deportista por email.
     * @param email Correo electrónico
     * @return Optional con el atleta si existe
     */
    Optional<Athlete> findByEmail(String email);

    /**
     * Verifica si un username ya existe (no cuenta eliminados lógicamente).
     * @param username Nombre de usuario
     * @return true si existe
     */
    boolean existsByUsername(String username);

    /**
     * Verifica si un email ya existe (no cuenta eliminados lógicamente).
     * @param email Correo electrónico
     * @return true si existe
     */
    boolean existsByEmail(String email);

    /**
     * Obtiene los atletas activos (no eliminados lógicamente) de forma paginada.
     * @param pageable Parámetros de paginación
     * @return Página de atletas activos
     */
    Page<Athlete> findAllByDeletedAtIsNull(Pageable pageable);
}
