package com.athletecore.api.checkup;

import java.math.BigDecimal;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import com.athletecore.api.domain.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
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
 * Entidad de tiempo nacional de referencia por (style, distance, category, position).
 * position: 1=oro, 2=plata, 3=bronce. Una fila por posición (decisión D3).
 * Extiende BaseEntity para auditoría y soft delete.
 */
@Entity
@Table(name = "national_reference_times")
@SQLDelete(sql = "UPDATE national_reference_times SET deleted_at = now(), updated_at = now() WHERE id=?")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NationalReferenceTime extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "national_reference_time_id_seq")
    @SequenceGenerator(name = "national_reference_time_id_seq", sequenceName = "national_reference_times_id_seq", allocationSize = 1)
    private Long id;

    @NotBlank(message = "El estilo es obligatorio")
    @Size(max = 20, message = "El estilo no puede superar 20 caracteres")
    @Column(nullable = false, length = 20)
    private String style;

    @NotNull(message = "La distancia es obligatoria")
    @Min(value = 1, message = "La distancia debe ser mayor a 0")
    @Column(nullable = false)
    private Integer distance;

    @NotBlank(message = "La categoría es obligatoria")
    @Size(max = 30, message = "La categoría no puede superar 30 caracteres")
    @Column(nullable = false, length = 30)
    private String category;

    @NotNull(message = "La posición es obligatoria")
    @Min(value = 1, message = "La posición debe ser 1, 2 o 3")
    @Max(value = 3, message = "La posición debe ser 1, 2 o 3")
    @Column(nullable = false)
    private Short position;

    @NotNull(message = "El tiempo es obligatorio")
    @DecimalMin(value = "0.0", inclusive = false, message = "El tiempo debe ser mayor a 0")
    @Column(name = "time_seconds", nullable = false, precision = 10, scale = 3)
    private BigDecimal timeSeconds;
}
