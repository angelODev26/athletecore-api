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
      bloque PL/pgSQL implementado con validaciones de username/email/hash
      (BCrypt coste 04-31), normalización `btrim`, alta del usuario + vínculo
      `ROLE_ADMIN`, y promoción solo de la cuenta sembrada (mismo hash exacto,
      `enabled`, sin soft delete); aborta si el email está en uso (D4, ver
      Desviación 2).
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
- [x] 6.3 `/review` con `quality-guardian`: informe recibido (0 bloqueantes,
      3 ALTO, 6 MEDIO, 6 BAJO) e incorporado; A1-A3 y M1-M6/B1-B6 corregidos
      (ver Desviaciones 6).
- [x] 6.4 Avisar al front: P1/P2/P3 entregados (verificados contra código), con
      la forma final del request y el nombre exacto de los roles; el front retoma
      `alta-usuarios-admin`.
- [x] 6.5a Merge a `develop` + push (merge `f4983d7`).
- [ ] 6.5b Sync de la spec `gestion-usuarios` a `openspec/specs/` + archive del
      change.

## Desviaciones

1. **Review sustituto.** `quality-guardian` no existe en la sesión del front; el
   diff lo revisó un agente `general` en modo solo lectura. Sus hallazgos
   BLOQUEANTE/MAYORES se corrigieron: seed que no promueve cuentas
   preexistentes no confiables (S1), validación BCrypt del hash (S2), propagación
   de `ADMIN_*` en `docker-compose` (M1), test de seguridad 401/403/201 (M2),
   falsos positivos y casos límite de tests (M3/M4), aislamiento del context test
   (D4) y ejemplo versionado del perfil local (M5). Queda como deuda T11 el 500
   con username/email de un usuario soft-deleted (preexistente, documentado).
2. **Seed más estricto que el diseño original.** La spec/design se actualizaron:
   un username preexistente solo recibe `ROLE_ADMIN` si su password coincide con
   el hash sembrado, está `enabled` y sin soft delete; si no, la migración aborta.
   Se añadió validación de formato BCrypt y de email en uso.
3. **Matcher de URL ampliado** a `/api/v1/users` + `/api/v1/users/**` (no solo
   `POST`) para cubrir métodos futuros, como pedía el rationale de D1.
4. **`application-local.properties.example`** nuevo (versionado) porque el
   archivo real está gitignored y la documentación decía que "traía" el admin de
   desarrollo.
5. **`docker-compose` propaga `ADMIN_*`** con default vacío (`:-`), para no
   romper `docker compose up -d postgres`; el fail-fast real ocurre en V6 con un
   mensaje accionable. El hash va entre comillas simples en `.env` (Compose
   interpreta `$`).
6. **Review oficial `quality-guardian` (informe A1-A3/M1-M6/B1-B6).** Corregido:
   - A1: `:?` en compose rompía `docker compose up -d postgres` →
     `${ADMIN_PASSWORD_HASH:-}`; el fail-fast ocurre en V6.
   - A2: documentado que el hash va entre comillas simples en `.env` (Compose
     interpreta `$`); también en README y `docs/flyway.md`.
   - A3: runbook de `flyway repair` para bases con el V6 anterior (checksum) y
     aviso del context test sobre un `admin` conflictivo; sin `mvn flyway:repair`
     (no hay plugin). Se documentó también el flujo real de Compose.
   - M1: test discriminante de la regla de URL (`GET /users/1` con USER → 403).
   - M2: regex BCrypt endurecido (coste 04-31 y charset base64).
   - M3/M4: design/proposal/tasks/flyway sin `ON CONFLICT` ni "misma password";
     se habla de comparación exacta del hash.
   - M5: documentado que username/email no admiten comilla simple (los valores se
     interpolan en SQL; el dollar-quoting con tag no lo sustituye Flyway).
   - M6/B6: deudas registradas en `TASKS.md` (test de V6 y T11).
   - B1/B2: soft delete se comprueba antes que el hash; username/email se
     normalizan con `btrim` y se usan vía variables PL/pgSQL.
   - B3/B4: corregidas las frases de "el perfil local ya trae" y añadido CORS al
     `.example`.
   - B5: 6.3 marcada tras incorporar el informe.
   - Extra: `postgres` añadido a los perfiles `migrate`/`development`/`production`
     y retirados de la doc los comandos `mvn flyway:*` / `scripts/flyway.sh`
     inexistentes.

7. **Review del bloque completo del change (M-1..M-4, B-1..B-4, N-1..N-8).**
   Corregido:
   - M-1: `docs/flyway.md` ya no documenta `mvn flyway:*` ni `scripts/flyway.sh`;
     validación y baseline se explican con Spring Boot/Compose.
   - M-2: `flyway` habilitado en los perfiles `development` y `production`, para
     que los `depends_on` de `app-dev`/`app` resuelvan.
   - M-3: credenciales de Flyway movidas al `environment` del servicio (no en
     `ps`/historial); el runbook de `repair` es `docker compose ... run flyway repair`.
   - M-4: `CORS_ALLOWED_ORIGINS` unificado a `CORS_ORIGINS` (la variable que lee
     `application.properties`), en compose y README.
   - B-1/B-3: documentado `SPRING_PROFILES_ACTIVE=local`, la restricción de
     apóstrofo y el coste/charset BCrypt en los documentos operativos.
   - B-2: comentario del test discriminante corregido (500, no 404/405).
   - B-4: PostgreSQL publicado solo en `127.0.0.1` y aviso de credenciales en
     producción.
   - N-1/N-6: flujo canónico y matriz de acceso alineados con el registro cerrado.
   - N-2: política de password unificada a mínimo 8 (DTO y entidad).
   - N-3: `DataIntegrityViolationException` → 409 (T11 resuelta).
   - N-4: context test con identidad y hash propios, no el admin de desarrollo.
   - N-5/N-7/N-8: `@EntityGraph` en `findAll`, rol de catálogo ausente → 400 y
     `JWT_SECRET` sin fallback en `app-dev`.
