package com.athletecore.api.checkup;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repositorio para entidad CheckupTime.
 * Las consultas activas filtran deleted_at IS NULL de forma explícita además
 * de la @SQLRestriction de la entidad.
 */
@Repository
public interface CheckupTimeRepository extends JpaRepository<CheckupTime, Long> {

    /**
     * Lista los tiempos activos de un chequeo, ordenados por estilo y distancia.
     */
    @Query("SELECT ct FROM CheckupTime ct WHERE ct.checkup.id = :checkupId AND ct.deletedAt IS NULL "
            + "ORDER BY ct.style ASC, ct.distance ASC")
    List<CheckupTime> findActiveByCheckupId(@Param("checkupId") Long checkupId);

    /**
     * Busca el tiempo activo de un chequeo para un (style, distance) dados.
     */
    @Query("SELECT ct FROM CheckupTime ct WHERE ct.checkup.id = :checkupId AND ct.style = :style "
            + "AND ct.distance = :distance AND ct.deletedAt IS NULL")
    Optional<CheckupTime> findActiveByCheckupIdAndStyleAndDistance(@Param("checkupId") Long checkupId,
            @Param("style") String style, @Param("distance") Integer distance);

    /**
     * Verifica si existe un tiempo activo para el mismo (checkup, style, distance).
     */
    @Query("SELECT CASE WHEN COUNT(ct) > 0 THEN TRUE ELSE FALSE END FROM CheckupTime ct "
            + "WHERE ct.checkup.id = :checkupId AND ct.style = :style AND ct.distance = :distance "
            + "AND ct.deletedAt IS NULL")
    boolean existsActiveByCheckupIdAndStyleAndDistance(@Param("checkupId") Long checkupId,
            @Param("style") String style, @Param("distance") Integer distance);
}
