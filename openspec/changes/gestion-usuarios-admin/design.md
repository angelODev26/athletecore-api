# Diseño: gestion-usuarios-admin

## Contexto

Spring Boot 3.5.6, Java 21, PostgreSQL, Flyway, JWT stateless. Estado actual
verificado contra código:

- `SecurityConfig.java:44` deja público `POST /api/v1/users` junto a login y
  refresh; `GET /api/v1/users` ya es `@PreAuthorize("hasRole('ADMIN')")`
  (`UserController.java:32-36`).
- `UserService.createUser` hardcodea `ROLE_USER` (`UserService.java:58-61`).
- `CreateUserRequest` no tiene `roles`; `UserResponse` no expone roles
  (`UserResponse.java:8-15`).
- Los roles viven como filas (`ROLE_USER` en `V2:155`, `ROLE_ADMIN` en `V2:156`,
  `ROLE_COACH` en `V3:226`), no como enum Java.
- El JWT emite el claim `roles` desde las authorities
  (`UserDetailsServiceImpl` → `SimpleGrantedAuthority(role.getName())`), así que
  los nombres son `ROLE_*`; el front los normaliza quitando el prefijo
  (`src/lib/auth/jwt.ts` de `athletecore-web`).
- `UserResponse.fromUser` se invoca en `AuthService.login/refresh` (métodos
  `@Transactional(readOnly = true)`, seguros) y en `UserController.createUser`
  **fuera** de la transacción del servicio — trampa de lazy loading si se añaden
  roles sin cambiar el retorno.

Restricciones del proyecto (`AGENTS.md`, `core/principles.md`): DTOs records,
excepciones de `common/exception` centralizadas en `GlobalExceptionHandler`,
soft delete universal, migraciones versionadas con Flyway, tests de servicio con
Mockito en español, documentación en español.

## Goals / Non-Goals

**Goals:**

- Cerrar el registro público sin romper login/refresh.
- Permitir que un ADMIN cree usuarios con uno o varios roles del catálogo real.
- Exponer los roles del usuario en todas las respuestas de usuario.
- Sembrar el primer ADMIN de forma determinista y segura en entornos nuevos.
- Desbloquear la fase B del frontend (`alta-usuarios-admin`).

**Non-Goals:**

- `PUT`/`DELETE`/`GET /users/{id}`, `/me`, logout, cambio de password.
- Toggle de `enabled` y reset de password (deuda T8).
- Paginación server-side de `GET /users` (el front pagina en cliente; deuda T9).
- Revocación de refresh tokens con estado y audit log de altas.
- UI o cambios en `athletecore-web`: los hace su change `alta-usuarios-admin`.

## Decisions

### D1 — P1 por regla de URL **y** anotación de método

`SecurityConfig` quita `POST /api/v1/users` de `permitAll` y declara el path
completo `/api/v1/users` (y `/api/v1/users/**`) bajo `hasAnyRole("ADMIN")`;
además `UserController.createUser` lleva `@PreAuthorize("hasRole('ADMIN')")`. Las
dos capas son redundantes a propósito: la regla de URL protege si mañana se
añade otro método en el mismo path y la anotación documenta el permiso junto al
endpoint (patrón ya usado en `getAllUsers`).

- Públicos quedan **solo** login y refresh (`docs/frontend-handoff/02` §1).
- Anónimo → 401 (`RestAuthenticationEntryPoint`); autenticado sin ADMIN → 403
  (`RestAccessDeniedHandler`). No se toca el formato de `ErrorResponse`.

### D2 — Contrato de `roles` en la creación

`CreateUserRequest.roles: List<String>` opcional.

- **Formatos aceptados**: `ADMIN`, `COACH`, `USER`, `ROLE_ADMIN`, `ROLE_COACH`,
  `ROLE_USER`, case-insensitive; se normaliza a `ROLE_*` (mayúsculas).
- **Catálogo cerrado**: allow-list `ROLE_ADMIN`, `ROLE_COACH`, `ROLE_USER`. Un
  valor fuera de la lista → `ValidationException` (400) con mensaje
  `"Rol inválido: <valor>"` en `details`, sin persistir nada.
- **Por qué allow-list y no `RoleRepository.findByName` a secas**: aunque hoy la
  tabla solo tiene esos tres roles, aceptar cualquier fila futura convertiría el
  endpoint en una vía de auto-asignación de roles que nadie revisó. La lista se
  mantiene en una constante del módulo `user` y se amplía a conciencia.
- **Duplicados**: se deduplican preservando el orden de entrada; el orden de
  salida en `UserResponse` se ordena alfabéticamente para tests estables.
- **Sin `roles` o lista vacía** → `ROLE_USER` (compatibilidad con el contrato
  actual y con los tests existentes; el registro ya no es público, así que el
  default solo aplica a un ADMIN que omita el campo).
- La validación de "al menos un rol" no añade una regla nueva: el default cubre
  el caso vacío.
- No se permite `roles` nulo explícito distinto de ausente: `null` se trata como
  ausente.

### D3 — `roles` en `UserResponse` y el límite transaccional

`UserResponse` gana `List<String> roles`. `fromUser` los mapea desde
`user.getRoles()` y los ordena.

- **Trampa resuelta**: `UserController.createUser` hoy mapea fuera de la
  transacción del servicio. Con `roles` lazy (ManyToMany) eso lanzaría
  `LazyInitializationException`. Decisión: `UserService.createUser` devuelve
  `UserResponse` (mapeo **dentro** de `@Transactional`) y el controller lo
  retorna tal cual. `getAllUsers` y `AuthService` ya mapean dentro de
  transacción.
- Los nombres expuestos son los persistidos (`ROLE_*`), iguales a los del claim
  `roles` del JWT; el front los normaliza. No se exponen roles en minúsculas para
  no crear dos representaciones.
- Cambio **aditivo** en el JSON: los clientes actuales que ignoran el campo no se
  rompen.

### D4 — P3 con Flyway + placeholders (hash, no contraseña en claro)

Migración SQL `V6__seed_admin_usuario_inicial.sql` con un bloque PL/pgSQL que:

1. valida username/email no vacíos (normalizados con `btrim`) y un hash BCrypt
   con coste 04-31 y charset base64;
2. si el username no existe, crea el usuario y el vínculo `user_roles` con
   `ROLE_ADMIN`;
3. si el username ya existe, **solo** añade el vínculo cuando el hash almacenado
   es exactamente el configurado, la cuenta está `enabled` y sin soft delete; en
   cualquier otro caso aborta con un mensaje accionable.

El SQL real es la fuente de verdad; no se duplica aquí para evitar deriva.

- **Placeholders** (`spring.flyway.placeholders.*`) alimentados por entorno:
  `ADMIN_USERNAME`, `ADMIN_EMAIL`, `ADMIN_PASSWORD_HASH`.
- **Fail-fast**: en `application.properties` el hash se mapea sin valor por
  defecto (`${ADMIN_PASSWORD_HASH}`); si falta, Spring no arranca (placeholder no
  resuelto). Es deliberado: un entorno sin admin usable tras cerrar el registro
  queda bloqueado, y es mejor fallar al desplegar que descubrirlo después.
- **Dev**: `application-local.properties` está gitignored; el ejemplo versionado
  `application-local.properties.example` trae el hash BCrypt de una contraseña de
  desarrollo documentada y se copia/mezcla en el archivo local. El `.env.example`
  explica cómo generar uno (`htpasswd -bnBC 12 "" 'mi-password'`) y que en
  Docker Compose el hash va entre comillas simples por el `$`.
- **Por qué hash y no contraseña en claro**: nunca se manipula la contraseña en
  texto plano en la migración ni en la BD; el operador genera el hash una vez.
- **Por qué no `pgcrypto`**: la extensión no está garantizada en todos los
  entornos y añadirla sería una dependencia de infraestructura nueva.
- **Por qué no un `CommandLineRunner`**: el proyecto ya tiene el patrón de seed
  en Flyway (`V2`/`V3` siembran roles y deportes) y RNF-04 exige Flyway; un
  initializer en Java duplicaría responsabilidades y no dejaría rastro en
  `flyway_schema_history`.
- **Validación del hash**: la migración exige un BCrypt válido (60 chars, coste
  04-31, `$2a/$2b/$2y`, charset base64 de BCrypt) y username/email no vacíos
  antes de tocar datos. Un `ADMIN_PASSWORD_HASH` vacío, en claro o malformado
  aborta. `ADMIN_USERNAME`/`ADMIN_EMAIL` no deben contener comilla simple: se
  interpolan en literales SQL.
- **Idempotencia y no intromisión**: si el username ya existe, la migración
  **solo** añade `ROLE_ADMIN` cuando la cuenta demuestra ser la sembrada (mismo
  hash BCrypt exacto que el configurado —el salting hace que la misma contraseña
  produzca hashes distintos—, `enabled = true` y sin soft delete). Si no, aborta
  con un error accionable: nunca promueve una cuenta preexistente creada por el
  antiguo registro público. Tampoco sobrescribe password, `enabled` ni roles
  existentes. Un email ya en uso por otro usuario aborta (UNIQUE global).
- **Tests**: `AthletecoreApiApplicationTests` recibe los placeholders por
  `@SpringBootTest(properties = ...)` para que el contexto arranque sin depender
  de una env externa. Usa una identidad propia (`context-admin`) con un hash de
  un valor no documentado: no siembra credenciales conocidas en la BD de test ni
  colisiona con un `admin` real con otro hash.

### D5 — Coordinación con el frontend y orden de despliegue

- El cambio es **breaking en seguridad**: `POST /users` deja de ser público.
- El front retira `/registro` en su change `alta-usuarios-admin` (fase B). Si el
  back se despliega antes, `/registro` falla con 401/403 **a propósito** hasta el
  corte del front — estado ya aceptado en el design del front (C1).
- **Recomendación operativa**: desplegar back y front en el mismo release.
- Login y refresh no cambian de forma salvo `user.roles[]` (aditivo).

### D6 — Sin cambios en JWT ni en el modelo de datos

- El claim `roles` ya se emite desde las authorities; no se toca `JwtService`.
- No hay migración de esquema: `users`, `roles` y `user_roles` ya existen. `V6`
  es solo datos.
- Soft delete intacto: el seed no borra ni reactiva nada.

### D7 — Tests y verificación

- Unitarios (Mockito, `@DisplayName` en español) en `UserServiceTest`:
  asignación de roles pedidos, normalización de alias, deduplicación, rol
  inválido → 400, default `ROLE_USER`, 409 de username/email, password mismatch.
- `UserResponse.fromUser` con roles (orden alfabético).
- Seguridad: al no haber tests de integración en el repo, la matriz 401/403/201
  se verifica manualmente contra PostgreSQL (runbook en `tasks.md`) y con
  `@WebMvcTest` si el equipo lo considera necesario; no se introducen
  TestContainers en este change.
- Verificación de migración: base limpia (`flyway clean`/BD nueva) + arranque con
  las env de seed; y base existente (idempotencia, no toca `admin.demo`).

## Riesgos / Trade-offs

- **[Bloqueo total si el seed falla y el registro ya está cerrado]** →
  fail-fast del placeholder + runbook de verificación post-deploy (`SELECT` del
  admin) + la migración es idempotente para reintentar.
- **[Usuario `admin` soft-deleted con el mismo username]** → la migración aborta
  con un mensaje accionable (no promueve cuentas borradas); el runbook indica
  revisar `users` incluyendo `deleted_at` y elegir otro username o restaurar el
  usuario.
- **[Cambiar `ADMIN_PASSWORD_HASH` después del primer arranque no cambia la
  contraseña]** → Flyway no re-ejecuta `V6`; la rotación es un `UPDATE` manual
  documentado (o un futuro endpoint de password, deuda T8). El runbook lo dice.
- **[`roles` en `UserResponse` provoca lazy loading]** → mitigado en D3 con el
  retorno de DTO desde el servicio; test que cubre el 201.
- **[Auto-asignación de ADMIN por un ADMIN]** → aceptado por diseño (el ADMIN es
  el dueño del sistema); el catálogo cerrado evita roles arbitrarios.
- **[Despliegue del back antes que el front]** → `/registro` roto a propósito;
  coordinado en D5.

## Migration Plan

1. Añadir placeholders a `application.properties` y el valor de desarrollo a
   `application-local.properties`.
2. Desplegar con `ADMIN_USERNAME`, `ADMIN_EMAIL`, `ADMIN_PASSWORD_HASH` definidos;
   Flyway aplica `V6` al arrancar.
3. Verificar: login del admin sembrado y `GET /users` con el token → 200;
   `POST /users` sin token → 401; con token USER → 403.
4. Desplegar el front `alta-usuarios-admin` (retira `/registro`) en el mismo
   release o inmediatamente después.
5. Rollback: revertir el despliegue del código; `V6` queda aplicada (Flyway es
   forward-only) y el usuario sembrado sigue existiendo — inofensivo. Si hay que
   retirarlo, `UPDATE users SET deleted_at = now()` es una decisión manual de
   operación, no un `clean`.

## Open Questions

- Ninguna bloqueante. La forma exacta de `roles` (alias aceptados, allow-list) se
  fija aquí y el front ya envía mayúsculas sin prefijo; cualquier ampliación del
  catálogo es un cambio de contrato explícito.
