-- ============================================================================
-- MIGRACIÓN V2: Esquema de deportistas (athletes) - AthleteCore API
-- ============================================================================
-- Versión: V2
-- Descripción: Crea tablas para gestión de deportistas, perfiles antropométricos,
--              deportes, disciplinas y relaciones.
-- Dependencias: V1__initial_schema.sql (roles, users, user_roles)
-- ============================================================================

-- ============================================================================
-- TABLA: athletes
-- Descripción: Tabla principal de deportistas del sistema.
--              Extiende BaseEntity con campos created_at, updated_at, deleted_at.
-- ============================================================================
CREATE TABLE athletes (
    id              BIGSERIAL PRIMARY KEY,                    -- Identificador único autoincremental
    username        VARCHAR(255) NOT NULL UNIQUE,             -- Nombre de usuario único
    email           VARCHAR(255) NOT NULL UNIQUE,             -- Correo electrónico único
    first_name      VARCHAR(100) NOT NULL,                    -- Nombre
    last_name       VARCHAR(100) NOT NULL,                    -- Apellido
    birth_date      DATE,                                     -- Fecha de nacimiento
    photo_url       VARCHAR(500),                             -- URL de foto de perfil (referencial)

    -- Campos de auditoría (heredados de BaseEntity)
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at      TIMESTAMP WITH TIME ZONE                  -- Soft delete timestamp

    -- Restricciones para soft delete
    CONSTRAINT chk_athletes_deleted CHECK (deleted_at IS NULL OR deleted_at > '1900-01-01')
);

-- Índices para búsqueda eficiente de deportistas
CREATE INDEX idx_athletes_username ON athletes(username) WHERE deleted_at IS NULL;
CREATE INDEX idx_athletes_email ON athletes(email) WHERE deleted_at IS NULL;
CREATE INDEX idx_athletes_fullname ON athletes(last_name, first_name) WHERE deleted_at IS NULL;
CREATE INDEX idx_athletes_birth_date ON athletes(birth_date) WHERE deleted_at IS NULL;

-- ============================================================================
-- TABLA: athlete_profiles
-- Descripción: Tabla de perfil antropométrico de deportistas.
--              Relación OneToOne con athletes (cada deportista tiene un perfil).
--              Extiende BaseEntity con campos created_at, updated_at, deleted_at.
-- ============================================================================
CREATE TABLE athlete_profiles (
    id              BIGSERIAL PRIMARY KEY,                    -- Identificador único autoincremental
    athlete_id      BIGINT NOT NULL UNIQUE,                   -- Referencia al atleta (relación OneToOne)
    weight_kgs      NUMERIC(5, 2),                            -- Peso en kilogramos (1-500)
    height_cm       NUMERIC(4, 1),                            -- Talla en centímetros (10-300)
    arm_span_cm     NUMERIC(4, 1),                            -- Envergadura en centímetros (10-300)
    notes           TEXT,                                     -- Notas adicionales del perfil

    -- Campos de auditoría (heredados de BaseEntity)
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at      TIMESTAMP WITH TIME ZONE                  -- Soft delete timestamp

    -- Restricciones para soft delete
    CONSTRAINT chk_athlete_profiles_deleted CHECK (deleted_at IS NULL OR deleted_at > '1900-01-01')
);

-- Índice para búsqueda por athlete_id
CREATE INDEX idx_athlete_profiles_athlete_id ON athlete_profiles(athlete_id) WHERE deleted_at IS NULL;

-- Foreign Key: athlete_id referencia a athletes(id)
ALTER TABLE athlete_profiles
ADD CONSTRAINT fk_athlete_profiles_athlete
FOREIGN KEY (athlete_id) REFERENCES athletes(id) ON DELETE CASCADE;

-- ============================================================================
-- TABLA: sports
-- Descripción: Tabla maestra de deportes/disciplinas deportivas.
--              Modelo genérico y extensible para soportar múltiples deportes.
--              Extiende BaseEntity con campos created_at, updated_at, deleted_at.
-- ============================================================================
CREATE TABLE sports (
    id              BIGSERIAL PRIMARY KEY,                    -- Identificador único autoincremental
    name            VARCHAR(100) NOT NULL UNIQUE,             -- Nombre del deporte (e.g., Natación, Atletismo)
    description     TEXT,                                     -- Descripción del deporte

    -- Campos de auditoría (heredados de BaseEntity)
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at      TIMESTAMP WITH TIME ZONE                  -- Soft delete timestamp

    -- Restricciones para soft delete
    CONSTRAINT chk_sports_deleted CHECK (deleted_at IS NULL OR deleted_at > '1900-01-01')
);

-- Índice único para nombre de deporte
CREATE UNIQUE INDEX idx_sports_name ON sports(name) WHERE deleted_at IS NULL;

-- ============================================================================
-- TABLA: disciplines
-- Descripción: Tabla de disciplinas dentro de cada deporte.
--              Relación OneToMany con sports (un deporte tiene múltiples disciplinas).
--              Extiende BaseEntity con campos created_at, updated_at, deleted_at.
-- ============================================================================
CREATE TABLE disciplines (
    id              BIGSERIAL PRIMARY KEY,                    -- Identificador único autoincremental
    sport_id        BIGINT NOT NULL,                          -- Referencia al deporte padre
    name            VARCHAR(100) NOT NULL,                    -- Nombre de la disciplina (e.g., Estilo Libre, Mariposa)
    description     TEXT,                                     -- Descripción de la disciplina

    -- Campos de auditoría (heredados de BaseEntity)
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at      TIMESTAMP WITH TIME ZONE                  -- Soft delete timestamp

    -- Restricciones para soft delete
    CONSTRAINT chk_disciplines_deleted CHECK (deleted_at IS NULL OR deleted_at > '1900-01-01')
);

-- Índice para búsqueda por sport_id
CREATE INDEX idx_disciplines_sport_id ON disciplines(sport_id) WHERE deleted_at IS NULL;

-- Foreign Key: sport_id referencia a sports(id)
ALTER TABLE disciplines
ADD CONSTRAINT fk_disciplines_sport
FOREIGN KEY (sport_id) REFERENCES sports(id) ON DELETE CASCADE;

-- ============================================================================
-- TABLA: athlete_sports (Tabla intermedia Many-to-Many)
-- Descripción: Asocia deportistas con deportes (relación muchos-a-muchos).
--              Permite que un deportista tenga múltiples deportes y que un deporte
--              sea asignado a múltiples deportistas.
-- ============================================================================
CREATE TABLE athlete_sports (
    athlete_id      BIGINT NOT NULL,
    sport_id        BIGINT NOT NULL,

    -- Campo para auditoría de la asignación
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (athlete_id, sport_id),

    -- Foreign Keys con referencias en cascada
    CONSTRAINT fk_athlete_sports_athlete
        FOREIGN KEY (athlete_id) REFERENCES athletes(id) ON DELETE CASCADE,
    CONSTRAINT fk_athlete_sports_sport
        FOREIGN KEY (sport_id) REFERENCES sports(id) ON DELETE CASCADE
);

-- Índices para consultas frecuentes
CREATE INDEX idx_athlete_sports_athlete_id ON athlete_sports(athlete_id);
CREATE INDEX idx_athlete_sports_sport_id ON athlete_sports(sport_id);

-- ============================================================================
-- DATOS DE REFERENCIA INICIALES
-- ============================================================================

-- Roles base del sistema (requeridos por UserService.createUser y @PreAuthorize)
INSERT INTO roles (name, created_at, updated_at) VALUES
    ('ROLE_USER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('ROLE_ADMIN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (name) DO NOTHING;

-- Deportes por defecto del sistema
INSERT INTO sports (name, description, created_at, updated_at) VALUES
    ('Natación', 'Deporte acuático que consiste en propulsarse a través del agua utilizando las extremidades', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Atletismo', 'Conjunto de disciplinas deportivas que incluyen carreras, saltos y lanzamientos', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Ciclismo', 'Deporte de desplazamiento en bicicleta por diferentes tipos de terreno', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Nado Sincronizado', 'Deporte acuático que combina natación, danza y música', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (name) DO NOTHING;

-- Disciplinas por defecto para Natación
INSERT INTO disciplines (sport_id, name, description, created_at, updated_at)
SELECT s.id, 'Estilo Libre', 'Nado en estilo libre (crol)', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM sports s WHERE s.name = 'Natación' ON CONFLICT DO NOTHING;

INSERT INTO disciplines (sport_id, name, description, created_at, updated_at)
SELECT s.id, 'Espalda', 'Nado en posición supina', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM sports s WHERE s.name = 'Natación' ON CONFLICT DO NOTHING;

INSERT INTO disciplines (sport_id, name, description, created_at, updated_at)
SELECT s.id, 'Pecho', 'Nado en posición prona con movimiento simultáneo de brazos y piernas', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM sports s WHERE s.name = 'Natación' ON CONFLICT DO NOTHING;

INSERT INTO disciplines (sport_id, name, description, created_at, updated_at)
SELECT s.id, 'Mariposa', 'Nado con movimiento simétrico de brazos y patada de delfín', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM sports s WHERE s.name = 'Natación' ON CONFLICT DO NOTHING;

-- ============================================================================
-- COMENTARIOS DOCUMENTALES (PostgreSQL Comments)
-- ============================================================================

COMMENT ON TABLE athletes IS 'Tabla principal de deportistas de la plataforma AthleteCore.';
COMMENT ON TABLE athlete_profiles IS 'Tabla de perfiles antropométricos de deportistas (peso, talla, envergadura).';
COMMENT ON TABLE sports IS 'Tabla maestra de deportes disponibles en el sistema.';
COMMENT ON TABLE disciplines IS 'Tabla de disciplinas dentro de cada deporte.';
COMMENT ON TABLE athlete_sports IS 'Tabla intermedia para relación muchos-a-muchos entre deportistas y deportes.';

COMMENT ON COLUMN athletes.id IS 'Identificador único autoincremental del deportista.';
COMMENT ON COLUMN athletes.username IS 'Nombre de usuario único para autenticación.';
COMMENT ON COLUMN athletes.email IS 'Correo electrónico único del deportista.';
COMMENT ON COLUMN athletes.first_name IS 'Nombre del deportista.';
COMMENT ON COLUMN athletes.last_name IS 'Apellido del deportista.';
COMMENT ON COLUMN athletes.birth_date IS 'Fecha de nacimiento del deportista.';
COMMENT ON COLUMN athletes.photo_url IS 'URL de referencia de la foto de perfil del deportista.';
COMMENT ON COLUMN athletes.created_at IS 'Fecha y hora de creación del deportista.';
COMMENT ON COLUMN athletes.updated_at IS 'Fecha y hora de la última actualización.';
COMMENT ON COLUMN athletes.deleted_at IS 'Fecha de eliminación lógica (soft delete). NULL = activo.';

COMMENT ON COLUMN athlete_profiles.id IS 'Identificador único autoincremental del perfil.';
COMMENT ON COLUMN athlete_profiles.athlete_id IS 'Identificador del atleta asociado (clave foránea a athletes).';
COMMENT ON COLUMN athlete_profiles.weight_kgs IS 'Peso en kilogramos (rango recomendado: 1-500 kg).';
COMMENT ON COLUMN athlete_profiles.height_cm IS 'Talla en centímetros (rango recomendado: 10-300 cm).';
COMMENT ON COLUMN athlete_profiles.arm_span_cm IS 'Envergadura en centímetros (rango recomendado: 10-300 cm).';
COMMENT ON COLUMN athlete_profiles.notes IS 'Notas adicionales sobre el perfil antropométrico.';
COMMENT ON COLUMN athlete_profiles.created_at IS 'Fecha y hora de creación del perfil.';
COMMENT ON COLUMN athlete_profiles.updated_at IS 'Fecha y hora de la última actualización.';
COMMENT ON COLUMN athlete_profiles.deleted_at IS 'Fecha de eliminación lógica (soft delete). NULL = activo.';

COMMENT ON COLUMN sports.id IS 'Identificador único autoincremental del deporte.';
COMMENT ON COLUMN sports.name IS 'Nombre único del deporte. Valores permitidos: Natación, Atletismo, Ciclismo, Nado Sincronizado, etc.';
COMMENT ON COLUMN sports.description IS 'Descripción detallada del deporte.';
COMMENT ON COLUMN sports.created_at IS 'Fecha y hora de creación del deporte.';
COMMENT ON COLUMN sports.updated_at IS 'Fecha y hora de la última actualización.';
COMMENT ON COLUMN sports.deleted_at IS 'Fecha de eliminación lógica (soft delete). NULL = activo.';

COMMENT ON COLUMN disciplines.id IS 'Identificador único autoincremental de la disciplina.';
COMMENT ON COLUMN disciplines.sport_id IS 'Identificador del deporte padre (clave foránea a sports).';
COMMENT ON COLUMN disciplines.name IS 'Nombre de la disciplina (ej: Estilo Libre, Espalda, Pecho, Mariposa).';
COMMENT ON COLUMN disciplines.description IS 'Descripción detallada de la disciplina.';
COMMENT ON COLUMN disciplines.created_at IS 'Fecha y hora de creación de la disciplina.';
COMMENT ON COLUMN disciplines.updated_at IS 'Fecha y hora de la última actualización.';
COMMENT ON COLUMN disciplines.deleted_at IS 'Fecha de eliminación lógica (soft delete). NULL = activo.';

COMMENT ON COLUMN athlete_sports.athlete_id IS 'Identificador del deportista asociado (clave foránea a athletes).';
COMMENT ON COLUMN athlete_sports.sport_id IS 'Identificador del deporte asociado (clave foránea a sports).';

-- ============================================================================
-- NOTAS PARA FUTURAS MIGRACIONES
-- ============================================================================
-- Próximos módulos a implementar:
--
-- MÓDULO DE ENTRENAMIENTOS (training):
--   - V3__training_schema.sql: training_plans, training_cycles, training_sessions, attendance
--
-- MÓDULO DE CHEQUEOS (checkup):
--   - V4__checkup_schema.sql: checkups, checkup_times, national_reference_times, medal_projections
--
-- MÓDULO DE REPORTES (report):
--   - V5__reports_schema.sql: reports, report_schedules, report_exports
-- ============================================================================
