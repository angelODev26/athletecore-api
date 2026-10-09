# 02 — Autenticación y Seguridad (impacto directo en el front)

> Fuentes: `SecurityConfig.java`, `JwtService.java`, `AuthService.java`,
> `RestAuthenticationEntryPoint.java`, `RestAccessDeniedHandler.java`, migraciones V2/V3.
> Este es **el archivo más importante del paquete** junto con el `08`: define cómo el front
> se autentica, qué pasa cuando el token expira y qué puede ver cada rol.

---

## 1. Modelo: JWT stateless, par access + refresh

- Login emite **dos tokens JWT firmados con HS256**: un **access token** (vida corta) y un
  **refresh token** (vida larga) para renovar la sesión sin repetir el login.
- Expiración del access token: **`JWT_EXPIRATION`, default 24 h** (`86400000` ms).
- Expiración del refresh token: **`JWT_REFRESH_EXPIRATION`, default 7 días** (`604800000` ms).
- **No hay endpoint de logout** (stateless: el servidor no guarda sesiones; "cerrar sesión"
  es simplemente que el front descarte ambos tokens).
- **No hay endpoint `/me`** — los datos del usuario vienen dentro de la respuesta del login
  y del refresh (ver archivo `03`).

### Claims del access token (decodificable en el front)

```json
{
  "sub": "<username>",
  "userId": 123,
  "roles": ["ROLE_USER", "ROLE_ADMIN"],
  "type": "access",
  "iat": 1757193600,
  "exp": 1757280000
}
```

El front **puede decodificar el JWT** para obtener username, userId, roles y expiración
sin ninguna llamada extra. Eso es intencional: no hay endpoint alternativo para obtenerlos.

### Refresh token

- Claims: `sub`, `userId`, `type: "refresh"` (sin `roles` — no autoriza nada).
- **No sirve como credencial de acceso**: si se envía en `Authorization: Bearer` a un
  endpoint protegido, la respuesta es 401. Del mismo modo, un access token enviado a
  `/auth/refresh` es rechazado con 401.
- **Rotación**: cada llamada a `/auth/refresh` devuelve un par nuevo (access + refresh).
  El refresh anterior **no se invalida en servidor** (revocación sin estado: solo se
  valida firma + expiración; ver decisión en `09`).

### Cómo enviar el token

```
Authorization: Bearer <accessToken>
```

en todas las peticiones excepto las públicas (§3).

---

## 2. Expiración de sesión y flujo de renovación

El access token expira en 24 h (default) y el refresh en 7 días (default). El flujo que
el front debe implementar:

1. **Login**: guardar `token`, `refreshToken` y `expiresIn` (ms, del access) que devuelve
   `POST /api/v1/auth/login`.
2. **Interceptor/outbound**: adjuntar `Authorization: Bearer <token>` si hay access token.
3. **Ante un `401`** en un endpoint protegido:
   a. Intentar `POST /api/v1/auth/refresh` con el `refreshToken` guardado.
   b. Si responde **200**: reemplazar ambos tokens por el par nuevo y reintentar la
      petición original.
   c. Si responde **401** (refresh inválido/expirado): limpiar sesión y redirigir a login.
4. Opcional pero recomendado: usar el claim `exp` del access token para renovar
   **proactivamente** antes de que expire y evitar el 401 intermedios.

### `POST /api/v1/auth/refresh` (público)

Request:

```json
{ "refreshToken": "<jwt de tipo refresh>" }
```

Response 200 (misma forma que el login):

```json
{
  "token": "<nuevo access token>",
  "refreshToken": "<nuevo refresh token>",
  "type": "Bearer",
  "expiresIn": 86400000,
  "user": { "id": 123, "username": "...", "email": "...", "roles": ["ROLE_USER"] }
}
```

Errores: refresh inválido, expirado, de tipo incorrecto o de un usuario deshabilitado →
**401** con `ErrorResponse` `"Autenticación requerida"` (ver §5).

---

## 3. Matriz de acceso (exacta, del `SecurityConfig`)

### Públicos (sin token)

| Método | Path |
|--------|------|
| `POST` | `/api/v1/auth/login` |
| `POST` | `/api/v1/auth/refresh` |
| `POST` | `/api/v1/users` (registro — siempre crea con `ROLE_USER`) |

### Restringidos por rol a nivel de URL (`hasAnyRole("ADMIN","COACH")`)

| Path pattern | Roles |
|---|---|
| `/api/v1/training-plans/**` | ADMIN, COACH |
| `/api/v1/training-cycles/**` | ADMIN, COACH |
| `/api/v1/training-sessions/**` | ADMIN, COACH |
| `/api/v1/alerts/attendance/**` | ADMIN, COACH |
| `/api/v1/athletes/*/attendance` | ADMIN, COACH |

> Un usuario `ROLE_USER` autenticado recibe **403** en cualquiera de estos paths.

### Restringidos por `@PreAuthorize` a nivel de método (en los controllers)

| Endpoint | Rol |
|---|---|
| `GET /api/v1/users` | ADMIN |
| `POST/PUT/DELETE /api/v1/national-reference-times/**` (tabla nacional) | ADMIN |

(La lista completa por endpoint está en los archivos `03`–`07`.)

### Todo lo demás: solo requiere estar autenticado

Athletes CRUD, checkups, reportes… cualquier rol válido (`ROLE_USER` incluido) accede.
**No hay restricción de propiedad** (un USER puede ver todos los deportistas, no solo "los suyos").

---

## 4. Roles existentes en el sistema

| Rol | Sembrado en | Qué puede hacer (resumen) |
|-----|------------|---------------------------|
| `ROLE_USER` | Migración V2 | Todo lo autenticado excepto lo restringido arriba |
| `ROLE_ADMIN` | Migración V2 | Todo + gestión de usuarios + tabla nacional de referencia |
| `ROLE_COACH` | Migración V3 | Entrenamientos, asistencia y alertas |

- Un usuario puede tener **varios roles** (claim `roles` es un array).
- **Todo registro público recibe únicamente `ROLE_USER`** (`UserService.createUser`, hardcoded).
  No existe endpoint para cambiar roles → los ADMIN/COACH se crean a mano en DB (ver `01` §5).
- Si en el futuro se quieren "entrenadores se registran solos", hace falta cambio de backend
  (decisión pendiente, ver `09`).

### Modelo sugerido de roles en el front

```
USER  → dashboards de consulta (deportistas, chequeos, reportes)
COACH → + planificación, sesiones, asistencia, alertas
ADMIN → + gestión de usuarios + tabla nacional de referencia
```

El front debe ocultar/deshabilitar rutas por rol usando el claim `roles` del JWT,
sabiendo que **el back siempre re-valida** (ocultar es UX, no seguridad).

---

## 5. Formato de errores de autenticación/autorización

Los handlers de seguridad devuelven el **mismo `ErrorResponse` JSON que el resto de la API**
(formato completo en archivo `08`):

### `401 Unauthorized` — sin token, token inválido o expirado

```json
{ "status": 401, "error": "Unauthorized", "message": "Autenticación requerida" }
```

- Origen: `RestAuthenticationEntryPoint` (filtro de seguridad).
- También 401 con `"message": "Credenciales inválidas"` en login fallido
  (`InvalidCredentialsException` — no distingue usuario inexistente de password errónea,
  deliberadamente, para no filtrar usuarios).
- **`POST /auth/refresh` con refresh inválido/expirado/tipo incorrecto también es 401**
  con `"message": "Autenticación requerida"` — el front debe tratarlo como "sesión
  terminada" y redirigir a login.

### `403 Forbidden` — autenticado pero sin el rol

```json
{ "status": 403, "error": "Forbidden", "message": "Acceso denegado" }
```

- Origen: `RestAccessDeniedHandler`.

**Distinción clave para el front:** `401` → a login; `403` → pantalla de "sin permisos",
NO a login.

---

## 6. Otros hechos de seguridad que afectan al front

- **CSRF deshabilitado** (API stateless con Bearer, no cookies): el front puede llamar
  desde cualquier origen permitido sin tokens CSRF.
- **`allow-credentials: true`** está activo cuando CORS está habilitado — compatible con
  bearer tokens en header. La elección de dónde guardar los tokens (memoria vs
  `localStorage`) es del front; el back es agnóstico. (Recomendación estándar:
  `localStorage` es aceptable dado el perfil de riesgo; el refresh token tiene vida
  más larga, así que si se quiere endurecer, guárdalo separado del access token o
  valora `sessionStorage` para el access. Decisión final del equipo front.)
- **Contraseñas**: BCrypt(12). Política de password en registro: mínimo 6 caracteres
  (débil — la robustez extra, si se quiere, se valida en front; ver `03`).
- **No hay** rate limiting ni bloqueo por fuerza bruta (backlog POST-v1.0.0). El front
  puede implementar su propio "debounce"/deshabilitar botón tras N intentos, pero no debe
  esperar 429 del servidor.
- **Swagger UI y `/v3/api-docs` también exigen JWT** — no asumir que son públicos.

---

**Anterior:** `01-infraestructura-y-setup.md` · **Siguiente:** `03-contrato-api-usuarios.md`
