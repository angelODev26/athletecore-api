## Context

La autenticación actual es stateless con JWT HS256: `JwtService.generateToken(User)` emite un access token con claims `sub` (username), `userId`, `roles`, `iat`, `exp`, firmado con el secret de `spring.security.jwt.secret` y expiración de `spring.security.jwt.expiration` (24 h por defecto). `JwtAuthenticationFilter` valida el token en cada request; `SecurityConfig` solo deja públicos `POST /api/v1/users` y `POST /api/v1/auth/login`. `AuthService.login` devuelve `AuthResponse(token, type, expiresIn, user)`.

El frontend ya consume esta API y dejó preparada la abstracción `AuthTokens { access, refresh? }` (decisión D1 de `docs/frontend-handoff/09-decisiones-deuda-y-pendientes.md`), por lo que añadir refresh tokens no rompe pantallas. Este cambio cierra el "Bloqueante de auth" de `TASKS.md §Seguridad Avanzada`.

Restricciones del proyecto: respuestas siempre con DTOs records, excepciones centralizadas en `GlobalExceptionHandler` desde `common/exception`, errores de auth → 401 con `ErrorResponse` "Autenticación requerida" (vía `RestAuthenticationEntryPoint`), tests de servicios con JUnit 5 + MockitoExtension en español, documentación en español.

## Goals / Non-Goals

**Goals:**
- Emitir par access + refresh en el login.
- Endpoint público `POST /api/v1/auth/refresh` con rotación (par nuevo en cada llamada).
- Refresh token distinguible del access token (claim `type: "refresh"`) y rechazado si se usa como access token.
- Expiración del refresh configurable de forma independiente (`spring.security.jwt.refresh-expiration`, sugerido 7 días).
- Contrato de error consistente: refresh inválido/expirado → 401 con `ErrorResponse`.
- Cobertura de tests unitarios de la lógica nueva (`JwtService`, `AuthService`).

**Non-Goals:**
- Logout (`POST /auth/logout`) y endpoint `/me` — features aparte.
- Revocación con estado en base de datos (tabla de refresh tokens, detección de reuso) — decisión D2: queda fuera del MVP, documentada como evolución futura.
- Cambios en el formato del access token existente (claims, expiración).
- Migraciones Flyway (no hay tablas nuevas).

## Decisions

### D1 — Claim `type` para distinguir access de refresh (en lugar de secret o audience distintos)
El refresh token se firma con el **mismo secret HS256** que el access token y se distingue por un claim `type`:
- Access tokens: se emiten con `type: "access"` (se añade el claim explícito; tokens antiguos sin claim se tratan como access para compatibilidad **solo durante el despliegue** — al ser stateless con exp 24 h, todos los tokens en circulación expiran en < 24 h, ventana aceptable).
- Refresh tokens: `type: "refresh"`, claims `sub` y `userId` (sin `roles`: no se usan para autorización).

**Validación cruzada obligatoria:**
- `JwtAuthenticationFilter` rechaza cualquier token cuyo `type` sea `"refresh"` (o distinto de `"access"`/ausente) → 401.
- El endpoint de refresh rechaza tokens cuyo `type` no sea `"refresh"` → 401.

**Alternativas consideradas:**
- *Secret distinto para refresh*: más aislamiento criptográfico, pero duplica configuración de secretos (otra env var, otro fallback local) sin beneficio real a esta escala; el claim `type` validado en ambos lados ya impide el uso cruzado.
- *Claim `aud`*: semánticamente correcto, pero jjwt lo valida con API distinta y el patrón `type` es más explícito y sencillo de testear.

### D2 — Rotación sin estado (sin tabla de refresh tokens) para el MVP
Cada `POST /auth/refresh` valida firma + expiración + `type: "refresh"` y emite un **par nuevo** (access + refresh). El refresh anterior **no se invalida en servidor**: sigue siendo válido hasta su expiración natural.

**Rationale:** no requiere migración ni tabla nueva, mantiene el sistema stateless y cierra el bloqueante del front hoy. El riesgo residual (un refresh robado es usable hasta 7 días) se mitiga con expiración corta configurable y con la rotación (el cliente legítimo siempre usa el último par).

**Evolución futura (documentada, no implementada):** tabla `refresh_tokens` (hash del token, `user_id`, `expires_at`, `revoked_at`) con revocación por usuario y detección de reuso (reuse detection: si se presenta un token ya rotado, revocar toda la cadena). Cuando se implemente, el contrato del endpoint no cambia.

### D3 — El refresh resuelve el usuario desde BD (no confía solo en claims)
`AuthService.refresh` extrae `sub` del refresh validado y carga el `User` desde `UserRepository`; si el usuario ya no existe o está inactivo → 401. Esto evita emitir access tokens para usuarios eliminados/desactivados entre emisión y refresh. Se aprovecha además para regenerar el access con los roles **actuales** del usuario (si un admin le quita un rol, el próximo refresh lo refleja en ≤ 24 h).

### D4 — Respuesta uniforme con el login
`POST /auth/refresh` devuelve el mismo DTO `AuthResponse` extendido: `{ token, refreshToken, type: "Bearer", expiresIn, user }`. El login también devuelve `refreshToken` (cambio aditivo). Así el front maneja una sola forma de respuesta de auth y la abstracción `AuthTokens { access, refresh? }` se alimenta igual en login y refresh.

### D5 — Errores vía excepción de dominio, no lógica ad-hoc en el controller
Refresh inválido/expirado/tipo incorrecto → `InvalidCredentialsException` (ya mapeada a 401 por `GlobalExceptionHandler` con el mensaje de autenticación requerida). No se crea una jerarquía de excepciones nueva: el contrato de error es idéntico al 401 actual.

## Risks / Trade-offs

- **[Refresh robado reutilizable hasta su expiración (7 días)]** → Mitigación: rotación en cada uso + expiración corta configurable; revocación con estado documentada como evolución (D2). Reevaluar si el perfil de riesgo cambia.
- **[Tokens access antiguos sin claim `type` (emitidos antes del despliegue) serían rechazados]** → Mitigación: el filtro acepta tokens sin claim `type` como access durante una ventana transitoria; como expiran en ≤ 24 h, el riesgo de ventana abierta es mínimo y autolimitado.
- **[Aumento de superficie del secret único (mismo secret firma access y refresh)]** → Mitigación: validación estricta del claim `type` en filtro y endpoint (D1); si se requiere aislamiento fuerte en el futuro, migrar a secret separado es cambio interno sin impacto de contrato.
- **[`expiresIn` del access en la respuesta podría confundirse con la expiración del refresh]** → Mitigación: `expiresIn` sigue refiriéndose al access token (compatibilidad); la expiración del refresh es documentación de configuración, no parte del contrato de respuesta.

## Migration Plan

1. Añadir `spring.security.jwt.refresh-expiration` (ms, sugerido `604800000` = 7 días) a `application.properties` con default y a `application-local.properties` (gitignored) como fallback de desarrollo.
2. Desplegar: los access tokens antiguos sin claim `type` siguen funcionando hasta su expiración (< 24 h); tras eso, todos los tokens llevan `type`.
3. Rollback: revertir el despliegue; los refresh tokens emitidos quedan inertes (no existe endpoint que los consuma en la versión anterior) y los access tokens nuevos son aceptados por el filtro anterior (claims extra se ignoran).

## Open Questions

- Ninguna bloqueante. La revocación con estado (tabla de refresh tokens + detección de reuso) queda como decisión diferida; si el front o seguridad la requieren, se propone como change separado sin romper contrato.
