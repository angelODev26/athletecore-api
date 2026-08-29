package com.athletecore.api.report;

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
 * Entidad de reporte generado (individual o general) con su estado de ciclo de
 * vida (PENDING → GENERATED / FAILED). El deportista se referencia por su id
 * (columna athlete_id) de forma opcional, sin relación JPA (decisión D5 del
 * design): la resolución del atleta se hace vía repositorio en el servicio.
 * Extiende BaseEntity para auditoría y soft delete.
 */
@Entity
@Table(name = "reports")
@SQLDelete(sql = "UPDATE reports SET deleted_at = now(), updated_at = now() WHERE id=?")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Report extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "report_id_seq")
    @SequenceGenerator(name = "report_id_seq", sequenceName = "reports_id_seq", allocationSize = 1)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", nullable = false, length = 20)
    private ReportType reportType;

    /**
     * Identificador del deportista (solo reporte individual; NULL para general).
     * Es una columna FK opcional, no una relación JPA.
     */
    @Column(name = "athlete_id")
    private Long athleteId;

    @Column(length = 30)
    private String category;

    @Column
    private Integer year;

    @Column
    private Integer month;

    @NotBlank(message = "El título es obligatorio")
    @Size(max = 200, message = "El título no puede superar 200 caracteres")
    @Column(nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ReportStatus status = ReportStatus.PENDING;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
}