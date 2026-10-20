# Tasks: gestion-usuarios-admin

> Implementación según `design.md` (decisiones D1–D7 vinculantes) y el delta
> `specs/gestion-usuarios/spec.md`. Rama: `feature/gestion-usuarios-admin`.
> Propietario: `user-security` (dominio de usuarios y seguridad del back).
> Cierre: `/review` con `quality-guardian`.
>
> Desbloquea la fase B del front `alta-usuarios-admin` (P1/P2/P3 de D7).
> Verificación final: `./mvnw test` en verde (con PostgreSQL vivo para el context
> test) y la matriz manual de 401/403/201 del §6.

## 1. P1 — Cerrar el registro público

- [x] 1.1 `config/SecurityConfig.java`: quitar `POST /api/v1/users` de
      `permitAll`; dejar públicos solo `POST /api/v1/auth/login` y
      `POST /api/v1/auth/refresh`. Añadir el matcher
      `POST /api/v1/users` → `hasAnyRole("ADMIN")` (D1).
- [x] 1.2 `user/UserController.java`: añadir
      `@PreAuthorize("hasRole('ADMIN')")` a `createUser` y actualizar el javadoc
      (ya no es público).
- [x] 1.3 Verificar que `GET /users` sigue con `@PreAuthorize` y que no queda
      ningún otro endpoint público no intencional.

## 2. P2 — Roles en la creación y en la respuesta

- [x] 2.1 `user/dto/CreateUserRequest.java`: campo `List<String> roles` con
      getter/setter y constructor actualizado (sin romper el constructor de 6
      argumentos que usan los tests).
- [x] 2.2 `user/UserService.java`: constante con el catálogo cerrado
      (`ROLE_ADMIN`, `ROLE_COACH`, `ROLE_USER`); normalizar alias
      (`ADMIN`/`COACH`/`USER`, case-insensitive) a `ROLE_*`, deduplicar, validar
      contra el catálogo (`ValidationException` → 400 con el valor inválido) y
      aplicar `ROLE_USER` cuando `roles` es nulo o vacío (D2). Resolver cada rol
      con `RoleRepository.findByName`.
- [x] 2.3 `UserService.createUser`: devolver `UserResponse` mapeado **dentro** de
      `@Transactional` (D3) y ajustar `UserController.createUser` para no volver a
      mapear.
- [x] 2.4 `user/dto/UserResponse.java`: añadir `List<String> roles` y mapearlos
      desde `user.getRoles()` ordenados alfabéticamente (D3).
- [x] 2.5 Revisar los llamadores de `UserResponse.fromUser` para confirmar que
      todos mapean dentro de una transacción (`AuthService.login/refresh`,
      `getAllUsers`).

## 3. P3 — Seed del admin inicial

- [x] 3.1 `src/main/resources/db/migration/V6__seed_admin_usuario_inicial.sql`:
      insertar el usuario con los placeholders `admin_username`, `admin_email`,
      `admin_password_hash` (`ON CONFLICT (username) DO NOTHING`) y crear el
      vínculo con `ROLE_ADMIN` solo si falta (D4).
- [x] 3.2 `src/main/resources/application.properties`: mapear
      `spring.flyway.placeholders.admin_username=${ADMIN_USERNAME:admin}`,
      `admin_email=${ADMIN_EMAIL:admin@athletecore.local}` y
      `admin_password_hash=${ADMIN_PASSWORD_HASH}` (sin default para el hash:
      fail-fast).
- [x] 3.3 `src/main/resources/application-local.properties` (gitignored): hash
      BCrypt de la contraseña de desarrollo documentada, para que el arranque
      local no dependa de una env externa.
- [x] 3.4 `.env.example`: documentar `ADMIN_USERNAME`, `ADMIN_EMAIL` y
      `ADMIN_PASSWORD_HASH`, con el comando de generación del hash
      (`htpasswd -bnBC 12 "" 'mi-password'`) y la advertencia de no usar la
      contraseña de desarrollo en producción.
- [x] 3.5 `src/test/java/com/athletecore/api/AthletecoreApiApplicationTests.java`:
      pasar el placeholder por `@SpringBootTest(properties = ...)` para que el
      contexto arranque en tests (D4).

## 4. Tests

- [x] 4.1 `UserServiceTest` (Mockito, `@DisplayName` en español): rol pedido
      (`COACH` → `ROLE_COACH`), alias con prefijo, duplicados deduplicados, rol
      inválido → `ValidationException` sin guardar, sin roles → `ROLE_USER`, y
      los casos existentes de 409 y contraseñas distintas siguen verdes.
- [x] 4.2 Test de `UserResponse.fromUser` con roles (orden alfabético y sin
      password).
- [x] 4.3 Verificación manual contra PostgreSQL (no hay tests de integración):
      login del admin sembrado → 200 con `user.roles`; `POST /users` sin token →
      401; con token `ROLE_USER` → 403; con token ADMIN y `roles: ["COACH"]` →
      201 con `ROLE_COACH`; `roles: ["SUPERUSER"]` → 400.
- [x] 4.4 Verificar la migración en base limpia (V6 aplica y el admin puede
      loguearse) y sobre una base existente (no duplica ni toca `admin.demo`).

## 5. Documentación

- [x] 5.1 `docs/frontend-handoff/03-contrato-api-usuarios.md`: tabla de endpoints
      (§1), reescribir §3 (`POST /users` ahora ADMIN, request con `roles`,
      respuestas), §4 (`UserResponse.roles`) y el diagrama de flujo de alta.
- [x] 5.2 `docs/frontend-handoff/02-autenticacion-y-seguridad.md`: endpoints
      públicos (solo login y refresh), política de alta por ADMIN y roles.
- [x] 5.3 `docs/frontend-handoff/00-vision-general.md`: actualizar la decisión
      "un solo endpoint público" y la mención al registro; `01-infraestructura-y-setup.md`:
      quitar "no hay usuario admin seedeado" y documentar el seed.
- [x] 5.4 `docs/frontend-handoff/09-decisiones-deuda-y-pendientes.md`: T7
      resuelta (roles en `UserResponse`), T8 acotada a lo que sigue faltando
      (`PUT`/`DELETE`/`/me`/password), decisión de arquitectura 4 actualizada.
- [x] 5.5 `AGENTS.md`: actualizar la línea de seguridad (solo login y refresh
      públicos; `POST /users` es ADMIN) y añadir el seed del admin a
      Database/Flyway.
- [x] 5.6 `TASKS.md`: actualizar la sección User/Security y el roadmap (el
      registro público ya no existe; admin inicial por seed).
- [x] 5.7 `docs/flyway.md`: documentar los placeholders y el runbook del seed
      (generación del hash, verificación post-deploy, rotación manual de la
      contraseña, caso de username soft-deleted).
- [x] 5.8 `README.md`: sección de arranque con las variables del seed y el orden
      de despliegue coordinado con el front (D5).

## 6. Cierre

- [x] 6.1 `./mvnw test` en verde (PostgreSQL vivo para el context test) y
      `./mvnw compile` limpio.
- [x] 6.2 Ejecutar la matriz manual de §4.3 y anotar el resultado.
- [ ] 6.3 `/review` con `quality-guardian` (alcance: el diff del change); corregir
      hallazgos.
- [ ] 6.4 Avisar al front: P1/P2/P3 entregados (verificados contra código), con
      la forma final del request y el nombre exacto de los roles; el front retoma
      `alta-usuarios-admin`.
- [ ] 6.5 Sync de la spec `gestion-usuarios` + archive del change + merge a
      `develop` + push.

## Desviaciones

(ninguna todavía)
