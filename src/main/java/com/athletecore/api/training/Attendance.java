package com.athletecore.api.training;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import com.athletecore.api.athlete.Athlete;
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
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidad de asistencia de un deportista a una sesión de entrenamiento.
 * Invariante de dominio: una fila activa por par (session, athlete); el
 * re-registro actualiza el estado (upsert). La unicidad se garantiza a nivel
 * de base de datos con un índice único parcial (WHERE deleted_at IS NULL).
 */
@Entity
@Table(name = "attendance", uniqueConstraints = @UniqueConstraint(
    name = "uq_attendance_session_athlete",
    columnNames = {"session_id", "athlete_id"}))
@SQLDelete(sql = "UPDATE attendance SET deleted_at = now(), updated_at = now() WHERE id=?")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Attendance extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "attendance_id_seq")
    @SequenceGenerator(name = "attendance_id_seq", sequenceName = "attendance_id_seq", allocationSize = 1)
    private Long id;

    @NotNull(message = "La sesión es obligatoria")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private TrainingSession session;

    @NotNull(message = "El deportista es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "athlete_id", nullable = false)
    private Athlete athlete;

    @NotNull(message = "El estado de asistencia es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AttendanceStatus status;
}