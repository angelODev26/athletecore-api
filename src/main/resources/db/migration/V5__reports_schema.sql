-- ============================================================================
-- MIGRACIÓN V5: Esquema de reportes (report) - AthleteCore API
-- ============================================================================
-- Versión: V5
-- Descripción: Crea tablas para la generación de reportes individuales y
--              generales, su programación automática recurrente y la
--              exportación de los artefactos (PDF). El contenido de los
--              reportes se ensambla en tiempo de consulta a partir de los
--              módulos athlete, training y checkup; no se materializa ninguna
--              tabla de proyección (ver D3 del design.md del change
--              implement-report-domain).
-- Dependencias: V2__athletes_schema.sql (athletes)
-- ============================================================================

-- ============================================================================
-- TABLA: reports
-- Descripción: Metadatos de un reporte generado (individual o general) con su
--              estado de ciclo de vida (PENDING → GENERATED / FAILED). El
--              artefacto exportado se persiste en report_exports.
--              Extiende BaseEntity con campos created_at, updated_at, deleted_at.
-- ============================================================================
CREATE TABLE reports (
    id              BIGSERIAL PRIMARY KEY,                       -- Identificador único autoincremental
    report_type     VARCHAR(20) NOT NULL,                        -- Tipo de reporte: INDIVIDUAL o GENERAL
    athlete_id      BIGINT,                                      -- Deportista (solo reporte individual; NULL para general)
    category        VARCHAR(30),                                 -- Categoría de competición (filtro opcional, general)
    year            INTEGER,                                     -- Año del filtro (opcional)
    month           INTEGER,                                     -- Mes del filtro (opcional)
    title           VARCHAR(200) NOT NULL,                       -- Título descriptivo del reporte
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',      -- Estado: PENDING, GENERATED, FAILED
    error_message   TEXT,                                        -- Mensaje de error cuando status = FAILED

    -- Campos de auditoría (heredados de BaseEntity)
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at      TIMESTAMP WITH TIME ZONE,                    -- Soft delete timestamp

    -- Restricciones para soft delete y valores de dominio
    CONSTRAINT chk_reports_deleted CHECK (deleted_at IS NULL OR deleted_at > '1900-01-01'),
    CONSTRAINT chk_reports_type CHECK (report_type IN ('INDIVIDUAL', 'GENERAL')),
    CONSTRAINT chk_reports_status CHECK (status IN ('PENDING', 'GENERATED', 'FAILED'))
);

-- Índice para búsqueda de reportes por deportista
CREATE INDEX idx_reports_athlete_id ON reports(athlete_id) WHERE deleted_at IS NULL;

-- Índice para listado/filtrado por tipo de reporte
CREATE INDEX idx_reports_report_type ON reports(report_type) WHERE deleted_at IS NULL;

-- Foreign Key: athlete_id referencia a athletes(id); opcional (reporte general)
ALTER TABLE reports
ADD CONSTRAINT fk_reports_athlete
FOREIGN KEY (athlete_id) REFERENCES athletes(id) ON DELETE RESTRICT;

-- ============================================================================
-- TABLA: report_schedules
-- Descripción: Programación automática recurrente de reportes. Almacena la
--              expresión cron, el estado active y las marcas de última/próxima
--              ejecución. Es ejecutada por el scheduler de la aplicación
--              (@Scheduled + CronExpression).
--              Extiende BaseEntity con campos created_at, updated_at, deleted_at.
-- ============================================================================
CREATE TABLE report_schedules (
    id              BIGSERIAL PRIMARY KEY,                       -- Identificador único autoincremental
    report_type     VARCHAR(20) NOT NULL,                        -- Tipo de reporte: INDIVIDUAL o GENERAL
    athlete_id      BIGINT,                                      -- Deportista (opcional, reporte individual)
    category        VARCHAR(30),                                 -- Categoría de competición (opcional)
    cron_expression VARCHAR(100) NOT NULL,                       -- Expresión cron (6 campos, Spring CronExpression)
    timezone        VARCHAR(63),                                 -- Zona horaria opcional (formato IANA)
    active          BOOLEAN NOT NULL DEFAULT TRUE,               -- Si el schedule está activo
    last_run_at     TIMESTAMP WITH TIME ZONE,                    -- Última ejecución exitosa
    next_run_at     TIMESTAMP WITH TIME ZONE,                    -- Próxima ejecución programada

    -- Campos de auditoría (heredados de BaseEntity)
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at      TIMESTAMP WITH TIME ZONE,                    -- Soft delete timestamp

    -- Restricciones para soft delete y valores de dominio
    CONSTRAINT chk_report_schedules_deleted CHECK (deleted_at IS NULL OR deleted_at > '1900-01-01'),
    CONSTRAINT chk_report_schedules_type CHECK (report_type IN ('INDIVIDUAL', 'GENERAL'))
);

-- Índice para el sondeo del scheduler de schedules activos
CREATE INDEX idx_report_schedules_active ON report_schedules(active) WHERE deleted_at IS NULL;

-- Foreign Key: athlete_id referencia a athletes(id); opcional
ALTER TABLE report_schedules
ADD CONSTRAINT fk_report_schedules_athlete
FOREIGN KEY (athlete_id) REFERENCES athletes(id) ON DELETE RESTRICT;

-- ============================================================================
-- TABLA: report_exports
-- Descripción: Artefacto exportado de un reporte (PDF). Persiste el contenido
--              binario (BYTEA), nombre de archivo, tipo MIME y tamaño para
--              descargas repetidas sin regenerar (decisión D4 del design).
--              Extiende BaseEntity con campos created_at, updated_at, deleted_at.
-- ============================================================================
CREATE TABLE report_exports (
    id              BIGSERIAL PRIMARY KEY,                       -- Identificador único autoincremental
    report_id       BIGINT NOT NULL,                             -- Reporte padre (FK a reports)
    format          VARCHAR(20) NOT NULL DEFAULT 'PDF',          -- Formato del artefacto (PDF)
    file_name       VARCHAR(255) NOT NULL,                       -- Nombre del archivo generado
    file_size_bytes BIGINT,                                      -- Tamaño del artefacto en bytes
    content_type    VARCHAR(100) NOT NULL DEFAULT 'application/pdf',  -- Tipo MIME del contenido
    content         BYTEA NOT NULL,                              -- Contenido binario del PDF

    -- Campos de auditoría (heredados de BaseEntity)
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at      TIMESTAMP WITH TIME ZONE,                    -- Soft delete timestamp

    -- Restricciones para soft delete
    CONSTRAINT chk_report_exports_deleted CHECK (deleted_at IS NULL OR deleted_at > '1900-01-01')
);

-- Índice para listado de exports por reporte
CREATE INDEX idx_report_exports_report_id ON report_exports(report_id) WHERE deleted_at IS NULL;

-- Foreign Key: report_id referencia a reports(id)
ALTER TABLE report_exports
ADD CONSTRAINT fk_report_exports_report
FOREIGN KEY (report_id) REFERENCES reports(id) ON DELETE RESTRICT;

-- ============================================================================
-- COMENTARIOS DOCUMENTALES (PostgreSQL Comments)
-- ============================================================================

COMMENT ON TABLE reports IS 'Metadatos de reportes generados (individuales o generales) con su estado de ciclo de vida PENDING → GENERATED / FAILED.';
COMMENT ON TABLE report_schedules IS 'Programación automática recurrente de reportes, con expresión cron y marcas de ejecución.';
COMMENT ON TABLE report_exports IS 'Artefactos exportados (PDF) de un reporte, con contenido binario autocontenido.';

COMMENT ON COLUMN reports.id IS 'Identificador único autoincremental del reporte.';
COMMENT ON COLUMN reports.report_type IS 'Tipo de reporte: INDIVIDUAL (por deportista) o GENERAL (por equipo/categoría).';
COMMENT ON COLUMN reports.athlete_id IS 'Identificador del deportista (solo reporte individual; NULL para general).';
COMMENT ON COLUMN reports.category IS 'Categoría de competición (filtro opcional del reporte general).';
COMMENT ON COLUMN reports.year IS 'Año del filtro del reporte (opcional).';
COMMENT ON COLUMN reports.month IS 'Mes del filtro del reporte (opcional).';
COMMENT ON COLUMN reports.title IS 'Título descriptivo del reporte.';
COMMENT ON COLUMN reports.status IS 'Estado del ciclo de vida: PENDING, GENERATED o FAILED.';
COMMENT ON COLUMN reports.error_message IS 'Mensaje de error cuando status = FAILED.';
COMMENT ON COLUMN reports.created_at IS 'Fecha y hora de creación del reporte.';
COMMENT ON COLUMN reports.updated_at IS 'Fecha y hora de la última actualización.';
COMMENT ON COLUMN reports.deleted_at IS 'Fecha de eliminación lógica (soft delete). NULL = activo.';

COMMENT ON COLUMN report_schedules.id IS 'Identificador único autoincremental de la programación.';
COMMENT ON COLUMN report_schedules.report_type IS 'Tipo de reporte que genera la programación: INDIVIDUAL o GENERAL.';
COMMENT ON COLUMN report_schedules.athlete_id IS 'Identificador del deportista (opcional, reporte individual).';
COMMENT ON COLUMN report_schedules.category IS 'Categoría de competición (opcional).';
COMMENT ON COLUMN report_schedules.cron_expression IS 'Expresión cron de 6 campos (formato Spring CronExpression).';
COMMENT ON COLUMN report_schedules.timezone IS 'Zona horaria opcional (formato IANA).';
COMMENT ON COLUMN report_schedules.active IS 'Indica si la programación está activa para el scheduler.';
COMMENT ON COLUMN report_schedules.last_run_at IS 'Fecha de la última ejecución exitosa.';
COMMENT ON COLUMN report_schedules.next_run_at IS 'Fecha de la próxima ejecución programada.';
COMMENT ON COLUMN report_schedules.created_at IS 'Fecha y hora de creación de la programación.';
COMMENT ON COLUMN report_schedules.updated_at IS 'Fecha y hora de la última actualización.';
COMMENT ON COLUMN report_schedules.deleted_at IS 'Fecha de eliminación lógica (soft delete). NULL = activo.';

COMMENT ON COLUMN report_exports.id IS 'Identificador único autoincremental del export.';
COMMENT ON COLUMN report_exports.report_id IS 'Identificador del reporte padre (clave foránea a reports).';
COMMENT ON COLUMN report_exports.format IS 'Formato del artefacto exportado (p.ej. PDF).';
COMMENT ON COLUMN report_exports.file_name IS 'Nombre del archivo generado.';
COMMENT ON COLUMN report_exports.file_size_bytes IS 'Tamaño del artefacto en bytes.';
COMMENT ON COLUMN report_exports.content_type IS 'Tipo MIME del contenido (p.ej. application/pdf).';
COMMENT ON COLUMN report_exports.content IS 'Contenido binario del artefacto exportado.';
COMMENT ON COLUMN report_exports.created_at IS 'Fecha y hora de creación del export.';
COMMENT ON COLUMN report_exports.updated_at IS 'Fecha y hora de la última actualización.';
COMMENT ON COLUMN report_exports.deleted_at IS 'Fecha de eliminación lógica (soft delete). NULL = activo.';

-- ============================================================================
-- ROLLBACK (reversión manual fuera de Flyway):
-- ============================================================================
-- DROP TABLE IF EXISTS report_exports;
-- DROP TABLE IF EXISTS report_schedules;
-- DROP TABLE IF EXISTS reports;
-- (Se respeta el orden de FK: hijos antes que padres.)
-- ============================================================================