## ADDED Requirements

### Requirement: Emisión de refresh token en el login
El sistema SHALL emitir, junto al access token, un refresh token JWT firmado con HS256 en la respuesta de `POST /api/v1/auth/login`. El refresh token MUST incluir los claims `sub` (username), `userId` y `type` con valor `"refresh"`, y MUST tener una expiración independiente del access token, configurable mediante la propiedad `spring.security.jwt.refresh-expiration`.

#### Scenario: Login exitoso devuelve par de tokens
- **WHEN** un usuario se autentica con credenciales válidas en `POST /api/v1/auth/login`
- **THEN** la respuesta 200 incluye `token` (access), `refreshToken`, `type: "Bearer"`, `expiresIn` (expiración del access en ms) y `user`

#### Scenario: Refresh token expira según su propia configuración
- **WHEN** se inspeccionan los claims del refresh token emitido en el login
- **THEN** su expiración corresponde a `spring.security.jwt.refresh-expiration` (7 días por defecto) y no a la del access token

### Requirement: Endpoint de renovación de tokens
El sistema SHALL exponer `POST /api/v1/auth/refresh` como endpoint público (sin autenticación previa) que recibe `{ "refreshToken": "<jwt>" }` y, si el refresh token es válido (firma válida, no expirado, `type: "refresh"`, usuario existente y activo), devuelve un par nuevo de tokens con el mismo formato de respuesta del login.

#### Scenario: Refresh válido devuelve par nuevo
- **WHEN** se envía `POST /api/v1/auth/refresh` con un refresh token válido emitido previamente
- **THEN** la respuesta es 200 con un `token` (access) y `refreshToken` nuevos, distintos de los anteriores, más `type`, `expiresIn` y `user`

#### Scenario: Access regenerado con roles actuales del usuario
- **WHEN** se renueva la sesión de un usuario cuyos roles cambiaron desde el login
- **THEN** el nuevo access token refleja los roles actuales cargados desde la base de datos

### Requirement: Rechazo de refresh tokens inválidos
El sistema SHALL rechazar con 401 y el `ErrorResponse` de autenticación requerida (mismo contrato que el resto de la API) cualquier petición a `POST /api/v1/auth/refresh` cuyo refresh token sea inválido, esté expirado, no sea de tipo `"refresh"` o pertenezca a un usuario inexistente o inactivo.

#### Scenario: Refresh token expirado
- **WHEN** se envía `POST /api/v1/auth/refresh` con un refresh token cuya expiración ya pasó
- **THEN** la respuesta es 401 con `ErrorResponse` "Autenticación requerida"

#### Scenario: Access token usado como refresh
- **WHEN** se envía `POST /api/v1/auth/refresh` con un access token válido (sin `type: "refresh"`)
- **THEN** la respuesta es 401 con `ErrorResponse` "Autenticación requerida"

#### Scenario: Usuario eliminado o inactivo
- **WHEN** se envía `POST /api/v1/auth/refresh` con un refresh token válido de un usuario que ya no existe o está inactivo
- **THEN** la respuesta es 401 con `ErrorResponse` "Autenticación requerida"

### Requirement: Rechazo de refresh tokens como credencial de acceso
El filtro de autenticación SHALL rechazar cualquier petición a endpoints protegidos que presente un refresh token (claim `type: "refresh"`) en la cabecera `Authorization`, respondiendo 401 con el `ErrorResponse` de autenticación requerida.

#### Scenario: Refresh token usado como access token
- **WHEN** se invoca un endpoint protegido con `Authorization: Bearer <refreshToken>`
- **THEN** la respuesta es 401 con `ErrorResponse` "Autenticación requerida"

#### Scenario: Access token sin claim type (transición de despliegue)
- **WHEN** se invoca un endpoint protegido con un access token válido emitido antes del despliegue (sin claim `type`)
- **THEN** la petición se autentica con normalidad mientras el token no haya expirado
