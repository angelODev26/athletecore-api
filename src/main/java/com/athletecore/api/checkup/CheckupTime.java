package com.athletecore.api.checkup;

import java.math.BigDecimal;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

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
import jakarta.validation.constraints.DecimalMin;
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
 * Entidad de tiempo de prueba registrado en un chequeo, por estilo y distancia.
 * timeSeconds se almacena en segundos con hasta 3 decimales (NUMERIC(10,3));
 * el formateo mm:ss.ms es responsabilidad de la capa de presentación (DTO).
 * Extiende BaseEntity para auditoría y soft delete.
 */
@Entity
@Table(name = "checkup_times")
@SQLDelete(sql = "UPDATE checkup_times SET deleted_at = now(), updated_at = now() WHERE id=?")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckupTime extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "checkup_time_id_seq")
    @SequenceGenerator(name = "checkup_time_id_seq", sequenceName = "checkup_times_id_seq", allocationSize = 1)
    private Long id;

    /**
     * Chequeo padre al que pertenece el tiempo.
     * Fetch.LAZY para no cargar el chequeo completo en consultas de tiempos.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "checkup_id", nullable = false)
    private Checkup checkup;

    /**
     * Estilo de nado (p.ej. LIBRE, ESPALDA). Validado por SwimmingStyle en la entrada.
     */
    @NotBlank(message = "El estilo es obligatorio")
    @Size(max = 20, message = "El estilo no puede superar 20 caracteres")
    @Column(nullable = false, length = 20)
    private String style;

    @NotNull(message = "La distancia es obligatoria")
    @Min(value = 1, message = "La distancia debe ser mayor a 0")
    @Column(nullable = false)
    private Integer distance;

    @NotNull(message = "El tiempo es obligatorio")
    @DecimalMin(value = "0.0", inclusive = false, message = "El tiempo debe ser mayor a 0")
    @Column(name = "time_seconds", nullable = false, precision = 10, scale = 3)
    private BigDecimal timeSeconds;
}
