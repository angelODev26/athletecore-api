package com.athletecore.api.training;

import java.time.LocalDate;
import java.time.LocalTime;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import com.athletecore.api.athlete.Discipline;
import com.athletecore.api.domain.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidad de sesión de entrenamiento.
 * Puede asociarse opcionalmente a un ciclo (TrainingCycle) y a una disciplina
 * del catálogo existente (Discipline). Extiende BaseEntity para auditoría y soft delete.
 */
@Entity
@Table(name = "training_sessions")
@SQLDelete(sql = "UPDATE training_sessions SET deleted_at = now(), updated_at = now() WHERE id=?")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainingSession extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "training_session_id_seq")
    @SequenceGenerator(name = "training_session_id_seq", sequenceName = "training_sessions_id_seq", allocationSize = 1)
    private Long id;

    /**
     * Ciclo al que pertenece la sesión.
     * Es opcional (nullable): una sesión puede existir sin ciclo.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cycle_id")
    private TrainingCycle cycle;

    /**
     * Disciplina deportiva de la sesión (catálogo del módulo athlete).
     * Opcional: se conserva la integridad referencial con disciplines.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "discipline_id")
    private Discipline discipline;

    @NotNull(message = "La fecha de la sesión es obligatoria")
    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;

    @Column(name = "start_time")
    private LocalTime startTime;

    @NotNull(message = "El estado de la sesión es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SessionStatus status;

    /** Volumen de la sesión en metros o repeticiones (rango 0-100000). */
    @Min(value = 0, message = "El volumen no puede ser negativo")
    @Max(value = 100000, message = "El volumen no puede superar 100000")
    @Column
    private Integer volume;

    /** Intensidad de la sesión en porcentaje (rango 0-100). */
    @Min(value = 0, message = "La intensidad no puede ser negativa")
    @Max(value = 100, message = "La intensidad no puede superar 100")
    @Column
    private Integer intensity;

    @Column
    private Double distance;

    @Column(columnDefinition = "TEXT")
    private String observations;
}