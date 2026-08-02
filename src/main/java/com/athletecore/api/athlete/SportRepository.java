package com.athletecore.api.athlete;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * Repositorio para entidad Sport.
 */
@Repository
public interface SportRepository extends JpaRepository<Sport, Long> {

    /**
     * Busca un deporte por nombre.
     * @param name Nombre del deporte
     * @return Optional con el deporte si existe
     */
    Optional<Sport> findByName(String name);

    /**
     * Verifica si un deporte con ese nombre ya existe.
     * @param name Nombre del deporte
     * @return true si existe
     */
    boolean existsByName(String name);

    /**
     * Obtiene todos los deportes activos con sus disciplinas (sin N+1).
     * @return Lista de deportes activos
     */
    @Query("SELECT DISTINCT s FROM Sport s LEFT JOIN FETCH s.disciplines WHERE s.deletedAt IS NULL")
    List<Sport> findAllByDeletedAtIsNull();
}
