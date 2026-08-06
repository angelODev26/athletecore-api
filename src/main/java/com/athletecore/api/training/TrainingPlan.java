package com.athletecore.api.training;

import java.time.LocalDate;

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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidad de plan anual de entrenamiento.
 * Nivel raíz de la jerarquía Plan → Mesociclo → Microciclo → Sesión.
 * Extiende BaseEntity para heredar auditoría y soft delete.
 */
@Entity
@Table(name = "training_plans")
@SQLDelete(sql = "UPDATE training_plans SET deleted_at = now(), updated_at = now() WHERE id=?")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainingPlan extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "training_plan_id_seq")
    @SequenceGenerator(name = "training_plan_id_seq", sequenceName = "training_plans_id_seq", allocationSize = 1)
    private Long id;

    @NotBlank(message = "El nombre del plan es obligatorio")
    @Size(max = 150, message = "El nombre del plan no puede superar 150 caracteres")
    @Column(nullable = false, length = 150)
    private String name;

    @NotNull(message = "La fecha de inicio es obligatoria")
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @NotNull(message = "La fecha de fin es obligatoria")
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(columnDefinition = "TEXT")
    private String description;
}