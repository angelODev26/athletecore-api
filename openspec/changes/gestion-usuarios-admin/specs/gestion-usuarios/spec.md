# specs/gestion-usuarios/spec.md (delta)

## ADDED Requirements

### Requirement: Registro público cerrado
El sistema SHALL exigir `ROLE_ADMIN` para `POST /api/v1/users`; los únicos
endpoints públicos MUST ser `POST /api/v1/auth/login` y
`POST /api/v1/auth/refresh`. Un usuario anónimo que intente crear usuarios MUST
recibir `401` con el `ErrorResponse` de autenticación requerida y un usuario
autenticado sin `ROLE_ADMIN` MUST recibir `403` con el `ErrorResponse` de acceso
denegado. La restricción SHALL aplicarse tanto en la configuración de seguridad
(regla de URL) como en el controller (`@PreAuthorize`).

#### Scenario: Anónimo intenta crear un usuario
- **WHEN** se invoca `POST /api/v1/users` sin cabecera `Authorization`
- **THEN** la respuesta es `401` con `ErrorResponse` `"Autenticación requerida"` y
  no se crea ningún usuario

#### Scenario: Usuario sin rol ADMIN intenta crear
- **WHEN** un usuario autenticado con `ROLE_USER` o `ROLE_COACH` invoca
  `POST /api/v1/users`
- **THEN** la respuesta es `403` con `ErrorResponse` `"Acceso denegado"` y no se
  crea ningún usuario

#### Scenario: ADMIN crea un usuario
- **WHEN** un usuario con `ROLE_ADMIN` invoca `POST /api/v1/users` con un body
  válido
- **THEN** la respuesta es `201` con el `UserResponse` del usuario creado

#### Scenario: Login y refresh siguen siendo públicos
- **WHEN** se invocan `POST /api/v1/auth/login` o `POST /api/v1/auth/refresh` sin
  token
- **THEN** la petición se procesa sin exigir autenticación previa

### Requirement: Creación de usuarios con roles
`POST /api/v1/users` SHALL aceptar un campo opcional `roles` (lista de strings).
Los valores aceptados MUST ser `ADMIN`, `COACH`, `USER` y sus formas con prefijo
`ROLE_ADMIN`, `ROLE_COACH`, `ROLE_USER`, sin distinguir mayúsculas/minúsculas; el
sistema SHALL normalizarlos a `ROLE_*` antes de persistirlos. Un valor fuera de
ese catálogo MUST rechazarse con `400` (`ValidationException`) y sin crear el
usuario. Los duplicados MUST deduplicarse. Si `roles` está ausente, es `null` o
viene vacío, el usuario SHALL crearse con `ROLE_USER`. El resto de validaciones
del DTO (username, email, nombres, password, confirmación) y el `409` de
username/email duplicado SHALL mantenerse sin cambios.

#### Scenario: ADMIN crea un COACH
- **WHEN** un ADMIN envía `roles: ["COACH"]`
- **THEN** el usuario se crea con el rol `ROLE_COACH` y la respuesta `201` lo
  incluye en `roles`

#### Scenario: Alias con y sin prefijo
- **WHEN** un ADMIN envía `roles: ["ADMIN", "ROLE_ADMIN", "coach"]`
- **THEN** el usuario se crea con `ROLE_ADMIN` y `ROLE_COACH`, sin duplicados y
  en orden alfabético en la respuesta

#### Scenario: Rol fuera del catálogo
- **WHEN** un ADMIN envía `roles: ["SUPERUSER"]`
- **THEN** la respuesta es `400` con un mensaje que identifica el rol inválido y
  el usuario no se crea

#### Scenario: Sin roles se aplica el default
- **WHEN** un ADMIN crea un usuario sin el campo `roles`
- **THEN** el usuario se crea con `ROLE_USER`

#### Scenario: Duplicados de username o email
- **WHEN** un ADMIN intenta crear un usuario con username o email ya existente
- **THEN** la respuesta es `409` con el mensaje de duplicado de siempre
  (`"User with username already exists"` / `"User with email already exists"`) y
  no se crea el usuario

#### Scenario: Contraseñas que no coinciden
- **WHEN** un ADMIN envía `password` y `confirmPassword` distintos
- **THEN** la respuesta es `400` y no se crea el usuario

### Requirement: Roles en las respuestas de usuario
`UserResponse` SHALL incluir `roles` como lista de nombres `ROLE_*`, ordenada
alfabéticamente, en todas las respuestas que lo devuelven: `201` de creación,
`GET /api/v1/users`, `user` del login y `user` del refresh. Los roles SHALL
coincidir con los que viajan en el claim `roles` del JWT y MUST NOT exponer
password ni datos internos de JPA. El mapeo SHALL realizarse dentro de la
transacción que carga al usuario para no provocar `LazyInitializationException`.

#### Scenario: Login devuelve los roles
- **WHEN** un usuario con `ROLE_ADMIN` inicia sesión
- **THEN** `user.roles` incluye `"ROLE_ADMIN"` y el access token lleva el mismo
  rol en su claim `roles`

#### Scenario: Listado con roles
- **WHEN** un ADMIN consulta `GET /api/v1/users`
- **THEN** cada elemento incluye `roles` con los nombres `ROLE_*` del usuario

#### Scenario: Creación devuelve los roles asignados
- **WHEN** un ADMIN crea un usuario con `roles: ["ADMIN", "COACH"]`
- **THEN** el `201` devuelve `roles: ["ROLE_ADMIN", "ROLE_COACH"]`

### Requirement: Primer administrador sembrado por Flyway
El sistema SHALL sembrar un usuario con `ROLE_ADMIN` mediante una migración
Flyway dedicada, con `username`, `email` y hash BCrypt tomados de variables de
entorno (`ADMIN_USERNAME`, `ADMIN_EMAIL`, `ADMIN_PASSWORD_HASH`). La aplicación
MUST NOT arrancar si falta el hash o si no es un hash BCrypt válido. La migración
SHALL ser idempotente y no intrusiva: si ya existe un usuario con ese username,
MUST NOT duplicarlo ni sobrescribir su password, su estado `enabled` ni sus
roles; SHALL añadir el vínculo con `ROLE_ADMIN` solo si la cuenta existente
demuestra ser la sembrada (misma password que el hash configurado, `enabled =
true` y sin soft delete). En cualquier otro caso MUST abortar con un error
accionable, sin promover la cuenta. Si el email ya está en uso por otro usuario,
MUST abortar. La contraseña MUST almacenarse como hash BCrypt, nunca en texto
plano.

#### Scenario: Entorno nuevo arranca con admin usable
- **WHEN** se despliega sobre una base sin usuarios con las tres variables de
  entorno definidas y un hash BCrypt válido
- **THEN** existe un usuario con `ROLE_ADMIN`, `enabled = true`, y puede iniciar
  sesión y operar `GET /users` y `POST /users`

#### Scenario: Falta el hash del admin
- **WHEN** se arranca la aplicación sin `ADMIN_PASSWORD_HASH`
- **THEN** el arranque falla por placeholder de Flyway no resuelto y no se aplica
  la migración

#### Scenario: Hash inválido o vacío
- **WHEN** `ADMIN_PASSWORD_HASH` está definido pero vacío o no es un BCrypt válido
- **THEN** la migración aborta con un error accionable y no crea ningún usuario

#### Scenario: Usuario preexistente no confiable
- **WHEN** la migración se aplica sobre una base donde ya existe un usuario con el
  username configurado pero distinta password, deshabilitado o con soft delete
- **THEN** la migración aborta, no concede `ROLE_ADMIN` y no modifica la cuenta

#### Scenario: Cuenta sembrada sin el vínculo de rol
- **WHEN** el usuario configurado existe con la misma password del seed, está
  habilitado, sin soft delete y no tiene `ROLE_ADMIN`
- **THEN** la migración añade únicamente el vínculo `user_roles` con `ROLE_ADMIN`
  sin tocar el resto de roles

#### Scenario: Email del seed ya en uso
- **WHEN** el username configurado no existe pero `ADMIN_EMAIL` pertenece a otro
  usuario
- **THEN** la migración aborta con un mensaje que pide configurar otro email
