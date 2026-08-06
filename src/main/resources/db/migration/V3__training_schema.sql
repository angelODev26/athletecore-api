-- ============================================================================
-- MIGRACIÓN V3: Esquema de entrenamientos (training) - AthleteCore API
-- ============================================================================
-- Versión: V3
-- Descripción: Crea tablas para planificación anual de entrenamientos (planes,
--              ciclos mesociclo/microciclo), registro de sesiones y control
--              de asistencia por deportista.
-- Dependencias: V1__initial_schema.sql (roles, users), V2__athletes_schema.sql
--               (athletes, disciplines)
-- ============================================================================

-- ============================================================================
-- TABLA: training_plans
-- Descripción: Plan anual de entrenamiento. Nivel raíz de la jerarquía
--              Plan → Mesociclo → Microciclo → Sesión.
--              Extiende BaseEntity con campos created_at, updated_at, deleted_at.
-- ============================================================================
CREATE TABLE training_plans (
    id              BIGSERIAL PRIMARY KEY,                    -- Identificador único autoincremental
    name            VARCHAR(150) NOT NULL,                    -- Nombre del plan anual
    start_date      DATE NOT NULL,                            -- Fecha de inicio del plan
    end_date        DATE NOT NULL,                            -- Fecha de fin del plan
    description     TEXT,                                     -- Descripción del plan

    -- Campos de auditoría (heredados de BaseEntity)
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at      TIMESTAMP WITH TIME ZONE                  -- Soft delete timestamp

    -- Restricciones para soft delete
    CONSTRAINT chk_training_plans_deleted CHECK (deleted_at IS NULL OR deleted_at > '1900-01-01'),
    -- Restricción de rango de fechas (fin no anterior a inicio)
    CONSTRAINT chk_training_plans_dates CHECK (end_date >= start_date)
);

-- Índice para búsqueda eficiente de planes por rango de fechas
CREATE INDEX idx_training_plans_dates ON training_plans(start_date, end_date) WHERE deleted_at IS NULL;

-- ============================================================================
-- TABLA: training_cycles
-- Descripción: Ciclos de planificación dentro de un plan anual. Autorelación
--              parent_cycle_id permite la jerarquía Mesociclo → Microciclo.
--              Extiende BaseEntity con campos created_at, updated_at, deleted_at.
-- ============================================================================
CREATE TABLE training_cycles (
    id              BIGSERIAL PRIMARY KEY,                    -- Identificador único autoincremental
    plan_id         BIGINT NOT NULL,                          -- Referencia al plan anual padre
    type            VARCHAR(20) NOT NULL,                     -- Tipo de ciclo: MESOCICLO | MICROCICLO
    name            VARCHAR(150) NOT NULL,                    -- Nombre del ciclo
    start_date      DATE,                                     -- Fecha de inicio del ciclo
    end_date        DATE,                                     -- Fecha de fin del ciclo
    order_index     INTEGER,                                  -- Orden del ciclo dentro de su nivel
    parent_cycle_id BIGINT,                                   -- Autorelación: ciclo padre (mesociclo)

    -- Campos de auditoría (heredados de BaseEntity)
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at      TIMESTAMP WITH TIME ZONE                  -- Soft delete timestamp

    -- Restricciones para soft delete
    CONSTRAINT chk_training_cycles_deleted CHECK (deleted_at IS NULL OR deleted_at > '1900-01-01'),
    -- Restricción de rango de fechas del ciclo
    CONSTRAINT chk_training_cycles_dates CHECK (end_date IS NULL OR start_date IS NULL OR end_date >= start_date)
);

-- Índices para consultas frecuentes (ciclos por plan y por padre)
CREATE INDEX idx_training_cycles_plan ON training_cycles(plan_id, parent_cycle_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_training_cycles_parent ON training_cycles(parent_cycle_id) WHERE deleted_at IS NULL;

-- Foreign Keys: plan padre y ciclo padre (autorelación)
ALTER TABLE training_cycles
ADD CONSTRAINT fk_training_cycles_plan
FOREIGN KEY (plan_id) REFERENCES training_plans(id) ON DELETE CASCADE;

ALTER TABLE training_cycles
ADD CONSTRAINT fk_training_cycles_parent
FOREIGN KEY (parent_cycle_id) REFERENCES training_cycles(id) ON DELETE CASCADE;

-- ============================================================================
-- TABLA: training_sessions
-- Descripción: Sesiones individuales de entrenamiento. Pueden asociarse a un
--              ciclo (cycle_id nullable: una sesión puede existir sin ciclo) y
--              a una disciplina del catálogo de V2 (disciplines).
--              Extiende BaseEntity con campos created_at, updated_at, deleted_at.
-- ============================================================================
CREATE TABLE training_sessions (
    id              BIGSERIAL PRIMARY KEY,                    -- Identificador único autoincremental
    cycle_id        BIGINT,                                   -- Referencia al ciclo padre (nullable)
    discipline_id   BIGINT,                                   -- Referencia a disciplina (nullable)
    session_date    DATE NOT NULL,                            -- Fecha de la sesión
    start_time      TIME,                                     -- Hora de inicio de la sesión
    status          VARCHAR(20) NOT NULL,                     -- Estado: PROGRAMADA | EJECUTADA | CANCELADA
    volume          INTEGER,                                  -- Volumen en metros/repeticiones (0-100000)
    intensity       INTEGER,                                  -- Intensidad en porcentaje (0-100)
    distance        DOUBLE PRECISION,                         -- Distancia recorrida en la sesión
    observations    TEXT,                                     -- Observaciones de la sesión

    -- Campos de auditoría (heredados de BaseEntity)
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at      TIMESTAMP WITH TIME ZONE                  -- Soft delete timestamp

    -- Restricciones para soft delete
    CONSTRAINT chk_training_sessions_deleted CHECK (deleted_at IS NULL OR deleted_at > '1900-01-01'),
    -- Restricciones de rango para volumen e intensidad
    CONSTRAINT chk_training_sessions_volume CHECK (volume IS NULL OR (volume >= 0 AND volume <= 100000)),
    CONSTRAINT chk_training_sessions_intensity CHECK (intensity IS NULL OR (intensity >= 0 AND intensity <= 100)),
    CONSTRAINT chk_training_sessions_distance CHECK (distance IS NULL OR distance >= 0),
    -- Restricción de estado válido
    CONSTRAINT chk_training_sessions_status CHECK (status IN ('PROGRAMADA', 'EJECUTADA', 'CANCELADA'))
);

-- Índice parcial para la consulta más frecuente: sesiones por ciclo ordenadas por fecha
CREATE INDEX idx_training_sessions_cycle ON training_sessions(cycle_id, session_date) WHERE deleted_at IS NULL;
-- Índice para búsquedas por fecha
CREATE INDEX idx_training_sessions_date ON training_sessions(session_date) WHERE deleted_at IS NULL;

-- Foreign Keys: ciclo y disciplina
ALTER TABLE training_sessions
ADD CONSTRAINT fk_training_sessions_cycle
FOREIGN KEY (cycle_id) REFERENCES training_cycles(id) ON DELETE CASCADE;

ALTER TABLE training_sessions
ADD CONSTRAINT fk_training_sessions_discipline
FOREIGN KEY (discipline_id) REFERENCES disciplines(id) ON DELETE SET NULL;

-- ============================================================================
-- TABLA: attendance
-- Descripción: Control de asistencia por deportista y sesión. Una fila por
--              par (session_id, athlete_id). La unicidad se garantiza con un
--              índice único PARCIAL (WHERE deleted_at IS NULL) para permitir
--              re-registrar asistencia tras un soft delete del registro.
--              Extiende BaseEntity con campos created_at, updated_at, deleted_at.
-- ============================================================================
CREATE TABLE attendance (
    id              BIGSERIAL PRIMARY KEY,                    -- Identificador único autoincremental
    session_id      BIGINT NOT NULL,                          -- Referencia a la sesión
    athlete_id      BIGINT NOT NULL,                          -- Referencia al deportista
    status          VARCHAR(20) NOT NULL,                     -- Estado: PRESENTE | AUSENTE | JUSTIFICADO

    -- Campos de auditoría (heredados de BaseEntity)
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at      TIMESTAMP WITH TIME ZONE                  -- Soft delete timestamp

    -- Restricciones para soft delete
    CONSTRAINT chk_attendance_deleted CHECK (deleted_at IS NULL OR deleted_at > '1900-01-01'),
    -- Restricción de estado válido
    CONSTRAINT chk_attendance_status CHECK (status IN ('PRESENTE', 'AUSENTE', 'JUSTIFICADO'))
);

-- Índice único parcial: una fila activa por (session_id, athlete_id)
CREATE UNIQUE INDEX uq_attendance_session_athlete ON attendance(session_id, athlete_id) WHERE deleted_at IS NULL;

-- Índices para consultas frecuentes: asistencia por sesión y por deportista
CREATE INDEX idx_attendance_session ON attendance(session_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_attendance_athlete ON attendance(athlete_id) WHERE deleted_at IS NULL;

-- Foreign Keys: sesión y deportista
ALTER TABLE attendance
ADD CONSTRAINT fk_attendance_session
FOREIGN KEY (session_id) REFERENCES training_sessions(id) ON DELETE CASCADE;

ALTER TABLE attendance
ADD CONSTRAINT fk_attendance_athlete
FOREIGN KEY (athlete_id) REFERENCES athletes(id) ON DELETE CASCADE;

-- ============================================================================
-- COMENTARIOS DOCUMENTALES (PostgreSQL Comments)
-- ============================================================================

COMMENT ON TABLE training_plans IS 'Planes anuales de entrenamiento, nivel raíz de la jerarquía Plan → Mesociclo → Microciclo → Sesión.';
COMMENT ON TABLE training_cycles IS 'Ciclos de planificación (mesociclos/microciclos) dentro de un plan anual, con autorelación parent_cycle_id.';
COMMENT ON TABLE training_sessions IS 'Sesiones individuales de entrenamiento con fecha, hora, estado, volumen, intensidad, distancia y observaciones.';
COMMENT ON TABLE attendance IS 'Control de asistencia por deportista y sesión, con estados PRESENTE, AUSENTE y JUSTIFICADO.';

COMMENT ON COLUMN training_plans.id IS 'Identificador único autoincremental del plan anual.';
COMMENT ON COLUMN training_plans.name IS 'Nombre del plan anual de entrenamiento.';
COMMENT ON COLUMN training_plans.start_date IS 'Fecha de inicio del plan anual.';
COMMENT ON COLUMN training_plans.end_date IS 'Fecha de fin del plan anual. No puede ser anterior a start_date.';
COMMENT ON COLUMN training_plans.description IS 'Descripción detallada del plan anual.';
COMMENT ON COLUMN training_plans.created_at IS 'Fecha y hora de creación del plan.';
COMMENT ON COLUMN training_plans.updated_at IS 'Fecha y hora de la última actualización.';
COMMENT ON COLUMN training_plans.deleted_at IS 'Fecha de eliminación lógica (soft delete). NULL = activo.';

COMMENT ON COLUMN training_cycles.id IS 'Identificador único autoincremental del ciclo.';
COMMENT ON COLUMN training_cycles.plan_id IS 'Identificador del plan anual padre (clave foránea a training_plans).';
COMMENT ON COLUMN training_cycles.type IS 'Tipo de ciclo: MESOCICLO o MICROCICLO. Un microciclo solo puede ser hijo de un mesociclo.';
COMMENT ON COLUMN training_cycles.name IS 'Nombre del ciclo de planificación.';
COMMENT ON COLUMN training_cycles.start_date IS 'Fecha de inicio del ciclo. Debe estar dentro del rango del plan.';
COMMENT ON COLUMN training_cycles.end_date IS 'Fecha de fin del ciclo. Debe estar dentro del rango del plan.';
COMMENT ON COLUMN training_cycles.order_index IS 'Orden del ciclo dentro de su nivel jerárquico.';
COMMENT ON COLUMN training_cycles.parent_cycle_id IS 'Identificador del ciclo padre (autorelación). Solo los microciclos referencian un mesociclo.';
COMMENT ON COLUMN training_cycles.created_at IS 'Fecha y hora de creación del ciclo.';
COMMENT ON COLUMN training_cycles.updated_at IS 'Fecha y hora de la última actualización.';
COMMENT ON COLUMN training_cycles.deleted_at IS 'Fecha de eliminación lógica (soft delete). NULL = activo.';

COMMENT ON COLUMN training_sessions.id IS 'Identificador único autoincremental de la sesión.';
COMMENT ON COLUMN training_sessions.cycle_id IS 'Identificador del ciclo padre (clave foránea a training_cycles). NULL si la sesión no pertenece a un ciclo.';
COMMENT ON COLUMN training_sessions.discipline_id IS 'Identificador de la disciplina (clave foránea a disciplines). NULL si no aplica disciplina.';
COMMENT ON COLUMN training_sessions.session_date IS 'Fecha en que se realiza la sesión.';
COMMENT ON COLUMN training_sessions.start_time IS 'Hora de inicio de la sesión.';
COMMENT ON COLUMN training_sessions.status IS 'Estado de la sesión: PROGRAMADA, EJECUTADA o CANCELADA.';
COMMENT ON COLUMN training_sessions.volume IS 'Volumen de la sesión en metros o repeticiones (rango 0-100000).';
COMMENT ON COLUMN training_sessions.intensity IS 'Intensidad de la sesión en porcentaje (rango 0-100).';
COMMENT ON COLUMN training_sessions.distance IS 'Distancia recorrida en la sesión (mayor o igual a 0).';
COMMENT ON COLUMN training_sessions.observations IS 'Observaciones técnicas de la sesión.';
COMMENT ON COLUMN training_sessions.created_at IS 'Fecha y hora de creación de la sesión.';
COMMENT ON COLUMN training_sessions.updated_at IS 'Fecha y hora de la última actualización.';
COMMENT ON COLUMN training_sessions.deleted_at IS 'Fecha de eliminación lógica (soft delete). NULL = activo.';

COMMENT ON COLUMN attendance.id IS 'Identificador único autoincremental del registro de asistencia.';
COMMENT ON COLUMN attendance.session_id IS 'Identificador de la sesión (clave foránea a training_sessions).';
COMMENT ON COLUMN attendance.athlete_id IS 'Identificador del deportista (clave foránea a athletes).';
COMMENT ON COLUMN attendance.status IS 'Estado de asistencia: PRESENTE, AUSENTE o JUSTIFICADO.';
COMMENT ON COLUMN attendance.created_at IS 'Fecha y hora de creación del registro de asistencia.';
COMMENT ON COLUMN attendance.updated_at IS 'Fecha y hora de la última actualización.';
COMMENT ON COLUMN attendance.deleted_at IS 'Fecha de eliminación lógica (soft delete). NULL = activo.';

-- ============================================================================
-- DATOS DE REFERENCIA INICIALES
-- ============================================================================

-- Rol COACH requerido por el módulo de entrenamientos (@PreAuthorize ADMIN/COACH)
INSERT INTO roles (name, created_at, updated_at) VALUES
    ('ROLE_COACH', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (name) DO NOTHING;
