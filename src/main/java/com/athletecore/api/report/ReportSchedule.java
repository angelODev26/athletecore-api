package com.athletecore.api.report;

import java.time.Instant;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import com.athletecore.api.domain.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidad de programación automática recurrente de reportes. Almacena la
 * expresión cron, el flag active y las marcas de última/próxima ejecución.
 * Es ejecutada por el scheduler de la aplicación (@Scheduled + CronExpression),
 * decisión D2 del design. Extiende BaseEntity para auditoría y soft delete.
 */
@Entity
@Table(name = "report_schedules")
@SQLDelete(sql = "UPDATE report_schedules SET deleted_at = now(), updated_at = now() WHERE id=?")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportSchedule extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "report_schedule_id_seq")
    @SequenceGenerator(name = "report_schedule_id_seq", sequenceName = "report_schedules_id_seq", allocationSize = 1)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", nullable = false, length = 20)
    private ReportType reportType;

    /**
     * Identificador del deportista (opcional, reporte individual). Es una columna
     * FK opcional, no una relación JPA (decisión D5 del design).
     */
    @Column(name = "athlete_id")
    private Long athleteId;

    @Column(length = 30)
    private String category;

    @NotBlank(message = "La expresión cron es obligatoria")
    @Size(max = 100, message = "La expresión cron no puede superar 100 caracteres")
    @Column(name = "cron_expression", nullable = false, length = 100)
    private String cronExpression;

    @Column(length = 63)
    private String timezone;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = Boolean.TRUE;

    @Column(name = "last_run_at")
    private Instant lastRunAt;

    @Column(name = "next_run_at")
    private Instant nextRunAt;
}