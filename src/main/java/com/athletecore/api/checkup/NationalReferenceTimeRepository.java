package com.athletecore.api.checkup;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repositorio para entidad NationalReferenceTime.
 * Las consultas activas filtran deleted_at IS NULL de forma explícita además
 * de la @SQLRestriction de la entidad.
 */
@Repository
public interface NationalReferenceTimeRepository extends JpaRepository<NationalReferenceTime, Long> {

    /**
     * Obtiene el triple activo de referencias (posiciones 1°, 2°, 3°) para un
     * (style, distance, category), ordenado por posición ascendente.
     */
    @Query("SELECT nrt FROM NationalReferenceTime nrt WHERE nrt.style = :style "
            + "AND nrt.distance = :distance AND nrt.category = :category AND nrt.deletedAt IS NULL "
            + "ORDER BY nrt.position ASC")
    List<NationalReferenceTime> findActiveTripleByStyleDistanceCategory(@Param("style") String style,
            @Param("distance") Integer distance, @Param("category") String category);

    /**
     * Verifica si existe una referencia activa para (style, distance, category, position).
     */
    @Query("SELECT CASE WHEN COUNT(nrt) > 0 THEN TRUE ELSE FALSE END FROM NationalReferenceTime nrt "
            + "WHERE nrt.style = :style AND nrt.distance = :distance AND nrt.category = :category "
            + "AND nrt.position = :position AND nrt.deletedAt IS NULL")
    boolean existsActiveByStyleDistanceCategoryAndPosition(@Param("style") String style,
            @Param("distance") Integer distance, @Param("category") String category,
            @Param("position") Short position);

    /**
     * Lista todas las referencias activas (para listados administrativos).
     */
    List<NationalReferenceTime> findAllByDeletedAtIsNull();
}
