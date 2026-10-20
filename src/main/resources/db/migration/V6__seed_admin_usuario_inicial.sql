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
-- La migración es idempotente y no intromete:
--   * si ya existe un usuario con ese username (incluido uno con soft delete),
--     no lo duplica ni sobrescribe su password, estado o roles;
--   * solo crea el vínculo con ROLE_ADMIN si falta.
-- ============================================================================

INSERT INTO users (username, email, password, first_name, last_name, enabled,
                   created_at, updated_at)
VALUES ('${admin_username}', '${admin_email}', '${admin_password_hash}',
        'Administrador', 'Inicial', TRUE,
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (username) DO NOTHING;

-- Asignar ROLE_ADMIN al usuario configurado solo si el vínculo no existe.
INSERT INTO user_roles (user_id, role_id, created_at, updated_at)
SELECT u.id, r.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM users u
JOIN roles r ON r.name = 'ROLE_ADMIN'
WHERE u.username = '${admin_username}'
  AND NOT EXISTS (
      SELECT 1
      FROM user_roles ur
      WHERE ur.user_id = u.id
        AND ur.role_id = r.id
  );
