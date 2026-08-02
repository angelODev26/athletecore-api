package com.athletecore.api.athlete;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio para entidad Discipline.
 */
@Repository
public interface DisciplineRepository extends JpaRepository<Discipline, Long> {

    /**
     * Obtiene todas las disciplinas de un deporte que no están eliminadas.
     * @param sportId ID del deporte padre
     * @return Lista de disciplinas activas
     */
    List<Discipline> findBySportIdAndDeletedAtIsNull(Long sportId);

    /**
     * Verifica si existe una disciplina con ese nombre para un deporte dado.
     * @param sportId ID del deporte
     * @param name Nombre de la disciplina
     * @return true si existe
     */
    boolean existsBySportIdAndName(Long sportId, String name);
}
