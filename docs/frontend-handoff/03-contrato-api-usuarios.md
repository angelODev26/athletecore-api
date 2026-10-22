# 03 — Contrato API: Autenticación y Usuarios

> Fuentes: `AuthController`, `UserController`, `UserService`, `AuthService` y los DTOs de
> `user/dto/`. Convenciones de error y tipos comunes: archivo `08`.
> Base URL: `http://localhost:8080`

---

## 1. Resumen de endpoints

| # | Método | Path | Auth | Descripción |
|---|--------|------|------|-------------|
| 1 | `POST` | `/api/v1/auth/login` | 🌐 Público | Login → par de tokens JWT (access + refresh) |
| 2 | `POST` | `/api/v1/auth/refresh` | 🌐 Público | Renueva la sesión (rotación de tokens) |
| 3 | `POST` | `/api/v1/users` | 🔒 `ROLE_ADMIN` | Alta de usuario con roles (el registro público está cerrado) |
| 4 | `GET` | `/api/v1/users` | 🔒 `ROLE_ADMIN` | Lista completa de usuarios (⚠️ sin paginar) |

**No existen** (no los diseñes en el front): `GET /users/{id}`, `PUT`, `DELETE`,
cambio de password, `/me`, logout. Si el front los necesita, son features
de backend nuevas (ver `09`).

---

## 2. `POST /api/v1/auth/login` 🌐

Autentica y devuelve el par de tokens JWT (access + refresh). Es el camino inicial
para obtener tokens; la renovación posterior se hace con `/auth/refresh` (§2.1).

### Request body

```json
{
  "username": "jdoe",
  "password": "secret123"
}
```

| Campo | Tipo | Validación |
|-------|------|-----------|
| `username` | string | `@NotBlank` — "El nombre de usuario es obligatorio" |
| `password` | string | `@NotBlank` — "La contraseña es obligatoria" |

### Response `200 OK`

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "type": "Bearer",
  "expiresIn": 86400000,
  "user": {
    "id": 1,
    "username": "jdoe",
    "email": "jdoe@mail.com",
    "firstName": "John",
    "lastName": "Doe",
    "enabled": true,
    "active": true,
    "roles": ["ROLE_ADMIN"]
  }
}
```

| Campo | Tipo | Notas |
|-------|------|-------|
| `token` | string | Access token JWT HS256; claims: `sub`, `userId`, `roles[]`, `type: "access"`, `iat`, `exp` |
| `refreshToken` | string | Refresh token JWT HS256; claims: `sub`, `userId`, `type: "refresh"`. Vida: `JWT_REFRESH_EXPIRATION` (default 7 días). **Solo** se usa en `/auth/refresh` |
| `type` | string | Siempre literal `"Bearer"` |
| `expiresIn` | number (long) | **Milisegundos** de vigencia del **access token** (default `86400000` = 24 h). NO es timestamp; es duración. La expiración del refresh no se expone |
| `user` | `UserResponse` | Ver §4 |

### Errores

| Status | Cuándo | `message` |
|--------|--------|-----------|
| `400` | username/password vacíos | Mensaje de validación correspondiente |
| `401` | Credenciales incorrectas | `"Credenciales inválidas"` |

> ⚠️ Usuario inexistente y password errónea devuelven **el mismo 401 con el mismo mensaje**
> (deliberado, anti-enumeración). El front no puede distinguirlos ni debe intentarlo.

---

## 2.1. `POST /api/v1/auth/refresh` 🌐 — Renovación de sesión

Renueva la sesión a partir de un refresh token válido, devolviendo un **par nuevo** de
tokens (rotación: cada llamada emite access y refresh nuevos).

### Request body

```json
{ "refreshToken": "eyJhbGciOiJIUzI1NiJ9..." }
```

| Campo | Tipo | Validación |
|-------|------|-----------|
| `refreshToken` | string | `@NotBlank` — "El refresh token es obligatorio". Debe ser un JWT de tipo refresh |

### Response `200 OK`

Misma forma que el login (`token`, `refreshToken`, `type`, `expiresIn`, `user`) con
tokens **nuevos** y los roles **actuales** del usuario.

### Errores

| Status | Cuándo | `message` |
|--------|--------|-----------|
| `400` | `refreshToken` vacío | Mensaje de validación |
| `401` | Refresh inválido, expirado, access token presentado como refresh, o usuario deshabilitado/inexistente | `"Autenticación requerida"` |

> ⚠️ **Rotación sin estado:** el refresh anterior **no queda revocado** en el servidor;
> sigue siendo válido hasta su expiración (hasta 7 días). El front debe reemplazar
> siempre el par completo y descartar el anterior.

---

## 3. `POST /api/v1/users` 🔒 `ROLE_ADMIN` — Alta de usuario con roles

Crea un usuario nuevo. **Requiere `ROLE_ADMIN`**: el registro público está
cerrado, no existe auto-registro. El ADMIN elige los roles del usuario; si omite
`roles`, el usuario nace con `ROLE_USER`.

### Request body

```json
{
  "username": "jdoe",
  "email": "jdoe@mail.com",
  "firstName": "John",
  "lastName": "Doe",
  "password": "secret123",
  "confirmPassword": "secret123",
  "roles": ["COACH"]
}
```

| Campo | Tipo | Validación (mensaje exacto) |
|-------|------|------------------------------|
| `username` | string | obligatorio, **3–50 chars** — `"Username must be between 3 and 50 characters"` |
| `email` | string | obligatorio, formato email — `"Email should be valid"` |
| `firstName` | string | obligatorio, máx 100 chars |
| `lastName` | string | obligatorio, máx 100 chars |
| `password` | string | obligatorio, **mín 8 chars** — `"Password must be at least 8 characters"` |
| `confirmPassword` | string | obligatorio; debe igualar `password` (validado en servicio, no en DTO) |
| `roles` | string[] | **opcional**. Acepta `ADMIN`, `COACH`, `USER` y sus formas `ROLE_ADMIN`, `ROLE_COACH`, `ROLE_USER` (case-insensitive); se normalizan a `ROLE_*`, se deduplican y se devuelven ordenados. Fuera del catálogo → `400` `"Rol inválido: <valor>"`. Ausente o vacío → `ROLE_USER` |

> Nota: los mensajes de validación de este DTO están en **inglés** (mejorable; el front
> debería mapear por campo y no mostrar el texto crudo — estrategia en archivo `08`).

### Response `201 Created`

Devuelve `UserResponse` (§4/§5) del usuario creado, **incluyendo `roles`**. No
devuelve token: el ADMIN crea la cuenta y el usuario inicia sesión aparte.

### Errores

| Status | Cuándo | `message` |
|--------|--------|-----------|
| `400` | Validación de campos fallida | Mensaje(es) por campo |
| `400` | `password` ≠ `confirmPassword` | `"Las contraseñas no coinciden"` |
| `400` | Rol fuera del catálogo | `"Rol inválido: <valor>"` |
| `401` | Sin token / token expirado | `"Autenticación requerida"` |
| `403` | Autenticado sin `ROLE_ADMIN` | `"Acceso denegado"` |
| `409` | Username ya existe | `"User with username already exists"` |
| `409` | Email ya existe | `"User with email already exists"` |
| `409` | Username/email de un usuario **soft-deleted** o carrera de altas | `"El recurso ya existe o entra en conflicto con una restricción de integridad"` |

> Si el username/email pertenece a un usuario **soft-deleted** (los chequeos de
> duplicado filtran `deleted_at`) o dos altas compiten por el mismo valor, el
> `UNIQUE` global se traduce a **409**, no a 500 (T11 resuelta). El front puede
> tratarlo como "ya existe" sin distinguir el caso.

---

## 4. `GET /api/v1/users` 🔒 `ROLE_ADMIN`

Lista **todos** los usuarios activos. Uso previsto: pantalla de administración de usuarios.

```json
[
  { "id": 1, "username": "jdoe", "email": "jdoe@mail.com", "firstName": "John",
    "lastName": "Doe", "enabled": true, "active": true, "roles": ["ROLE_USER"] }
]
```

- ⚠️ **Sin paginación** — lista completa en memoria. Bloqueante conocido (ver `08`/TASKS):
  el front debe renderizar con virtualización/paginación client-side hasta que el back pagine.
- ✅ Cada elemento incluye `roles` (nombres `ROLE_*`, orden alfabético), así que la tabla
  de administración puede mostrar la columna de rol.

### Errores

| Status | Cuándo |
|--------|--------|
| `401` | Sin token / token expirado |
| `403` | Autenticado sin `ROLE_ADMIN` |

---

## 5. `UserResponse` — shape completo

```json
{
  "id": 1,
  "username": "jdoe",
  "email": "jdoe@mail.com",
  "firstName": "John",
  "lastName": "Doe",
  "enabled": true,
  "active": true,
  "roles": ["ROLE_USER"]
}
```

| Campo | Tipo | Significado |
|-------|------|-------------|
| `id` | number (Long) | ID interno |
| `username` | string | Único |
| `email` | string | Único |
| `firstName` / `lastName` | string | |
| `enabled` | boolean | Cuenta habilitada (flag de la entidad) |
| `active` | boolean | `!deleted` — derivado del soft delete; siempre `true` en la práctica (los borrados se filtran) |
| `roles` | string[] | Nombres `ROLE_*` (los mismos del claim `roles` del JWT), orden alfabético. Puede ser vacío en datos antiguos sin rol |

---

## 6. Flujo de UI sugerido (derivado del contrato)

```
[Login] ──POST /auth/login──► guarda token + refreshToken + user → app
[Admin] ──POST /users (roles)──► OK → el usuario aparece en la lista de /usuarios
[Admin inicial] ──seed Flyway V6──► login con las credenciales del entorno
[Sesión expirada] 401 → intenta POST /auth/refresh → OK: reintenta · 401: borra tokens → Login
[Rol insuficiente] 403 → pantalla "Acceso denegado"
```

- El estado "quién soy" se reconstruye del `user` guardado en el login o decodificando
  el JWT (`sub`, `userId`, `roles`). Sin endpoint `/me`, ésa es la única fuente.
- Tras F5, el front recupera la sesión leyendo el refresh token persistido (si lo guardó)
  y llamando a `/auth/refresh` para obtener un par nuevo.

---

**Anterior:** `02-autenticacion-y-seguridad.md` · **Siguiente:** `04-contrato-api-deportistas.md`
