-- ============================================================================
-- MIGRACIÓN V4: Esquema de chequeos (checkup) - AthleteCore API
-- ============================================================================
-- Versión: V4
-- Descripción: Crea tablas para registro de chequeos mensuales de rendimiento,
--              tiempos de prueba por estilo/distancia y tabla nacional de
--              tiempos de referencia para comparación y proyección de medallería.
--              NOTA: No incluye la tabla medal_projections (ver D5 del
--              design.md del change implement-checkup-domain): MedalProjection
--              es un POJO/record calculado en tiempo de consulta, no persiste.
-- Dependencias: V2__athletes_schema.sql (athletes)
-- ============================================================================

-- ============================================================================
-- TABLA: checkups
-- Descripción: Chequeo mensual de rendimiento de un deportista. Una fila por
--              par (athlete, year, month) y categoría de competición. La
--              categoría se valida a nivel Java (enum) en Fase B, no por CHECK
--              en base de datos, para permitir extensión de categorías.
--              Extiende BaseEntity con campos created_at, updated_at, deleted_at.
-- ============================================================================
CREATE TABLE checkups (
    id              BIGSERIAL PRIMARY KEY,                    -- Identificador único autoincremental
    athlete_id      BIGINT NOT NULL,                          -- Referencia al deportista (FK a athletes)
    year            INTEGER NOT NULL,                         -- Año del chequeo (1900-2100)
    month           INTEGER NOT NULL,                         -- Mes del chequeo (1-12)
    category        VARCHAR(30) NOT NULL,                     -- Categoría de competición (validada por enum Java en Fase B)
    notes           TEXT,                                     -- Notas del chequeo

    -- Campos de auditoría (heredados de BaseEntity)
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at      TIMESTAMP WITH TIME ZONE                  -- Soft delete timestamp

    -- Restricciones para soft delete
    CONSTRAINT chk_checkups_deleted CHECK (deleted_at IS NULL OR deleted_at > '1900-01-01'),
    -- Restricciones de rango para año y mes
    CONSTRAINT chk_checkups_year CHECK (year BETWEEN 1900 AND 2100),
    CONSTRAINT chk_checkups_month CHECK (month BETWEEN 1 AND 12)
);

-- Índice único parcial: un chequeo activo por (athlete_id, year, month, category)
CREATE UNIQUE INDEX uq_checkups_athlete_year_month_category
    ON checkups(athlete_id, year, month, category) WHERE deleted_at IS NULL;

-- Índice para búsqueda de chequeos por deportista
-- (athletes.id es PK, pero no existe índice sobre checkups.athlete_id)
CREATE INDEX idx_checkups_athlete_id ON checkups(athlete_id) WHERE deleted_at IS NULL;

-- Foreign Key: athlete_id referencia a athletes(id)
ALTER TABLE checkups
ADD CONSTRAINT fk_checkups_athlete
FOREIGN KEY (athlete_id) REFERENCES athletes(id) ON DELETE RESTRICT;

-- ============================================================================
-- TABLA: checkup_times
-- Descripción: Tiempos de prueba registrados en un chequeo, por estilo y
--              distancia. time_seconds almacena segundos con hasta 3 decimales
--              (milisegundos) como NUMERIC(10,3) (D2), nunca INTERVAL.
--              Extiende BaseEntity con campos created_at, updated_at, deleted_at.
-- ============================================================================
CREATE TABLE checkup_times (
    id              BIGSERIAL PRIMARY KEY,                    -- Identificador único autoincremental
    checkup_id      BIGINT NOT NULL,                          -- Referencia al chequeo padre (FK a checkups)
    style           VARCHAR(20) NOT NULL,                     -- Estilo de nado (e.g., LIBRE, ESPALDA)
    distance        INTEGER NOT NULL,                         -- Distancia de la prueba en metros (> 0)
    time_seconds    NUMERIC(10,3) NOT NULL,                   -- Tiempo en segundos con 3 decimales (> 0)

    -- Campos de auditoría (heredados de BaseEntity)
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at      TIMESTAMP WITH TIME ZONE                  -- Soft delete timestamp

    -- Restricciones para soft delete
    CONSTRAINT chk_checkup_times_deleted CHECK (deleted_at IS NULL OR deleted_at > '1900-01-01'),
    -- Restricciones de rango para distancia y tiempo
    CONSTRAINT chk_checkup_times_distance CHECK (distance > 0),
    CONSTRAINT chk_checkup_times_time_seconds CHECK (time_seconds > 0)
);

-- Índice único parcial: una fila activa por (checkup_id, style, distance)
CREATE UNIQUE INDEX uq_checkup_times_checkup_style_distance
    ON checkup_times(checkup_id, style, distance) WHERE deleted_at IS NULL;

-- Índice para búsqueda de tiempos por chequeo
CREATE INDEX idx_checkup_times_checkup_id ON checkup_times(checkup_id) WHERE deleted_at IS NULL;

-- Foreign Key: checkup_id referencia a checkups(id)
ALTER TABLE checkup_times
ADD CONSTRAINT fk_checkup_times_checkup
FOREIGN KEY (checkup_id) REFERENCES checkups(id) ON DELETE RESTRICT;

-- ============================================================================
-- TABLA: national_reference_times
-- Descripción: Tabla nacional de tiempos de referencia por (style, distance,
--              category, position). Una fila por posición (1=oro, 2=plata,
--              3=bronce) en lugar de columnas time_gold/silver/bronze (D3).
--              No depende por FK de otras tablas nuevas: la relación con
--              checkup_times se resuelve lógicamente por (style, distance,
--              category) en consultas.
--              Extiende BaseEntity con campos created_at, updated_at, deleted_at.
-- ============================================================================
CREATE TABLE national_reference_times (
    id              BIGSERIAL PRIMARY KEY,                    -- Identificador único autoincremental
    style           VARCHAR(20) NOT NULL,                     -- Estilo de nado (e.g., LIBRE, ESPALDA)
    distance        INTEGER NOT NULL,                         -- Distancia de la prueba en metros (> 0)
    category        VARCHAR(30) NOT NULL,                     -- Categoría de competición (validada por enum Java en Fase B)
    position        SMALLINT NOT NULL,                        -- Posición de medallería: 1=oro, 2=plata, 3=bronce
    time_seconds    NUMERIC(10,3) NOT NULL,                   -- Tiempo de referencia en segundos con 3 decimales (> 0)

    -- Campos de auditoría (heredados de BaseEntity)
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at      TIMESTAMP WITH TIME ZONE                  -- Soft delete timestamp

    -- Restricciones para soft delete
    CONSTRAINT chk_national_reference_times_deleted CHECK (deleted_at IS NULL OR deleted_at > '1900-01-01'),
    -- Restricciones de rango para distancia, tiempo y posición
    CONSTRAINT chk_national_reference_times_distance CHECK (distance > 0),
    CONSTRAINT chk_national_reference_times_time_seconds CHECK (time_seconds > 0),
    CONSTRAINT chk_national_reference_times_position CHECK (position IN (1, 2, 3))
);

-- Índice único parcial: una fila activa por (style, distance, category, position)
CREATE UNIQUE INDEX uq_national_reference_times_style_distance_category_position
    ON national_reference_times(style, distance, category, position) WHERE deleted_at IS NULL;

-- Índice para comparación/ranking por (style, distance, category)
CREATE INDEX idx_national_reference_times_style_distance_category
    ON national_reference_times(style, distance, category) WHERE deleted_at IS NULL;

-- ============================================================================
-- COMENTARIOS DOCUMENTALES (PostgreSQL Comments)
-- ============================================================================

COMMENT ON TABLE checkups IS 'Chequeos mensuales de rendimiento de deportistas, agrupados por (athlete, year, month) y categoría de competición.';
COMMENT ON TABLE checkup_times IS 'Tiempos de prueba registrados por chequeo, por estilo y distancia, en segundos con 3 decimales (NUMERIC(10,3)).';
COMMENT ON TABLE national_reference_times IS 'Tabla nacional de tiempos de referencia por (style, distance, category, position), con una fila por posición de medallería (1=oro, 2=plata, 3=bronce).';

COMMENT ON COLUMN checkups.id IS 'Identificador único autoincremental del chequeo.';
COMMENT ON COLUMN checkups.athlete_id IS 'Identificador del deportista (clave foránea a athletes).';
COMMENT ON COLUMN checkups.year IS 'Año del chequeo (rango válido: 1900-2100).';
COMMENT ON COLUMN checkups.month IS 'Mes del chequeo (rango válido: 1-12).';
COMMENT ON COLUMN checkups.category IS 'Categoría de competición (p.ej. INFANTIL, JUVENIL, MAYOR, MASTER). Validada por enum Java, extensible sin migración.';
COMMENT ON COLUMN checkups.notes IS 'Notas u observaciones del chequeo.';
COMMENT ON COLUMN checkups.created_at IS 'Fecha y hora de creación del chequeo.';
COMMENT ON COLUMN checkups.updated_at IS 'Fecha y hora de la última actualización.';
COMMENT ON COLUMN checkups.deleted_at IS 'Fecha de eliminación lógica (soft delete). NULL = activo.';

COMMENT ON COLUMN checkup_times.id IS 'Identificador único autoincremental del tiempo registrado.';
COMMENT ON COLUMN checkup_times.checkup_id IS 'Identificador del chequeo padre (clave foránea a checkups).';
COMMENT ON COLUMN checkup_times.style IS 'Estilo de nado de la prueba (p.ej. LIBRE, ESPALDA, PECHO, MARIPOSA).';
COMMENT ON COLUMN checkup_times.distance IS 'Distancia de la prueba en metros (debe ser mayor a 0).';
COMMENT ON COLUMN checkup_times.time_seconds IS 'Tiempo de la prueba en segundos con hasta 3 decimales (milisegundos), mayor a 0.';
COMMENT ON COLUMN checkup_times.created_at IS 'Fecha y hora de creación del registro de tiempo.';
COMMENT ON COLUMN checkup_times.updated_at IS 'Fecha y hora de la última actualización.';
COMMENT ON COLUMN checkup_times.deleted_at IS 'Fecha de eliminación lógica (soft delete). NULL = activo.';

COMMENT ON COLUMN national_reference_times.id IS 'Identificador único autoincremental del tiempo de referencia.';
COMMENT ON COLUMN national_reference_times.style IS 'Estilo de nado de la prueba (p.ej. LIBRE, ESPALDA, PECHO, MARIPOSA).';
COMMENT ON COLUMN national_reference_times.distance IS 'Distancia de la prueba en metros (debe ser mayor a 0).';
COMMENT ON COLUMN national_reference_times.category IS 'Categoría de competición (validada por enum Java, extensible sin migración).';
COMMENT ON COLUMN national_reference_times.position IS 'Posición de medallería: 1=oro, 2=plata, 3=bronce.';
COMMENT ON COLUMN national_reference_times.time_seconds IS 'Tiempo de referencia en segundos con hasta 3 decimales (milisegundos), mayor a 0.';
COMMENT ON COLUMN national_reference_times.created_at IS 'Fecha y hora de creación del tiempo de referencia.';
COMMENT ON COLUMN national_reference_times.updated_at IS 'Fecha y hora de la última actualización.';
COMMENT ON COLUMN national_reference_times.deleted_at IS 'Fecha de eliminación lógica (soft delete). NULL = activo.';

-- ============================================================================
-- ROLLBACK (reversión manual fuera de Flyway):
-- ============================================================================
-- DROP TABLE IF EXISTS checkup_times;
-- DROP TABLE IF EXISTS national_reference_times;
-- DROP TABLE IF EXISTS checkups;
-- (Se respeta el orden de FK: hijos antes que padres.)
-- ============================================================================
