package com.athletecore.api.checkup;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repositorio para entidad Checkup.
 * Las consultas activas filtran deleted_at IS NULL de forma explícita además
 * de la @SQLRestriction de la entidad.
 */
@Repository
public interface CheckupRepository extends JpaRepository<Checkup, Long> {

    /**
     * Lista los chequeos activos de un deportista, del más reciente al más antiguo.
     */
    @Query("SELECT c FROM Checkup c WHERE c.athlete.id = :athleteId AND c.deletedAt IS NULL "
            + "ORDER BY c.year DESC, c.month DESC")
    List<Checkup> findActiveByAthleteId(@Param("athleteId") Long athleteId);

    /**
     * Lista los chequeos activos de un deportista para un año y mes dados.
     * Puede haber varios por categoría (la unicidad incluye category).
     */
    @Query("SELECT c FROM Checkup c WHERE c.athlete.id = :athleteId AND c.year = :year "
            + "AND c.month = :month AND c.deletedAt IS NULL ORDER BY c.year DESC, c.month DESC")
    List<Checkup> findActiveByAthleteIdAndYearMonth(@Param("athleteId") Long athleteId,
            @Param("year") Integer year, @Param("month") Integer month);

    /**
     * Verifica si existe un chequeo activo para el mismo (athlete, year, month, category).
     */
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN TRUE ELSE FALSE END FROM Checkup c "
            + "WHERE c.athlete.id = :athleteId AND c.year = :year AND c.month = :month "
            + "AND c.category = :category AND c.deletedAt IS NULL")
    boolean existsActiveByAthleteIdAndYearMonthAndCategory(@Param("athleteId") Long athleteId,
            @Param("year") Integer year, @Param("month") Integer month,
            @Param("category") String category);
}
