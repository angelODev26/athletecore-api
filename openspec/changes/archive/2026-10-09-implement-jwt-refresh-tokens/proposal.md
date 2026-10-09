## Why

El flujo de autenticación actual solo emite un **access token** JWT (`JwtService.generateToken`, expiración por defecto 24 h vía `spring.security.jwt.expiration`). No existe endpoint de refresh: al expirar el token, el frontend se ve forzado a re-hacer el login completo. Esto es el **bloqueante de auth** identificado en `TASKS.md` (línea 127) y en la decisión **D1** de `docs/frontend-handoff/09-decisiones-deuda-y-pendientes.md`: el front ya dejó lista la abstracción `AuthTokens { access, refresh? }` para no romper pantallas cuando el refresh exista.

## What Changes

### Nuevas Funcionalidades:
- **Endpoint `POST /api/v1/auth/refresh`** (público, `permitAll`): recibe `{ "refreshToken": "<jwt>" }` y devuelve un par nuevo `{ accessToken, refreshToken, ... }` con el mismo estilo de respuesta que `POST /api/v1/auth/login`.
- **Emisión de refresh token** en el login: el `AuthResponse` pasa a incluir `refreshToken` además del access token.
- **`JwtService.generateRefreshToken`**: token JWT HS256 con expiración propia configurable (`spring.security.jwt.refresh-expiration`, sugerido 7 días), claim discriminador `type: "refresh"` y claims `sub` + `userId`.
- **Validación de tipo de token**: el filtro de autenticación (`JwtAuthenticationFilter`) rechaza refresh tokens usados como access tokens (valida el claim `type`).
- **Rotación de refresh tokens**: cada llamada a `/auth/refresh` emite un par nuevo.

### Modificaciones:
- `JwtService`: nuevo método `generateRefreshToken`, validación de claim `type`, propiedad de expiración propia.
- `AuthService` / `AuthController`: login devuelve par de tokens; nuevo endpoint `/auth/refresh`.
- `SecurityConfig`: `/api/v1/auth/refresh` añadido a los endpoints públicos.
- Propiedad nueva `spring.security.jwt.refresh-expiration` en `application.properties` (y su fallback en `application-local.properties` gitignored).
- Errores: refresh inválido/expirado → **401** con `ErrorResponse` "Autenticación requerida" (mismo contrato que el resto de la API).
- Documentación: actualización de `docs/frontend-handoff/02-autenticacion-y-seguridad.md` y `TASKS.md` (quitar bloqueante, marcar checkbox).

### Fuera de alcance:
- Logout y `/me` (features aparte).
- Revocación con estado en BD (tabla de refresh tokens) — opcional; si no se implementa, revocación = validación de firma + expiración + rotación (documentado como decisión en `design.md`).

### BREAKING:
- **Ninguno para el contrato existente** de `/auth/login` (se añade un campo `refreshToken` a la respuesta — aditivo, no rompe clientes que ignoran campos desconocidos).

## Capabilities

### New Capabilities
- `token-refresh`: Renovación de sesión mediante refresh tokens JWT con rotación, validación de tipo de token y respuesta 401 consistente ante tokens inválidos o expirados.

### Modified Capabilities
- Ninguna (`openspec/specs/` no contiene specs existentes; el login actual no cambia sus requisitos, solo extiende su respuesta).

## Impact

### Archivos Afectados (estimación):
- **Modificados**: `user/security/JwtService.java`, `user/security/JwtAuthenticationFilter.java`, `user/AuthService.java`, `user/AuthController.java`, `user/dto/AuthResponse.java`, `config/SecurityConfig.java`, `application.properties`
- **Nuevos**: `user/dto/RefreshTokenRequest.java`, tests en `src/test/java/com/athletecore/api/user/` (unit tests de `JwtService` y `AuthService` con MockitoExtension)
- **Docs**: `docs/frontend-handoff/02-autenticacion-y-seguridad.md`, `TASKS.md`

### API Endpoints:
```
POST /api/v1/auth/refresh    # NUEVO — público. Body: { "refreshToken": "<jwt>" }
                             # 200: { "token", "refreshToken", "type", "expiresIn", "user" }
                             # 401: ErrorResponse "Autenticación requerida" si inválido/expirado
POST /api/v1/auth/login      # MODIFICADO (aditivo) — la respuesta ahora incluye refreshToken
```

### Dependencias:
- Sin dependencias nuevas: se reutiliza `io.jsonwebtoken` (jjwt) ya presente.
- Sin migración Flyway (la revocación con estado queda fuera del MVP salvo decisión contraria en `design.md`).

### Consideraciones:
- **Rotación**: cada `/auth/refresh` emite par nuevo. Con revocación mínima (firma + expiración), reusar el refresh anterior sigue siendo técnicamente válido hasta su expiración; la revocación estricta por reuso requiere estado (tabla) y se documenta como decisión.
- **Mismo secret**: el refresh se firma con el mismo `JWT_SECRET` HS256; la distinción access/refresh es por claim `type`, validado en el filtro.
- **Criterio de aceptación**: con login de un usuario seed, `POST /auth/refresh` con el refresh emitido devuelve par nuevo (200); si se implementa revocación con estado, reusar el refresh anterior falla con 401.
