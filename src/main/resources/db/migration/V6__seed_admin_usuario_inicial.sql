-- ============================================================================
-- SEED: Administrador inicial
-- ============================================================================
-- Versión: V6
-- Descripción: Siembra el primer usuario ADMIN del sistema para que un entorno
--              nuevo tenga una vía de administración tras cerrar el registro
--              público (POST /api/v1/users pasa a ser solo ADMIN).
--
-- Credenciales: se toman de placeholders de Flyway alimentados por variables de
-- entorno (ADMIN_USERNAME, ADMIN_EMAIL, ADMIN_PASSWORD_HASH):
--   spring.flyway.placeholders.admin_username
--   spring.flyway.placeholders.admin_email
--   spring.flyway.placeholders.admin_password_hash  (BCrypt, obligatorio)
--
-- Seguridad: la migración NUNCA promueve una cuenta preexistente no confiable.
--   * Si el username ya existe, solo se le añade ROLE_ADMIN cuando su hash de
--     password es EXACTAMENTE el configurado, la cuenta está habilitada y sin
--     soft delete. En cualquier otro caso falla con un mensaje accionable.
--   * Si el email ya está en uso por otro usuario, falla (UNIQUE global).
--   * El hash debe ser BCrypt con coste 04-31 (60 chars, $2a/$2b/$2y) y el
--     charset base64 de BCrypt; un valor vacío o en claro aborta la migración.
--
-- Los valores de entorno se interpolan en literales SQL. Los valores de
-- ADMIN_USERNAME y ADMIN_EMAIL no deben contener comilla simple (') — el
-- apóstrofo es válido en un email RFC pero rompería el script antes de poder
-- dar un mensaje accionable. Los valores se normalizan con btrim y se usan vía
-- variables PL/pgSQL (nunca se reinterpola el placeholder).
--
-- La migración no duplica filas ni sobrescribe la password/roles de la cuenta
-- sembrada.
-- ============================================================================

DO $$
DECLARE
    seed_username  TEXT := btrim('${admin_username}');
    seed_email     TEXT := btrim('${admin_email}');
    seed_hash      TEXT := '${admin_password_hash}';
    existing_user  users%ROWTYPE;
    new_user_id    BIGINT;
    admin_role_id  BIGINT;
BEGIN
    IF seed_username = '' THEN
        RAISE EXCEPTION 'ADMIN_USERNAME no puede estar vacío';
    END IF;

    IF seed_email = '' THEN
        RAISE EXCEPTION 'ADMIN_EMAIL no puede estar vacío';
    END IF;

    -- Validar formato BCrypt antes de tocar datos: un hash vacío, en claro o
    -- malformado dejaría un admin inutilizable (coste/rounds fuera de rango o
    -- charset inválido) y el fallo se descubriría en el login, no al desplegar.
    IF seed_hash !~ '^[$]2[aby][$](0[4-9]|[12][0-9]|3[01])[$][./0-9A-Za-z]{53}$' THEN
        RAISE EXCEPTION 'ADMIN_PASSWORD_HASH debe ser un hash BCrypt válido (60 caracteres, coste 04-31, $2a/$2b/$2y)';
    END IF;

    SELECT id INTO admin_role_id
    FROM roles
    WHERE name = 'ROLE_ADMIN'
      AND deleted_at IS NULL;

    IF admin_role_id IS NULL THEN
        RAISE EXCEPTION 'No existe un ROLE_ADMIN activo; revise las migraciones V2/V3';
    END IF;

    SELECT * INTO existing_user
    FROM users
    WHERE username = seed_username;

    IF FOUND THEN
        -- Solo se puede promover una cuenta que demuestre ser la sembrada.
        -- El orden de los chequeos importa para que el mensaje sea accionable.
        IF existing_user.deleted_at IS NOT NULL THEN
            RAISE EXCEPTION
                'El username % existe con soft delete; restaure la cuenta o configure otro ADMIN_USERNAME',
                seed_username;
        END IF;

        IF NOT existing_user.enabled THEN
            RAISE EXCEPTION
                'El username % existe pero está deshabilitado; habilítelo o configure otro ADMIN_USERNAME',
                seed_username;
        END IF;

        -- Mismo hash BCrypt exacto: por el salting, la misma contraseña produce
        -- hashes distintos; exigir el hash es la única prueba verificable sin
        -- pgcrypto.
        IF existing_user.password <> seed_hash THEN
            RAISE EXCEPTION
                'El username % ya existe con otro hash de contraseña; no se promueve a ADMIN. Configure otro ADMIN_USERNAME o promueva la cuenta manualmente tras auditarla.',
                seed_username;
        END IF;

        INSERT INTO user_roles (user_id, role_id, created_at, updated_at)
        SELECT existing_user.id, admin_role_id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
        WHERE NOT EXISTS (
            SELECT 1
            FROM user_roles ur
            WHERE ur.user_id = existing_user.id
              AND ur.role_id = admin_role_id
        );

        RETURN;
    END IF;

    -- El email es UNIQUE global: comprobar antes de insertar para dar un error
    -- accionable en lugar de una violación de constraint críptica.
    IF EXISTS (SELECT 1 FROM users WHERE email = seed_email) THEN
        RAISE EXCEPTION
            'ADMIN_EMAIL % ya está en uso por otro usuario; configure otro email',
            seed_email;
    END IF;

    INSERT INTO users (username, email, password, first_name, last_name, enabled,
                       created_at, updated_at)
    VALUES (seed_username, seed_email, seed_hash,
            'Administrador', 'Inicial', TRUE,
            CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
    RETURNING id INTO new_user_id;

    INSERT INTO user_roles (user_id, role_id, created_at, updated_at)
    VALUES (new_user_id, admin_role_id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
END $$;
