## 1. Configuración y JwtService

- [x] 1.1 Añadir propiedad `spring.security.jwt.refresh-expiration` (default `604800000` = 7 días) a `application.properties` y a `application-local.properties` (fallback de desarrollo)
- [x] 1.2 Extender `JwtService`: nuevo método `generateRefreshToken(User)` con claims `sub`, `userId`, `type: "refresh"` y expiración propia; añadir claim `type: "access"` al access token en `generateToken`
- [x] 1.3 Añadir a `JwtService` método de validación de refresh token (`isRefreshToken` / validación de firma + expiración + claim `type`), con `Clock` inyectable como en el resto del servicio

## 2. Seguridad

- [x] 2.1 Actualizar `JwtAuthenticationFilter` para rechazar tokens con `type: "refresh"` (y aceptar tokens sin claim `type` como access durante la ventana de transición, ver design D1)
- [x] 2.2 Añadir `/api/v1/auth/refresh` a los endpoints públicos (`permitAll`) en `SecurityConfig`

## 3. Endpoint de refresh

- [x] 3.1 Crear DTO record `RefreshTokenRequest` (`refreshToken` con `@NotBlank`) en `user/dto/`
- [x] 3.2 Extender `AuthResponse` con el campo `refreshToken` (ajustar `AuthService.login` para emitir el par)
- [x] 3.3 Implementar `AuthService.refresh(RefreshTokenRequest)`: valida refresh (firma + expiración + `type`), carga el `User` desde `UserRepository` (401 si no existe/está inactivo) y devuelve `AuthResponse` con par nuevo y roles actuales; errores vía `InvalidCredentialsException` (401)
- [x] 3.4 Añadir `POST /api/v1/auth/refresh` en `AuthController` delegando en `AuthService.refresh`

## 4. Tests

- [x] 4.1 Tests unitarios de `JwtService` (JUnit 5 + Mockito, `@DisplayName` en español): generación de refresh con claims y expiración propia; validación rechaza refresh expirado y token de tipo incorrecto
- [x] 4.2 Tests unitarios de `AuthService.refresh`: par nuevo con refresh válido; 401 con refresh expirado, con access usado como refresh y con usuario inexistente
- [x] 4.3 Verificación manual del criterio de aceptación: `docker-compose up -d postgres` + login con usuario seed → `POST /auth/refresh` devuelve par nuevo (200) con tokens distintos
- [x] 4.4 `./mvnw test` en verde (levantar PostgreSQL con docker-compose para el context test)

## 5. Documentación y cierre

- [x] 5.1 Actualizar `docs/frontend-handoff/02-autenticacion-y-seguridad.md`: sección de refresh (request/response, expiraciones, rotación, errores 401), ajustar las menciones de "NO existe refresh token" (también en `00-vision-general.md` D1 y `09-decisiones-deuda-y-pendientes.md` → resolver D1 como "sí, implementado")
- [x] 5.2 Actualizar `TASKS.md`: marcar `[x] Implementar refresh tokens JWT` en §Seguridad Avanzada y eliminar/actualizar el bloque "⚠️ Bloqueante de auth — refresh tokens JWT"
- [x] 5.3 Actualizar el diagrama de flujo de auth en `docs/frontend-handoff/flujo-completo-athletecore.md` (DENTRO del repo, junto al handoff) quitando el "Sin refresh" del note de login y añadiendo el paso de renovación vía POST /api/v1/auth/refresh
