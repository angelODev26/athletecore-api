package com.athletecore.api.report;

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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidad de artefacto exportado de un reporte (PDF). Persiste el contenido
 * binario autocontenido (BYTEA), nombre de archivo, tipo MIME y tamaño para
 * descargar sin regenerar (decisión D4 del design).
 * Extiende BaseEntity para auditoría y soft delete.
 */
@Entity
@Table(name = "report_exports")
@SQLDelete(sql = "UPDATE report_exports SET deleted_at = now(), updated_at = now() WHERE id=?")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportExport extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "report_export_id_seq")
    @SequenceGenerator(name = "report_export_id_seq", sequenceName = "report_exports_id_seq", allocationSize = 1)
    private Long id;

    /**
     * Reporte padre al que pertenece el export.
     * Fetch.LAZY para no cargar el reporte completo en consultas de exports.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id", nullable = false)
    private Report report;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String format = "PDF";

    @NotBlank(message = "El nombre de archivo es obligatorio")
    @Size(max = 255, message = "El nombre de archivo no puede superar 255 caracteres")
    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "content_type", nullable = false, length = 100)
    @Builder.Default
    private String contentType = "application/pdf";

    @Column(nullable = false)
    private byte[] content;
}