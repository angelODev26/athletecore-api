package com.athletecore.api.checkup;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import com.athletecore.api.athlete.Athlete;
import com.athletecore.api.domain.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidad de chequeo mensual de rendimiento de un deportista.
 * Una fila por par (athlete, year, month) y categoría de competición.
 * Extiende BaseEntity para auditoría y soft delete.
 */
@Entity
@Table(name = "checkups")
@SQLDelete(sql = "UPDATE checkups SET deleted_at = now(), updated_at = now() WHERE id=?")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Checkup extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "checkup_id_seq")
    @SequenceGenerator(name = "checkup_id_seq", sequenceName = "checkups_id_seq", allocationSize = 1)
    private Long id;

    /**
     * Deportista al que pertenece el chequeo.
     * Fetch.LAZY para no cargar el atleta completo en consultas de chequeo.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "athlete_id", nullable = false)
    private Athlete athlete;

    @NotNull(message = "El año es obligatorio")
    @Min(value = 1900, message = "El año debe estar entre 1900 y 2100")
    @Max(value = 2100, message = "El año debe estar entre 1900 y 2100")
    @Column(nullable = false)
    private Integer year;

    @NotNull(message = "El mes es obligatorio")
    @Min(value = 1, message = "El mes debe estar entre 1 y 12")
    @Max(value = 12, message = "El mes debe estar entre 1 y 12")
    @Column(nullable = false)
    private Integer month;

    /**
     * Categoría de competición (p.ej. INFANTIL, JUVENIL, MAYOR).
     * Se guarda como texto validado por CheckupCategory en la entrada para
     * permitir extensión sin migración (decisión D1).
     */
    @NotBlank(message = "La categoría es obligatoria")
    @Size(max = 30, message = "La categoría no puede superar 30 caracteres")
    @Column(nullable = false, length = 30)
    private String category;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
