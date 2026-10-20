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
--   * Si el username ya existe, solo se le añade ROLE_ADMIN cuando su password
--     coincide con el hash configurado y la cuenta está habilitada y sin soft
--     delete. En cualquier otro caso falla con un mensaje accionable.
--   * Si el email ya está en uso por otro usuario, falla (UNIQUE global).
--   * El hash debe ser BCrypt válido (60 chars, $2a/$2b/$2y); un valor vacío o
--     en claro aborta la migración.
--
-- La migración es idempotente en el sentido de que no duplica filas ni
-- sobrescribe la password/roles de la cuenta sembrada.
-- ============================================================================

DO $$
DECLARE
    existing_user  users%ROWTYPE;
    new_user_id    BIGINT;
    admin_role_id  BIGINT;
BEGIN
    IF btrim('${admin_username}') = '' THEN
        RAISE EXCEPTION 'ADMIN_USERNAME no puede estar vacío';
    END IF;

    IF btrim('${admin_email}') = '' THEN
        RAISE EXCEPTION 'ADMIN_EMAIL no puede estar vacío';
    END IF;

    -- Validar formato BCrypt antes de tocar datos: un hash vacío o en claro
    -- dejaría un admin inutilizable o una credencial expuesta.
    IF '${admin_password_hash}' !~ '^[$]2[aby][$][0-9]{2}[$].{53}$' THEN
        RAISE EXCEPTION 'ADMIN_PASSWORD_HASH debe ser un hash BCrypt válido de 60 caracteres ($2a/$2b/$2y)';
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
    WHERE username = '${admin_username}';

    IF FOUND THEN
        -- Solo se puede promover una cuenta que demuestre ser la sembrada:
        -- misma password, habilitada y no borrada. Así el seed no convierte en
        -- ADMIN una cuenta creada por el antiguo registro público.
        IF existing_user.password <> '${admin_password_hash}' THEN
            RAISE EXCEPTION
                'El username % ya existe con otra contraseña; no se promueve a ADMIN. Configure otro ADMIN_USERNAME o promueva la cuenta manualmente tras auditarla.',
                '${admin_username}';
        END IF;

        IF existing_user.deleted_at IS NOT NULL THEN
            RAISE EXCEPTION
                'El username % existe con soft delete; restaure la cuenta o configure otro ADMIN_USERNAME',
                '${admin_username}';
        END IF;

        IF NOT existing_user.enabled THEN
            RAISE EXCEPTION
                'El username % existe pero está deshabilitado; habilítelo o configure otro ADMIN_USERNAME',
                '${admin_username}';
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
    IF EXISTS (SELECT 1 FROM users WHERE email = '${admin_email}') THEN
        RAISE EXCEPTION
            'ADMIN_EMAIL % ya está en uso por otro usuario; configure otro email',
            '${admin_email}';
    END IF;

    INSERT INTO users (username, email, password, first_name, last_name, enabled,
                       created_at, updated_at)
    VALUES ('${admin_username}', '${admin_email}', '${admin_password_hash}',
            'Administrador', 'Inicial', TRUE,
            CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
    RETURNING id INTO new_user_id;

    INSERT INTO user_roles (user_id, role_id, created_at, updated_at)
    VALUES (new_user_id, admin_role_id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
END $$;
