-- ============================================================================
-- MIGRACIÓN INICIAL: Esquema base de AthleteCore API
-- ============================================================================
-- Versión: V1
-- Descrição: Crea el esquema inicial con tablas de roles, usuarios y entidades
--            base extendiendo con auditoría y soft delete.
-- Dependencias: Ninguna (primera migración)
-- ============================================================================

-- ============================================================================
-- TABLA: roles
-- Descripción: Tabla maestra de roles para el sistema de RBAC (Role-Based Access Control)
--              Los roles definen permisos y asignaciones de usuarios.
--              Extiende BaseEntity con campos created_at, updated_at, deleted_at.
-- ============================================================================
CREATE TABLE roles (
    id              BIGSERIAL PRIMARY KEY,           -- Identificador único autoincremental
    name            VARCHAR(50) NOT NULL UNIQUE,     -- Nombre del rol (e.g., ADMIN, USER, TRAINER)

    -- Campos de auditoría (heredados de BaseEntity)
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at      TIMESTAMP WITH TIME ZONE          -- Soft delete timestamp
);

-- Índice único para nombre de rol
CREATE UNIQUE INDEX idx_roles_name ON roles(name) WHERE deleted_at IS NULL;

-- ============================================================================
-- TABLA: users
-- Descripción: Tabla principal de usuarios del sistema.
--              Contiene información de autenticación y perfil del usuario.
--              Extiende BaseEntity con campos created_at, updated_at, deleted_at.
-- ============================================================================
CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,                    -- Identificador único autoincremental
    username        VARCHAR(255) NOT NULL UNIQUE,             -- Nombre de usuario único
    email           VARCHAR(255) NOT NULL UNIQUE,             -- Correo electrónico único
    password        VARCHAR(255) NOT NULL,                     -- Contraseña hasheada (BCrypt)
    first_name      VARCHAR(100) NOT NULL,                    -- Nombre
    last_name       VARCHAR(100) NOT NULL,                    -- Apellido
    enabled         BOOLEAN NOT NULL DEFAULT TRUE,            -- Estado del usuario (activo/inactivo)

    -- Campos de auditoría (heredados de BaseEntity)
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at      TIMESTAMP WITH TIME ZONE                   -- Soft delete timestamp
);

-- Índices para búsqueda eficiente de usuarios
CREATE INDEX idx_users_username ON users(username) WHERE deleted_at IS NULL;
CREATE INDEX idx_users_email ON users(email) WHERE deleted_at IS NULL;
CREATE INDEX idx_users_enabled ON users(enabled) WHERE deleted_at IS NULL;
CREATE INDEX idx_users_fullname ON users(last_name, first_name) WHERE deleted_at IS NULL;

-- ============================================================================
-- TABLA: user_roles (Tabla intermedia Many-to-Many)
-- Descripción: Asocia usuarios con roles (relación muchos-a-muchos).
--              Permite que un usuario tenga múltiples roles y que un rol
--              sea asignado a múltiples usuarios.
-- ============================================================================
CREATE TABLE user_roles (
    user_id         BIGINT NOT NULL,
    role_id         BIGINT NOT NULL,

    -- Campo para auditoría de la asignación
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (user_id, role_id),

    -- Foreign Keys con referencias en cascada
    CONSTRAINT fk_user_roles_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role
        FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);

-- Índices para consultas frecuentes
CREATE INDEX idx_user_roles_user_id ON user_roles(user_id);
CREATE INDEX idx_user_roles_role_id ON user_roles(role_id);

-- ============================================================================
-- VISTAS / FUNCIONES DE AYUDA (Opcional pero recomendadas)
-- ============================================================================

-- Función para actualizar automáticamente el field updated_at
-- NOTA: Esta función se mantiene por compatibilidad, pero Hibernate @LastModifiedDate
-- también actualiza estos campos automáticamente. Se recomienda usar solo Hibernate.
-- CREATE OR REPLACE FUNCTION update_updated_at_column()
-- RETURNS TRIGGER AS $$
-- BEGIN
--     NEW.updated_at = CURRENT_TIMESTAMP;
--     RETURN NEW;
-- END;
-- $$ language 'plpgsql';

-- ============================================================================
-- NOTAS PARA FUTURAS MIGRACIONES
-- ============================================================================
-- Próximas tablas a implementar (según roadmaps de módulos):
--
-- MÓDULO DEPORTISTAS (athlete):
--   - athletes (información básica del deportista)
--   - athlete_profiles (datos antropométricos)
--   - athlete_photos (fotos de perfil)
--
-- MÓDULO DE ENTRENAMIENTOS (training):
--   - training_sessions (sesiones de entrenamiento individuales)
--   - training_plans (planes de entrenamiento)
--   - training_cycles (ciclos: micro, meso, anual)
--
-- MÓDULO DE CHEQUEOS (checkup):
--   - checkups (registros de chequeos mensuales)
--   - checkup_times (tiempos de prueba registrados)
--   - reference_tables (tablas de referencia nacionales)
--
-- MÓDULO DE REPORTES (report):
--   - reports (generación de reportes)
--   - report_schedules (programación de reportes)
-- ============================================================================
