package com.athletecore.api.training;

import java.time.LocalDate;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidad de ciclo de planificación dentro de un plan anual.
 * Contiene una autorelación (parent) que permite la jerarquía
 * Mesociclo → Microciclo. Extiende BaseEntity para auditoría y soft delete.
 */
@Entity
@Table(name = "training_cycles")
@SQLDelete(sql = "UPDATE training_cycles SET deleted_at = now(), updated_at = now() WHERE id=?")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainingCycle extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "training_cycle_id_seq")
    @SequenceGenerator(name = "training_cycle_id_seq", sequenceName = "training_cycles_id_seq", allocationSize = 1)
    private Long id;

    @NotNull(message = "El plan anual es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private TrainingPlan plan;

    @NotNull(message = "El tipo de ciclo es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CycleType type;

    @NotBlank(message = "El nombre del ciclo es obligatorio")
    @Size(max = 150, message = "El nombre del ciclo no puede superar 150 caracteres")
    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "order_index")
    private Integer orderIndex;

    /**
     * Ciclo padre (autorelación).
     * Solo los microciclos referencian un mesociclo; los mesociclos no tienen padre.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_cycle_id")
    private TrainingCycle parent;
}