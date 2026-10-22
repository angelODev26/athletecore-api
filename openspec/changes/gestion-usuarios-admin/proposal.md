# Propuesta: gestion-usuarios-admin

## Why

Hoy la única vía de alta es el **registro público** (`POST /api/v1/users`,
`SecurityConfig.java:44`) y siempre crea `ROLE_USER` hardcodeado
(`UserService.java:58-61`). No hay forma de crear un COACH, un segundo ADMIN ni de
sembrar el primer administrador de un entorno nuevo: las migraciones `V2`/`V3`
solo insertan los roles, ningún usuario. Además `UserResponse` no expone los roles
(`UserResponse.java:8-15`), así que ninguna pantalla puede mostrar el rol de un
usuario.

Esto bloquea la fase B del change de frontend `alta-usuarios-admin`
(`../athletecore-web`), cuyos prerrequisitos **D7** son exactamente P1/P2/P3:
cerrar el registro público, exponer la creación con roles y sembrar el admin
inicial. El modelo de producto acordado es: **el registro queda cerrado; solo un
ADMIN crea usuarios y les asigna el rol**; el primer admin nace de un seed.

Base de referencia en el repo: `docs/frontend-handoff/03-contrato-api-usuarios.md`
§1/§3, `docs/frontend-handoff/09-decisiones-deuda-y-pendientes.md` (T7/T8, decisión
de arquitectura 4) y `TASKS.md` (User/Security).

## What Changes

### P1 — Cierre del registro público

- `POST /api/v1/users` deja de ser público: pasa a requerir `ROLE_ADMIN` en
  `SecurityConfig` **y** `@PreAuthorize("hasRole('ADMIN')")` en el controller
  (defensa en profundidad).
- Endpoints públicos del sistema: solo `POST /api/v1/auth/login` y
  `POST /api/v1/auth/refresh`.
- Anónimo → **401** `ErrorResponse` "Autenticación requerida"; autenticado sin
  ADMIN → **403** `ErrorResponse` "Acceso denegado" (handlers ya existentes).

### P2 — Creación de usuarios con roles y roles en la respuesta

- `CreateUserRequest` gana `roles` (lista opcional). Acepta `ADMIN`, `COACH`,
  `USER` y sus formas `ROLE_ADMIN`/`ROLE_COACH`/`ROLE_USER`
  (case-insensitive), normaliza a `ROLE_*` y valida contra el catálogo soportado
  (los tres roles que ya existen en BD). Rol desconocido → **400**
  `ValidationException` con mensaje claro; duplicados se deduplican. Sin `roles`
  → `ROLE_USER` (compatibilidad con el contrato actual y con los tests).
- `UserService.createUser` deja de hardcodear `ROLE_USER` y resuelve los roles
  pedidos contra `RoleRepository`.
- `UserResponse` gana `roles: List<String>` con los nombres `ROLE_*` (los mismos
  que viajan en el claim `roles` del JWT). Se devuelve en el login, el refresh,
  `GET /users` y el 201 de creación.

### P3 — Seed Flyway del admin inicial

- Migración `V6__seed_admin_usuario_inicial.sql` que siembra **un** usuario ADMIN
  con `username`, `email` y **hash BCrypt** tomados de placeholders de Flyway
  alimentados por variables de entorno (`ADMIN_USERNAME`, `ADMIN_EMAIL`,
  `ADMIN_PASSWORD_HASH`), y le asigna `ROLE_ADMIN`.
- Idempotente y fail-closed: si el usuario ya existe solo se añade el vínculo
  `ROLE_ADMIN` cuando el hash almacenado es exactamente el configurado, la cuenta
  está habilitada y sin soft delete; en cualquier otro caso la migración aborta
  sin promover la cuenta. No sobrescribe password/estado/roles.
- Fail-fast: sin `ADMIN_PASSWORD_HASH` (o con uno vacío/malformado) la aplicación
  no arranca. En `application-local.properties.example` (versionado) hay un hash
  de desarrollo documentado.
- Runbook de generación del hash y rotación de la contraseña en `README.md` /
  `.env.example`.

### Documentación

- `docs/frontend-handoff/03-contrato-api-usuarios.md`: `POST /users` deja de ser
  público, request con `roles`, `UserResponse` con `roles`, tabla de endpoints y
  flujo de alta.
- `docs/frontend-handoff/02-autenticacion-y-seguridad.md` §endpoints públicos y
  política de alta; `00-vision-general.md` (decisión "un solo endpoint público");
  `01-infraestructura-y-setup.md` (quitar "no hay admin seedeado").
- `docs/frontend-handoff/09-decisiones-deuda-y-pendientes.md`: T7 resuelta,
  T8 actualizada (sigue sin `PUT`/`DELETE`/`/me`) y decisión de arquitectura 4
  actualizada.
- `AGENTS.md` (línea del endpoint público), `TASKS.md` (User/Security, roadmap) y
  `docs/flyway.md` (placeholders y seed).

## Capabilities

### New Capabilities

- `gestion-usuarios`: cierre del registro público, creación de usuarios con roles
  por un ADMIN, exposición de roles en `UserResponse` y seed Flyway del
  administrador inicial.

### Modified Capabilities

- Ninguna (`openspec/specs/` solo contiene `token-refresh`; el contrato de login
  y refresh no cambia más allá del campo aditivo `roles` dentro de `user`).

## Impact

### Archivos afectados (estimación)

- **Modificados**: `config/SecurityConfig.java`, `user/UserController.java`,
  `user/UserService.java`, `user/dto/CreateUserRequest.java`,
  `user/dto/UserResponse.java`, `src/main/resources/application.properties`,
  `src/main/resources/application-local.properties` (gitignored),
  `.env.example`, `README.md`, `AGENTS.md`, `TASKS.md`,
  `docs/frontend-handoff/{00,01,02,03,09}.md`, `docs/flyway.md`.
- **Nuevos**: `src/main/resources/db/migration/V6__seed_admin_usuario_inicial.sql`;
  tests unitarios en `src/test/java/com/athletecore/api/user/`.

### Contrato

```
POST /api/v1/users   # MODIFICADO (BREAKING en seguridad): ahora SOLO ADMIN
                     # body: { username, email, firstName, lastName, password,
                     #         confirmPassword, roles?: ["ADMIN"|"COACH"|"USER"|"ROLE_*"] }
                     # 201: UserResponse (ahora con roles[])
                     # 400: validación / rol inválido; 409: username/email duplicado
                     # 401: sin token; 403: sin ROLE_ADMIN

GET  /api/v1/users   # MODIFICADO (aditivo): cada UserResponse incluye roles[]
POST /api/v1/auth/login | /auth/refresh  # MODIFICADO (aditivo): user.roles[]
```

### BREAKING / coordinación con el frontend

- `POST /api/v1/users` deja de ser público: el front debe retirar `/registro`
  (change `alta-usuarios-admin`). Si el back se despliega antes, `/registro`
  queda roto a propósito hasta el corte del front — estado ya aceptado y
  documentado en el design de ese change. **Recomendación: desplegar ambos en el
  mismo release.**
- `roles` en `UserResponse` es aditivo: el front actual lo ignora sin romperse.

### Fuera de alcance

- `PUT /users/{id}` (toggle `enabled`, reset de password), `DELETE`, `GET
  /users/{id}`, `/me`, logout.
- Paginación server-side de `GET /users` (el front pagina en cliente, decisión D2
  del front; deuda T9).
- Revocación de refresh tokens con estado, audit log de altas, UI de usuarios.

### Dependencias

- Ninguna nueva: BCrypt y Flyway ya están en el proyecto.
