This file is a merged representation of the entire codebase, combined into a single document by Repomix.

# File Summary

## Purpose
This file contains a packed representation of the entire repository's contents.
It is designed to be easily consumable by AI systems for analysis, code review,
or other automated processes.

## File Format
The content is organized as follows:
1. This summary section
2. Repository information
3. Directory structure
4. Repository files (if enabled)
5. Multiple file entries, each consisting of:
  a. A header with the file path (## File: path/to/file)
  b. The full contents of the file in a code block

## Usage Guidelines
- This file should be treated as read-only. Any changes should be made to the
  original repository files, not this packed version.
- When processing this file, use the file path to distinguish
  between different files in the repository.
- Be aware that this file may contain sensitive information. Handle it with
  the same level of security as you would the original repository.

## Notes
- Some files may have been excluded based on .gitignore rules and Repomix's configuration
- Binary files are not included in this packed representation. Please refer to the Repository Structure section for a complete list of file paths, including binary files
- Files matching patterns in .gitignore are excluded
- Files matching default ignore patterns are excluded
- Files are sorted by Git change count (files with more changes are at the bottom)

# Directory Structure
````
.claude/
  agents/
    athlete-domain.md
    backend-architect.md
    checkup-domain.md
    quality-guardian.md
    report-domain.md
    training-domain.md
    user-security.md
  commands/
    opsx/
      apply.md
      archive.md
      explore.md
      propose.md
      sync.md
      update.md
  skills/
    openspec-apply-change/
      SKILL.md
    openspec-archive-change/
      SKILL.md
    openspec-explore/
      SKILL.md
    openspec-propose/
      SKILL.md
    openspec-sync-specs/
      SKILL.md
    openspec-update-change/
      SKILL.md
.mvn/
  wrapper/
    maven-wrapper.properties
    maven-wrapper.properties:Zone.Identifier
core/
  principles.md
docs/
  flyway.md
openspec/
  config.yaml
scripts/
  flyway.sh
  init-local.sh
src/
  main/
    java/
      com/
        athletecore/
          api/
            common/
              exception/
                AccessDeniedException.java
                DuplicateResourceException.java
                ErrorResponse.java
                GlobalExceptionHandler.java
                InternalServerErrorException.java
                ResourceNotFoundException.java
                ValidationException.java
            config/
              SecurityConfig.java
            domain/
              BaseEntity.java
              Role.java
              User.java
            user/
              dto/
                CreateUserRequest.java
              RoleRepository.java
              UserController.java
              UserRepository.java
              UserService.java
            AthletecoreApiApplication.java
            AthletecoreApiApplication.java:Zone.Identifier
    resources/
      db/
        migration/
          V1__initial_schema.sql
      application.properties
      application.properties:Zone.Identifier
  test/
    java/
      com/
        athletecore/
          api/
            user/
              UserServiceTest.java
            AthletecoreApiApplicationTests.java
            AthletecoreApiApplicationTests.java:Zone.Identifier
.dockerignore
.env.example
.gitattributes
.gitattributes:Zone.Identifier
.gitignore
.gitignore:Zone.Identifier
CONFIGURATION_GUIDE.md
docker-compose.yml
Dockerfile
HELP.md:Zone.Identifier
mvnw
mvnw:Zone.Identifier
mvnw.cmd
mvnw.cmd:Zone.Identifier
pom.xml
pom.xml:Zone.Identifier
README.md
````

# Files

## File: core/principles.md
````markdown
# Principios de Diseño - AthleteCore API

## Principios Fundamentales

Este documento define los principios que guían el diseño y desarrollo de AthleteCore API.

### 1. Seguridad primero

- **Cero credenciales hardcodeadas**: Todos los secrets deben venir de variables de entorno
- **Autenticación JWT**: Stateless, tokens con expiración definida
- **RBAC (Role-Based Access Control)**: Todos los endpoints sensibles deben tener protección

### 2. Arquitectura limpia

- **Hexagonal Architecture**: Separación clara entre dominio, aplicación e infraestructura
- **Entities-first**: Las entidades JPA son la fuente de verdad del dominio
- **No exponer entidades**: DTOs nunca deben exponer entidades JPA directamente

### 3. Base de datos

- **Flyway**: Todas las migraciones deben ser versionadas y reversibles
- **No modificar esquema en runtime**: `ddl-auto` nunca `create` o `update` fuera de desarrollo inicial
- **Soft delete**: Todas las entidades deben extender `BaseEntity` con `@SQLRestriction`

### 4. Configuración

- **Variables de entorno**: Usar `${VAR:fallback}` para flexibilidad
- **Perfiles de Spring**: `dev`, `docker`, `prod` según el entorno
- **Configuración local never versionada**: `**/application-*.properties` en gitignore

### 5. Logging y monitoreo

- **Logging estructurado**: JSON en producción, texto en desarrollo
- **Métricas activadas**: `/actuator/metrics` siempre disponible
- **Health checks**: `/actuator/health` para orquestación

### 6. Testing

- **Test strategy**: Unit tests > Integration tests > E2E
- **Coverage mínimo**: 80% en lógica de negocio crítica
- **CI/CD integration**: Tests deben correr en pipeline antes de merge

### 7. API Design

- **RESTful**: Endpoints claros y verbosos en paths, nouns en recursos
- **Versioning**: `/api/v1/` como prefix para versionamiento
- **Consistent error handling**: `@RestControllerAdvice` centralizado

---

## Convenciones de Código

### Nombres de Entidades

```java
// ✅ Correcto
public class Athlete implements BaseEntity
public class TrainingSession implements BaseEntity
public class Checkup implements BaseEntity

// ❌ Incorrecto
public class AthleteEntity implements BaseEntity
public class Training implements BaseEntity
```

### DTOs

```java
// ✅ Correcto - DTOs separados de entidades
public record AthleteDto(
    Long id,
    String name,
    AthleteProfileDto profile
) {}

// ❌ Incorrecto - Usar entidad directamente
public record AthleteResponse(Athlete athlete) {}
```

### Service Layers

```java
// ✅ Correcto - Servicios enfocados en una tarea
public class AthleteRegistrationService
public class AthleteProfileService
public class AthleteReportingService

// ❌ Incorrecto - Dioses Services
public class SuperAthleteService {
    // Haces todo con el atleta aquí
}
```

### Repositorios

```java
// ✅ Correcto - Métodos con nombres descriptivos
public interface AthleteRepository extends JpaRepository<Athlete, Long> {
    @Query("SELECT a FROM Athlete a WHERE a.nationality = :nationality AND a.deletedAt IS NULL")
    List<Athlete> findByNationality(@Param("nationality") String nationality);
}

// ❌ Incorrecto
public interface AthleteRepository {
    void doSomething(Long id);
}
```

---

## Configuración de Entornos

### Development (`application.properties`)

```properties
# Valores de fallback seguros
spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:5432/athletecore}
spring.jpa.hibernate.ddl-auto=validate
logging.level.com.athletecore=DEBUG
```

### Docker Development

```properties
# Same as dev, pero con vars de entorno de docker-compose
spring.datasource.url=${DB_URL}
```

### Production (`application-prod.properties`)

```properties
# NO fallbacks - todo debe estar configurado
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USER}
spring.datasource.password=${DB_PASSWORD}
spring.jpa.hibernate.ddl-auto=validate
logging.level.com.athletecore=WARN
```

---

## Revisión de PR

Antes de merge, verificar:

- [ ] No hay credenciales hardcodeadas
- [ ] Entities extienden `BaseEntity`
- [ ] Migraciones Flyway versionadas
- [ ] DTOs usados en responses
- [ ] Endpoints protegidos con `@PreAuthorize`
- [ ] Tests unitarios agregados
- [ ] Logging sin información sensible

---

## Referencias

- [Spring Boot Configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config)
- [Postgres Best Practices](https://www.cybertec-postgresql.com/en/top-10-postgresql-best-practices/)
- [JWT Best Practices](https://auth0.com/blog/best-practices-for-jwt-in-web-applications/)
````

## File: docs/flyway.md
````markdown
# Configuración Flyway - AthleteCore API

## Resumen

Este documento describe la configuración de Flyway para la gestión de migraciones de base de datos en AthleteCore API.

## Estructura de archivos

```
src/main/resources/db/migration/
└── V1__initial_schema.sql    # Primera migración: roles, users, user_roles
```

## Migración V1: Schema Inicial

**Archivo**: `V1__initial_schema.sql`

### Tablas creadas

| Tabla | Descripción |
|-------|-------------|
| `roles` | Roles del sistema (ADMIN, USER, TRAINER, COACH) |
| `users` | Usuarios del sistema con información de autenticación |
| `user_roles` | Tabla intermedia Many-to-Many entre usuarios y roles |

### Características

- **Soft Delete**: Todas las tablas incluyen `deleted_at` para soporte de elimiación lógica
- **Auditoría**: `created_at` y `updated_at` en todas las tablas
- **Triggers**: Actualización automática de `updated_at`
- **Índices**: Índices optimizados para consultas frecuentes
- **Comentarios**: Comentarios SQL para documentación del esquema

### Datos de referencia

La migración incluye:
- 4 roles por defecto: `ROLE_ADMIN`, `ROLE_USER`, `ROLE_TRAINER`, `ROLE_COACH`
- 1 usuario administrador por defecto: `admin` (password: `admin123`)
- Asignación automática del rol ADMIN al usuario admin

## Configuración

### application.properties (Producción)

```properties
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=false
spring.flyway.locations=classpath:db/migration
spring.flyway.validate-on-migrate=true
```

### application-local.properties (Desarrollo)

```properties
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=true      # Solo primera vez
spring.flyway.validate-on-migrate=true
spring.flyway.out-of-order=true
```

## Uso

### Ejecutar migraciones

Con Spring Boot:
```bash
# Iniciar la aplicación ejecutará Flyway automáticamente
./mvnw spring-boot:run
# o
java -jar target/athletecore-api-0.0.1-SNAPSHOT.jar
```

Con script de utilidad:
```bash
 ./scripts/flyway.sh migrate
```

### Ver estado de migraciones

```bash
# Con script:
./scripts/flyway.sh info

# Con Spring Boot (endpoint):
curl http://localhost:8080/actuator/health
```

### Crear baseline (primera vez)

Solo necesario la primera vez si no hay tablas existentes:
```bash
./scripts/flyway.sh baseline 1
```

### Validar migraciones

```bash
./scripts/flyway.sh validate
```

## Variables de entorno

| Variable | Descripción | Default |
|----------|-------------|---------|
| `DB_URL` | URL de conexión PostgreSQL | `jdbc:postgresql://localhost:5432/athletecore` |
| `DB_USER` | Usuario de PostgreSQL | `postgres` |
| `DB_PASSWORD` | Contraseña de PostgreSQL | `postgres` |

## Consideraciones importantes

### Baseline
- La primera vez que se inicia la aplicación, `baseline-on-migrate=true` permite que Flyway cree la base de datos
- Después de la inicialización, cambiar a `false` para que no se cree una nueva baseline
- En Docker Compose, este valor se configura automáticamente

### Producción
- NUNCA usar `ddl-auto=create` ni `ddl-auto=update` en producción
- Solo validar esquema con `ddl-auto=validate`
- Las migraciones se ejecutan automáticamente al iniciar Spring Boot

### Seguridad
- Las credenciales de base de datos se cargan desde variables de entorno
- Nunca hardcodear credenciales en archivos de configuración
- Usar `.env` o variables de entorno para valores sensibles

## Próximas migraciones

Según el roadmap del proyecto, las siguientes migraciones incluirán:

- **V2__athletes_schema**: Entidades de deportistas
- **V3__training_schema**: Entidades de entrenamientos
- **V4__checkup_schema**: Entidades de chequeos mensuales
- **V5__reports_schema**: Entidades de reportes

Cada migración será creada por los agentes especializados correspondientes.

## Referencias

- [Flyway Documentation](https://flywaydb.org/documentation/)
- [Spring Boot Flyway](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#data.sql.datasource.flyway)
- [PostgreSQL Best Practices](https://www.postgresql.org/docs/current/tutorial.html)
````

## File: scripts/flyway.sh
````bash
#!/bin/bash
# ============================================================================
# ATHLETECORE API - SCRIPTS DE UTILIDAD PARA FLYWAY
# ============================================================================
#
# Uso:
#   ./scripts/flyway.sh migrate          # Ejecutar migraciones
#   ./scripts/flyway.sh info             # Ver estado de migraciones
#   ./scripts/flyway.sh validate         # Validar scripts
#   ./scripts/flyway.sh baseline [version] # Crear baseline (primera vez)
#
# ============================================================================

set -e

# Configuración basada en variables de entorno o defaults
DB_URL="${DB_URL:-jdbc:postgresql://localhost:5432/athletecore}"
DB_USER="${DB_USER:-postgres}"
DB_PASSWORD="${DB_PASSWORD:-postgres}"
FLYWAY_URL="${FLYWAY_URL:-http://localhost:8080}"

# Obtener directorio del script
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

echo "=========================================="
echo "AthleteCore API - Flyway Utility Script"
echo "=========================================="
echo "DB_URL: $DB_URL"
echo "DB_USER: $DB_USER"
echo

# Verificar que postgres esté corriendo
check_postgres() {
    echo "Verificando conexión a PostgreSQL..."
    if ! pg_isready -h localhost -p 5432 -U "$DB_USER" > /dev/null 2>&1; then
        echo "ERROR: PostgreSQL no está disponible. ¿Está corriendo?"
        echo
        echo "Para iniciar PostgreSQL con Docker Compose:"
        echo "  docker-compose up -d postgres"
        exit 1
    fi
    echo "PostgreSQL está disponible."
    echo
}

# Baseline inicial (solo para primera vez)
do_baseline() {
    local version="${1:-1}"
    echo "Creando baseline en versión $version..."

    export FLYWAY_BASELINE_VERSION="$version"
    export FLYWAY_BASELINE_ON_MIGRATE=true

    flyway \
        -url="$DB_URL" \
        -user="$DB_USER" \
        -password="$DB_PASSWORD" \
        -locations="filesystem:$PROJECT_ROOT/src/main/resources/db/migration" \
        baseline
}

# Ejecutar migraciones
do_migrate() {
    echo "Ejecutando migraciones..."

    flyway \
        -url="$DB_URL" \
        -user="$DB_USER" \
        -password="$DB_PASSWORD" \
        -locations="filesystem:$PROJECT_ROOT/src/main/resources/db/migration" \
        migrate
}

# Mostrar información de migraciones
do_info() {
    echo "Información de migraciones..."
    echo

    flyway \
        -url="$DB_URL" \
        -user="$DB_USER" \
        -password="$DB_PASSWORD" \
        -locations="filesystem:$PROJECT_ROOT/src/main/resources/db/migration" \
        info
}

# Validar scripts
do_validate() {
    echo "Validando scripts de migración..."

    flyway \
        -url="$DB_URL" \
        -user="$DB_USER" \
        -password="$DB_PASSWORD" \
        -locations="filesystem:$PROJECT_ROOT/src/main/resources/db/migration" \
        validate
}

# Limpiar (¡CUIDADO! Solo para desarrollo)
do_clean() {
    echo "ADVERTENCIA: Esto borrará todas las tablas y datos!"
    read -p "¿Estás seguro? (yes/no): " confirm
    if [ "$confirm" = "yes" ]; then
        echo "Limpiando base de datos..."
        flyway \
            -url="$DB_URL" \
            -user="$DB_USER" \
            -password="$DB_PASSWORD" \
            -locations="filesystem:$PROJECT_ROOT/src/main/resources/db/migration" \
            clean
    else
        echo "Cancelado."
        exit 0
    fi
}

# Mostrar ayuda
show_help() {
    echo "Usos:"
    echo "  $0 migrate          - Ejecutar migraciones pendientes"
    echo "  $0 info             - Ver estado de migraciones"
    echo "  $0 validate         - Validar scripts de migración"
    echo "  $0 baseline [v]     - Crear baseline (v = versión, default: 1)"
    echo "  $0 clean            - Limpiar base de datos (¡CUIDADO!)"
    echo
    echo "Variables de entorno:"
    echo "  DB_URL      - URL de conexión a PostgreSQL"
    echo "  DB_USER     - Usuario de PostgreSQL"
    echo "  DB_PASSWORD - Contraseña de PostgreSQL"
    echo
    echo "Ejemplos:"
    echo "  ./scripts/flyway.sh migrate"
    echo "  DB_USER=admin DB_PASSWORD=secret ./scripts/flyway.sh validate"
}

# Principal
case "${1:-help}" in
    migrate)
        check_postgres
        do_migrate
        ;;
    info)
        check_postgres
        do_info
        ;;
    validate)
        check_postgres
        do_validate
        ;;
    baseline)
        check_postgres
        do_baseline "${2:-1}"
        ;;
    clean)
        do_clean
        ;;
    help|--help|-h)
        show_help
        ;;
    *)
        echo "ERROR: Comando desconocido: $1"
        echo
        show_help
        exit 1
        ;;
esac

echo
echo "Completado exitosamente."
````

## File: scripts/init-local.sh
````bash
#!/bin/bash

# ============================================================================
# ATHLETECORE API - Script de Inicialización Local
# ============================================================================
#
# Este script automatiza la configuración inicial del entorno de desarrollo.
#
# Requisitos:
# - Docker Compose disponible
# - PostgreSQL accesible (desde Docker o nativo)
#
# ============================================================================

set -e

# Colores para salida
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Variables de configuración
DB_NAME="${DB_NAME:-athletecore}"
DB_USER="${DB_USER:-postgres}"
DB_PASSWORD="${DB_PASSWORD:-postgres}"
DB_URL="jdbc:postgresql://localhost:5432/${DB_NAME}"
JWT_SECRET="${JWT_SECRET:-$(openssl rand -hex 32)}"
SERVER_PORT="${SERVER_PORT:-8080}"

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}  AthleteCore API - Inicialización Local${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""

# Función para verificar comandos
check_command() {
    if ! command -v "$1" &> /dev/null; then
        echo -e "${RED}ERROR: $1 no está instalado.${NC}"
        exit 1
    fi
}

# Verificar dependencias
echo -e "${YELLOW}Verificando dependencias...${NC}"
check_command docker
check_command docker-compose

echo -e "${GREEN}✓ Dependencias verificadas${NC}"
echo ""

# Generar archivo .env
echo -e "${YELLOW}Generando archivo .env${NC}"
cat > .env << EOF
# AthleteCore API - Variables de Entorno Local
# Este archivo no debe ser versionado en Git

# Base de Datos
DB_URL=${DB_URL}
DB_USER=${DB_USER}
DB_PASSWORD=${DB_PASSWORD}

# JWT Configuration
JWT_SECRET=${JWT_SECRET}
JWT_EXPIRATION=86400000

# Server Configuration
SERVER_PORT=${SERVER_PORT}

# CORS Configuration (ajustar según tu frontend)
CORS_ORIGINS=http://localhost:3000,http://localhost:5173,http://localhost:4200
EOF

echo -e "${GREEN}✓ Archivo .env creado${NC}"
echo ""

# Verificar si PostgreSQL está corriendo
echo -e "${YELLOW}Verificando PostgreSQL...${NC}"
if docker ps | grep -q "postgres"; then
    echo -e "${GREEN}✓ PostgreSQL está corriendo en Docker${NC}"
else
    echo -e "${YELLOW}⚠ PostgreSQL no detectado en Docker${NC}"
    echo -e "${YELLOW}Intentando verificar instalación nativa...${NC}"

    if command -v psql &> /dev/null; then
        if psql -U postgres -c "\l" > /dev/null 2>&1; then
            echo -e "${GREEN}✓ PostgreSQL nativo detectado${NC}"
        else
            echo -e "${RED}⚠ No se pudo conectar a PostgreSQL nativo${NC}"
            echo -e "${YELLOW}sugerencia: docker-compose up -d db${NC}"
        fi
    else
        echo -e "${YELLOW}⚠ PostgreSQL nativo no detectado${NC}"
        echo -e "${YELLOW}sugerencia: docker-compose up -d db${NC}"
    fi
fi
echo ""

# Crear base de datos si no existe
echo -e "${YELLOW}Configurando base de datos${NC}"
if command -v psql &> /dev/null; then
    # Intentar crear base de datos nativa
    psql -U postgres -c "CREATE DATABASE ${DB_NAME};" 2>/dev/null || \
    echo -e "${GREEN}✓ Base de datos '${DB_NAME}' existe o ya creada${NC}"
else
    # Intentar con Docker
    docker exec -it $(docker ps --filter "name=postgres" --format "{{.ID}}") \
        psql -U postgres -c "CREATE DATABASE ${DB_NAME};" 2>/dev/null || \
        echo -e "${GREEN}✓ Intentando crear base de datos${NC}"
fi
echo ""

# Iniciar servicios si no están corriendo
echo -e "${YELLOW}Iniciando servicios...${NC}"
if ! docker-compose ps | grep -q "Up"; then
    echo -e "${GREEN}Iniciando servicios con docker-compose...${NC}"
    docker-compose up -d
else
    echo -e "${GREEN}✓ Servicios ya están corriendo${NC}"
fi
echo ""

# Esperar a que PostgreSQL esté listo
echo -e "${YELLOW}Esperando a que PostgreSQL esté listo...${NC}"
retry=0
max_retry=30
until docker exec $(docker ps --filter "name=postgres" --format "{{.ID}}") \
    pg_isready -U ${DB_USER} > /dev/null 2>&1 || [ $retry -eq $max_retry ]; do
    sleep 2
    retry=$((retry + 1))
done

if [ $retry -eq $max_retry ]; then
    echo -e "${RED}ERROR: PostgreSQL no se pudo conectar después de ${max_retry} intentos${NC}"
    exit 1
fi

echo -e "${GREEN}✓ PostgreSQL está listo${NC}"
echo ""

# Ejecutar migraciones Flyway
echo -e "${YELLOW}Ejecutando migraciones Flyway...${NC}"
./mvnw flyway:migrate
echo ""

# Resumir configuración
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}  Configuración Completada Con Éxito${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo "Variables configuradas en .env:"
echo "  - DB_URL:        ${DB_URL}"
echo "  - DB_USER:       ${DB_USER}"
echo "  - DB_PASSWORD:   ***"
echo "  - JWT_SECRET:    ${JWT_SECRET}"
echo "  - SERVER_PORT:   ${SERVER_PORT}"
echo ""
echo "Servicios corriendo:"
docker-compose ps
echo ""
echo -e "${YELLOW}Próximos pasos:${NC}"
echo "  1. Ejecutar la aplicación: ./mvnw spring-boot:run"
echo "  2. Verificar que la API esté disponible: curl http://localhost:${SERVER_PORT}/actuator/health"
echo "  3. Revisar la documentación en CONFIGURATION_GUIDE.md"
echo ""
````

## File: src/main/java/com/athletecore/api/common/exception/AccessDeniedException.java
````java
package com.athletecore.api.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class AccessDeniedException extends RuntimeException {

    public AccessDeniedException(String message) {
        super(message);
    }

    public AccessDeniedException() {
        super("Access denied. You do not have permission to perform this action.");
    }
}
````

## File: src/main/java/com/athletecore/api/common/exception/DuplicateResourceException.java
````java
package com.athletecore.api.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }

    public DuplicateResourceException(String resourceName, String fieldName) {
        super(String.format("%s with %s already exists", resourceName, fieldName));
    }
}
````

## File: src/main/java/com/athletecore/api/common/exception/ErrorResponse.java
````java
package com.athletecore.api.common.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private final LocalDateTime timestamp;

    private final int status;

    private final String error;

    private final String message;

    private final String details;

    public static ErrorResponseBuilder builder() {
        return ErrorResponse.builder()
                .timestamp(LocalDateTime.now());
    }
}
````

## File: src/main/java/com/athletecore/api/common/exception/GlobalExceptionHandler.java
````java
package com.athletecore.api.common.exception;

import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Manejo de recursos no encontrados
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex) {
        logger.warn("Recurso no encontrado: {}", ex.getMessage());
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.NOT_FOUND.value())
                .error(HttpStatus.NOT_FOUND.getReasonPhrase())
                .message(ex.getMessage())
                .build();
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    // Manejo de validación de datos
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        String validationErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));

        logger.warn("Error de validación: {}", validationErrors);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message("Error de validación")
                .details(validationErrors)
                .build();
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(ValidationException ex) {
        String details = ex.getValidationErrors() != null
                ? String.join(", ", ex.getValidationErrors())
                : ex.getMessage();

        logger.warn("Error de validación: {}", details);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(ex.getMessage())
                .details(details)
                .build();
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    // Manejo de recursos duplicados
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResourceException(DuplicateResourceException ex) {
        logger.warn("Recurso duplicado: {}", ex.getMessage());
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.CONFLICT.value())
                .error(HttpStatus.CONFLICT.getReasonPhrase())
                .message(ex.getMessage())
                .build();
        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    // Manejo de acceso denegado
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex) {
        logger.warn("Acceso denegado: {}", ex.getMessage());
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.FORBIDDEN.value())
                .error(HttpStatus.FORBIDDEN.getReasonPhrase())
                .message(ex.getMessage() != null ? ex.getMessage() : "Acceso denegado")
                .build();
        return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
    }

    // Manejo de AccessDeniedException de Spring Security (versión por defecto)
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleSpringAccessDeniedException(
            org.springframework.security.access.AccessDeniedException ex) {
        logger.warn("Acceso denegado por Spring Security");
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.FORBIDDEN.value())
                .error(HttpStatus.FORBIDDEN.getReasonPhrase())
                .message("No tiene permisos para acceder a este recurso")
                .build();
        return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
    }

    // Manejo de excepciones personalizadas
    @ExceptionHandler(InternalServerErrorException.class)
    public ResponseEntity<ErrorResponse> handleInternalServerErrorException(InternalServerErrorException ex) {
        logger.error("Error interno del servidor: {}", ex.getMessage(), ex);
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .message("Ocurrió un error interno en el servidor")
                .build();
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // Manejo genérico de Exceptions no capturadas
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception ex) {
        logger.error("Excepción no manejada: {}", ex.getMessage(), ex);
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .message("Ocurrió un error inesperado")
                .build();
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // Manejo de exepciones de tipo RUNTIME
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex) {
        logger.error("Runtimeexception no manejada: {}", ex.getMessage(), ex);
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .message("Ocurrió un error inesperado")
                .build();
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // Manejo de lectura de mensaje HTTP inválida
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        logger.warn("Mensaje HTTP no legible: {}", ex.getMessage());
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message("El cuerpo de la solicitud no es válido")
                .build();
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    // Manejo de rutas no encontradas (404 para endpoints inexistentes)
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFoundException(NoResourceFoundException ex) {
        logger.warn("Ruta no encontrada: {}", ex.getResourcePath());
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.NOT_FOUND.value())
                .error(HttpStatus.NOT_FOUND.getReasonPhrase())
                .message("Endpoint no encontrado")
                .build();
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }
}
````

## File: src/main/java/com/athletecore/api/common/exception/InternalServerErrorException.java
````java
package com.athletecore.api.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class InternalServerErrorException extends RuntimeException {

    public InternalServerErrorException(String message) {
        super(message);
    }

    public InternalServerErrorException(String message, Throwable cause) {
        super(message, cause);
    }
}
````

## File: src/main/java/com/athletecore/api/common/exception/ResourceNotFoundException.java
````java
package com.athletecore.api.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s not found with %s: '%s'", resourceName, fieldName, fieldValue));
    }
}
````

## File: src/main/java/com/athletecore/api/common/exception/ValidationException.java
````java
package com.athletecore.api.common.exception;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class ValidationException extends RuntimeException {

    private final List<String> validationErrors;

    public ValidationException(String message) {
        super(message);
        this.validationErrors = null;
    }

    public ValidationException(List<String> errors) {
        super("Validation failed");
        this.validationErrors = errors;
    }

    public ValidationException(String message, List<String> errors) {
        super(message);
        this.validationErrors = errors;
    }

    public List<String> getValidationErrors() {
        return validationErrors;
    }
}
````

## File: src/main/resources/db/migration/V1__initial_schema.sql
````sql
-- ============================================================================
-- MIGRACIÓN INICIAL: Esquema base de AthleteCore API
-- ============================================================================
-- Versión: V1
-- Descrição: Crea el esquema inicial con tablas de roles, usuarios y entidades
--            base extendiendo con auditoría y soft delete.
-- Dependencias: Ninguna (primera migración)
-- ============================================================================

-- ============================================================================
-- TABLA: roles
-- Descripción: Tabla maestra de roles para el sistema deRBAC (Role-Based Access Control)
--              Los roles definen permisos y asignaciones de usuarios.
-- ============================================================================
CREATE TABLE roles (
    id              BIGSERIAL PRIMARY KEY,           -- Identificador único autoincremental
    name            VARCHAR(50) NOT NULL UNIQUE,     -- Nombre del rol (e.g., ADMIN, USER, TRAINER)

    -- Campos de auditoría (heredados de BaseEntity)
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at      TIMESTAMP WITH TIME ZONE         -- Soft delete timestamp

    -- Restricciones para soft delete
    CONSTRAINT chk_roles_deleted CHECK (deleted_at IS NULL OR deleted_at > '1900-01-01')
);

-- Índice único para nombre de rol
CREATE UNIQUE INDEX idx_roles_name ON roles(name) WHERE deleted_at IS NULL;

-- ============================================================================
-- TABLA: users
-- Descripción: Tabla principal de usuarios del sistema.
--              Contiene información de autenticación y perfil del usuario.
--              Extiende BaseEntity con campos created_at, updated_at, deleted_at.
-- ============================================================================
CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,                    -- Identificador único autoincremental
    username        VARCHAR(255) NOT NULL UNIQUE,             -- Nombre de usuario único
    email           VARCHAR(255) NOT NULL UNIQUE,             -- Correo electrónico único
    password        VARCHAR(255) NOT NULL,                     -- Contraseña hasheada (BCrypt)
    first_name      VARCHAR(100) NOT NULL,                    -- Nombre
    last_name       VARCHAR(100) NOT NULL,                    -- Apellido
    enabled         BOOLEAN NOT NULL DEFAULT TRUE,            -- Estado del usuario (activo/inactivo)

    -- Campos de auditoría (heredados de BaseEntity)
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at      TIMESTAMP WITH TIME ZONE                  -- Soft delete timestamp

    -- Restricciones para soft delete
    CONSTRAINT chk_users_deleted CHECK (deleted_at IS NULL OR deleted_at > '1900-01-01')
);

-- Índices para búsqueda eficiente de usuarios
CREATE INDEX idx_users_username ON users(username) WHERE deleted_at IS NULL;
CREATE INDEX idx_users_email ON users(email) WHERE deleted_at IS NULL;
CREATE INDEX idx_users_enabled ON users(enabled) WHERE deleted_at IS NULL;
CREATE INDEX idx_users_fullname ON users(last_name, first_name) WHERE deleted_at IS NULL;

-- ============================================================================
-- TABLA: user_roles (Tabla intermedia Many-to-Many)
-- Descripción: Asocia usuarios con roles (relación muchos-a-muchos).
--              Permite que un usuario tenga múltiples roles y que un rol
--              sea asignado a múltiples usuarios.
-- ============================================================================
CREATE TABLE user_roles (
    user_id         BIGINT NOT NULL,
    role_id         BIGINT NOT NULL,

    -- Campo para auditoría de la asignación
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (user_id, role_id),

    -- Foreign Keys con referencias en cascada
    CONSTRAINT fk_user_roles_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role
        FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);

-- Índices para consultas frecuentes
CREATE INDEX idx_user_roles_user_id ON user_roles(user_id);
CREATE INDEX idx_user_roles_role_id ON user_roles(role_id);

-- ============================================================================
-- VISTAS / FUNCIONES DE AYUDA (Opcional pero recomendadas)
-- ============================================================================

-- Función para actualizar automáticamente el field updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Trigger para actualizar updated_at en roles
CREATE TRIGGER update_roles_updated_at
    BEFORE UPDATE ON roles
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Trigger para actualizar updated_at en users
CREATE TRIGGER update_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Trigger para actualizar updated_at en user_roles
CREATE TRIGGER update_user_roles_updated_at
    BEFORE UPDATE ON user_roles
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- DATOS DE REFERENCIA INICIALES
-- ============================================================================

-- Roles por defecto del sistema
INSERT INTO roles (name, created_at, updated_at) VALUES
    ('ROLE_ADMIN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('ROLE_USER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('ROLE_TRAINER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('ROLE_COACH', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (name) DO NOTHING;

-- Usuario administrador por defecto
-- Password por defecto: 'admin123' (BCrypt hash correspondiente)
-- NOTA: En producción, cambiar esta contraseña inmediatamente después del primer login
INSERT INTO users (username, email, password, first_name, last_name, enabled, created_at, updated_at)
VALUES (
    'admin',
    'admin@athletecore.com',
    '$2a$10$NzFzYWJ5c2VjcmV0a2V5aGVyZXRvZW5jb2RlZHBhc3N3b3Jk',  -- Hash BCrypt de 'admin123'
    'Sistema',
    'Administrador',
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (username) DO NOTHING
ON CONFLICT (email) DO NOTHING;

-- Asignar rol ADMIN al usuario admin por defecto
INSERT INTO user_roles (user_id, role_id, created_at, updated_at)
SELECT u.id, r.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM users u, roles r
WHERE u.username = 'admin' AND r.name = 'ROLE_ADMIN'
ON CONFLICT (user_id, role_id) DO NOTHING;

-- ============================================================================
-- COMENTARIOS DOCUMENTALES (PostgreSQL Comments)
-- ============================================================================

COMMENT ON TABLE roles IS 'Tabla maestra de roles para control de acceso basado en roles (RBAC).';
COMMENT ON TABLE users IS 'Tabla principal de usuarios de la plataforma AthleteCore.';
COMMENT ON TABLE user_roles IS 'Tabla intermedia para relación molti-a-muchos entre usuarios y roles.';

COMMENT ON COLUMN roles.id IS 'Identificador único autoincremental del rol.';
COMMENT ON COLUMN roles.name IS 'Nombre único del rol. Valores permitidos: ROLE_ADMIN, ROLE_USER, ROLE_TRAINER, ROLE_COACH.';
COMMENT ON COLUMN roles.created_at IS 'Fecha y hora de creación del registro.';
COMMENT ON COLUMN roles.updated_at IS 'Fecha y hora de la última actualización.';
COMMENT ON COLUMN roles.deleted_at IS 'Fecha de eliminación lógica (soft delete). NULL = activo.';

COMMENT ON COLUMN users.id IS 'Identificador único autoincremental del usuario.';
COMMENT ON COLUMN users.username IS 'Nombre de usuario único para autenticación.';
COMMENT ON COLUMN users.email IS 'Correo electrónico único del usuario.';
COMMENT ON COLUMN users.password IS 'Contraseña hasheada usando BCrypt.';
COMMENT ON COLUMN users.first_name IS 'Nombre del usuario.';
COMMENT ON COLUMN users.last_name IS 'Apellido del usuario.';
COMMENT ON COLUMN users.enabled IS 'Indicador de estado activo (TRUE) o inactivo (FALSE).';
COMMENT ON COLUMN users.created_at IS 'Fecha y hora de creación del usuario.';
COMMENT ON COLUMN users.updated_at IS 'Fecha y hora de la última actualización.';
COMMENT ON COLUMN users.deleted_at IS 'Fecha de eliminación lógica (soft delete). NULL = activo.';

COMMENT ON COLUMN user_roles.user_id IS 'Identificador del usuario asociado (clave foránea a users).';
COMMENT ON COLUMN user_roles.role_id IS 'Identificador del rol asociado (clave foránea a roles).';

-- ============================================================================
-- NOTAS PARA FUTURAS MIGRACIONES
-- ============================================================================
-- Próximas tablas a implementar (según roadmaps de módulos):
--
-- MÓDULO DEPORTISTAS (athlete):
--   - athletes (información básica del deportista)
--   - athlete_profiles (datos antropométricos)
--   - athlete_photos (fotos de perfil)
--
-- MÓDULO DE ENTRENAMIENTOS (training):
--   - training_sessions (sesiones de entrenamiento individuales)
--   - training_plans (planes de entrenamiento)
--   - training_cycles (ciclos: micro, meso, anual)
--
-- MÓDULO DE CHEQUEOS (checkup):
--   - checkups (registros de chequeos mensuales)
--   - checkup_times (tiempos de prueba registrados)
--   - reference_tables (tablas de referencia nacionales)
--
-- MÓDULO DE REPORTES (report):
--   - reports (generación de reportes)
--   - report_schedules (programación de reportes)
--
-- ============================================================================
````

## File: src/test/java/com/athletecore/api/user/UserServiceTest.java
````java
package com.athletecore.api.user;

import com.athletecore.api.common.exception.DuplicateResourceException;
import com.athletecore.api.domain.Role;
import com.athletecore.api.domain.User;
import com.athletecore.api.user.dto.CreateUserRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private CreateUserRequest createUserRequest;
    private Role userRole;
    private User newUser;

    @BeforeEach
    void setUp() {
        createUserRequest = new CreateUserRequest();
        createUserRequest.setUsername("testuser");
        createUserRequest.setEmail("test@example.com");
        createUserRequest.setPassword("password123");
        createUserRequest.setConfirmPassword("password123");

        userRole = Role.builder()
                .id(1L)
                .name("ROLE_USER")
                .build();

        newUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .password("encodedPassword")
                .firstName("Test")
                .lastName("User")
                .enabled(true)
                .roles(Collections.singleton(userRole))
                .build();
    }

    @Test
    @DisplayName("Debe crear usuario exitosamente")
    void createUser_Success() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(userRole));
        when(userRepository.save(any(User.class))).thenReturn(newUser);

        // Act
        User result = userService.createUser(createUserRequest);

        // Assert
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("test@example.com", result.getEmail());
        assertEquals("encodedPassword", result.getPassword());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando el usuario ya existe")
    void createUser_DuplicateUsername() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(newUser));

        // Act & Assert
        assertThrows(DuplicateResourceException.class, () -> {
            userService.createUser(createUserRequest);
        });
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando el email ya existe")
    void createUser_DuplicateEmail() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(newUser));

        // Act & Assert
        assertThrows(DuplicateResourceException.class, () -> {
            userService.createUser(createUserRequest);
        });
    }

    @Test
    @DisplayName("Debe lanzar IllegalStateException cuando el role no existe")
    void createUser_RoleNotFound() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.empty());

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            userService.createUser(createUserRequest);
        });
        assertEquals("Default role ROLE_USER not found", exception.getMessage());
    }

    @Test
    @DisplayName("Debe devolver lista vacía cuando no hay usuarios")
    void getAllUsers_Empty() {
        // Arrange
        when(userRepository.findAll()).thenReturn(Collections.emptyList());

        // Act
        var result = userService.getAllUsers();

        // Assert
        assertTrue(result.isEmpty());
    }
}
````

## File: .dockerignore
````
# Docker
.docker/
.dockerignore

# Archivos de Docker
Dockerfile*
docker-compose*
Dockerfile.*

# .env con credenciales reales
.env

# Build outputs
target/
build/

# Maven
.mvn/
**!/.mvn/

# IDE
.idea/
*.iml
*.ipr
*.iws
.settings/
.project
.classpath
.factorypath
.springBeans
.sts4-cache
.apt_generated
.vscode/

# OS
.DS_Store
Thumbs.db

# Logs
*.log
logs/

# Temp
*.tmp
.cache/
````

## File: .env.example
````
# ============================================================================
# ATHLETECORE API - Ejemplo de Variables de Entorno
# ============================================================================
#
# COPIA ESTE ARCHIVO como .env y configura tus valores reales
#
# Este archivo es un EJEMPLO con valores de desarrollo genéricos.
# NUNCA uses estos valores en producción.
#
# ============================================================================

# ---------------------------------------------------------------------------
# BASE DE DATOS (PostgreSQL)
# ---------------------------------------------------------------------------

# URL de conexión a la base de datos
# Formato: jdbc:postgresql://host:puerto/base_de_datos
DB_URL=jdbc:postgresql://localhost:5432/athletecore

# Usuario de PostgreSQL
# Cambia este valor según tu configuración
DB_USER=postgres

# Contraseña de PostgreSQL
# IMPORTANTE: Cambia esta contraseña en cada entorno
DB_PASSWORD=postgres

# ---------------------------------------------------------------------------
# CONFIGURACIÓN JWT (JSON Web Tokens)
# ---------------------------------------------------------------------------

# Clave secreta para firmar tokens JWT
# REQUERIMIENTO: Mínimo 32 caracteres
# GENERAR NUEVA: openssl rand -hex 32
JWT_SECRET=generate-a-new-secret-key-minimum-32-characters-long

# Tiempo de expiración del token en milisegundos
# 86400000 = 24 horas
# 3600000 = 1 hora
JWT_EXPIRATION=86400000

# Issuer URL para validación JWT (si usas Auth0, Keycloak, etc.)
# Dejar vacío para autenticación interna
JWT_ISSUER_URI=

# ---------------------------------------------------------------------------
# CONFIGURACIÓN DEL SERVIDOR
# ---------------------------------------------------------------------------

# Puerto donde se ejecutará la aplicación
SERVER_PORT=8080

# ---------------------------------------------------------------------------
# CONFIGURACIÓN CORS (Cross-Origin Resource Sharing)
# ---------------------------------------------------------------------------

# Orígenes permitidos para requests del frontend
# Separar múltiples orígenes con coma
CORS_ORIGINS=http://localhost:3000,http://localhost:5173,http://localhost:4200

# ---------------------------------------------------------------------------
# CONFIGURACIÓN DE LOGGING (Opcional)
# ---------------------------------------------------------------------------

# Nivel de logging para el paquete principal
# Valores: DEBUG, INFO, WARN, ERROR
LOG_LEVEL_DEBUG=com.athletecore=DEBUG,org.springframework.security=DEBUG

# ---------------------------------------------------------------------------
# CONFIGURACIÓN DOCKER (Si usas Docker Compose)
# ---------------------------------------------------------------------------

# Desactivar si usas Docker
DOCKER_ENABLED=true

# ---------------------------------------------------------------------------
# CONFIGURACIÓN DE PRODUCCIÓN (NO USAR EN LOCAL)
# ---------------------------------------------------------------------------

# Estos valores SOLO deben configurarse en producción
# DB_URL=
# DB_USER=
# DB_PASSWORD=
# JWT_SECRET=
````

## File: CONFIGURATION_GUIDE.md
````markdown
# Guía de Configuración de AthleteCore API

Esta guía documenta la configuración del proyecto AthleteCore API para desarrollo local y producción.

## Arquitectura de Configuración

El proyecto utiliza un sistema de configuración basado en Spring Boot con perfiles:

1. **`application.properties`** - Configuración base con valores por defecto
2. **`application-local.properties`** - Configuración de desarrollo local (NO versionada)
3. **`application-{profile}.properties`** - Configuración específica por entorno

## Archivos de Configuración

### application.properties (Versionado)

Archivo base que contiene:
- Valores por defecto seguros que evitan errores en entornos no configurados
- Configuración genérica de aplicaciones
- Fallbacks para variables de entorno

**NO contiene credenciales reales ni secretos.**

### application-local.properties (NO Versionado)

Archivo de configuración local con:
- Configuración detallada para desarrollo
- Variables de entorno con fallbacks
- Logging verbose para debugging
- CORS configurado para frontend local

**ESTE ARCHIVO NO DEBE SUBIRSE A GIT.**

---

## Variables de Entorno Requeridas

### Obligatorias para Producción

| Variable | Descripción | Ejemplo |
|----------|-------------|---------|
| `DB_URL` | URL de conexión PostgreSQL | `jdbc:postgresql://prod-db:5432/athletecore` |
| `DB_USER` | Usuario de base de datos | `athletecore_prod` |
| `DB_PASSWORD` | Contraseña de base de datos | `secreta-produccion` |
| `JWT_SECRET` | Clave secreta para JWT (min. 32 chars) | `a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6` |
| `JWT_EXPIRATION` | Expiración del token en ms | `86400000` (24 horas) |

### Opcionales

| Variable | Descripción | Valor por defecto |
|----------|-------------|-------------------|
| `SERVER_PORT` | Puerto del servidor | `8080` |
| `CORS_ORIGINS` | Orígenes permitidos para CORS | `http://localhost:3000,http://localhost:5173` |
| `JWT_ISSUER_URI` | Issuer URL para validación JWT | (vacío) |

---

## Configuración Rápida para Desarrollo Local

### Opción 1: Variables de entorno en el shell

```bash
# Unix/Linux/Mac
export DB_USER=postgres
export DB_PASSWORD=postgres
export JWT_SECRET=developer-secret-key-change-me-32chars
export SERVER_PORT=8080

# Windows (PowerShell)
$env:DB_USER="postgres"
$env:DB_PASSWORD="postgres"
$env:JWT_SECRET="developer-secret-key-change-me-32chars"
$env:SERVER_PORT="8080"

# Ejecutar aplicación
./mvnw spring-boot:run
```

### Opción 2: Archivo .env (RECOMENDADO)

1. Crear archivo `.env` en la raíz del proyecto:

```env
# AthleteCore API - Variables de Entorno Local
DB_URL=jdbc:postgresql://localhost:5432/athletecore
DB_USER=postgres
DB_PASSWORD=postgres
JWT_SECRET=local-developer-secret-key-32-characters
JWT_EXPIRATION=86400000
SERVER_PORT=8080
CORS_ORIGINS=http://localhost:3000,http://localhost:5173
```

2. Cargar variables antes de ejecutar:

```bash
# Opción A: Usar dotenv-maven-plugin
source .env && ./mvnw spring-boot:run

# Opción B: Usar la herramienta dotenv-cli
dotenv -- ./mvnw spring-boot:run

# Opción C: En IntelliJ IDEA - ir a Run/Debug Configurations -> Environment variables
```

---

## Configuración de Base de Datos

### Crear Base de Datos

```bash
# Conexión como superusuario PostgreSQL
psql postgres

# Crear base de datos
CREATE DATABASE athletecore;

# Salir
\q
```

### Migraciones Flyway

La primera vez que ejecutas la aplicación, Flyway:
1. Inicializa el esquema de control `flyway_schema_history`
2. Ejecuta todas las migraciones no aplicadas
3. Crea las tablas según tus entidades

**Importante:** `spring.flyway.baseline-on-migrate=true` solo debe usarse la primera vez. Después de inicializar, cambia a `false` para validar estrictamente el esquema.

---

## Logging y Debug

### Niveles de Logging por defecto

| Paquete | Nivel | Descripción |
|---------|-------|-------------|
| `com.athletecore` | `DEBUG` | Tu aplicación |
| `org.hibernate.SQL` | `DEBUG` | SQL generado |
| `org.hibernate.type.descriptor.sql.BasicBinder` | `TRACE` | Parámetros SQL |
| `org.springframework.security` | `DEBUG` | Seguridad y autenticación |

### Cambiar nivel de logging en runtime

```bash
# Cambiar nivel de log con Spring Boot Actuator
curl -X PUT http://localhost:8080/actuator/loggers/com.athletecore -d '{"configuredLevel": "INFO"}'
```

---

## Docker Compose

### docker-compose.yml de ejemplo

```yaml
version: '3.8'

services:
  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      - DB_URL=jdbc:postgresql://db:5432/athletecore
      - DB_USER=postgres
      - DB_PASSWORD=postgres
      - JWT_SECRET=local-secret-key-32-characters
      - SERVER_PORT=8080
    depends_on:
      db:
        condition: service_healthy
    networks:
      - athletecore-network

  db:
    image: postgres:16-alpine
    ports:
      - "5432:5432"
    environment:
      - POSTGRES_USER=postgres
      - POSTGRES_PASSWORD=postgres
      - POSTGRES_DB=athletecore
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 5s
      timeout: 5s
      retries: 5
    networks:
      - athletecore-network

volumes:
  postgres_data:

networks:
  athletecore-network:
    driver: bridge
```

### Ejecutar con Docker Compose

```bash
# Construir y levantar todo
docker-compose up --build

# Levantar en segundo plano
docker-compose up -d

# Ver logs
docker-compose logs -f

# Parar
docker-compose down

# Parar y eliminar volúmenes (¡CUIDADO! pierde datos)
docker-compose down -v
```

---

## Seguridad

### JWT Secret

**CRÍTICO:** Nunca uses el valor por defecto en producción.

Generar un JWT_SECRET seguro:

```bash
# Linux/Mac
openssl rand -hex 32

# O usar Python
python3 -c "import secrets; print(secrets.token_hex(32))"
```

### Gestión de Secrets en Producción

Para producción, considera usar:
- **AWS Secrets Manager** / **Parameter Store**
- **HashiCorp Vault**
- **Azure Key Vault**
- **Google Secret Manager**

---

## Solución de Problemas Comunes

### Error: "Database 'athletecore' does not exist"

```bash
# Crear la base de datos
psql -U postgres -c "CREATE DATABASE athletecore;"
```

### Error: "Flyway migration failed"

1. Verificar las migraciones en `src/main/resources/db/migration/`
2. Revisar logs de Flyway: `logging.level.org.flywaydb=DEBUG`
3. Limpiar esquema (DEVELOPMENT ONLY):
   ```bash
   # Advertencia: Esto borra todos los datos
   spring.flyway.clean=true
   ./mvnw flyway:clean
   ./mvnw flyway:migrate
   ```

### Error: "Port 8080 already in use"

```bash
# Cambiar puerto
export SERVER_PORT=8081

# O matar el proceso que usa el puerto
lsof -ti:8080 | xargs kill -9
```

### Error: "JWT signature invalid"

```bash
# Verificar JWT_SECRET
export JWT_SECRET=$(openssl rand -hex 32)

# Reiniciar la aplicación
```

---

## Referencias

- [Spring Boot Configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config)
- [Flyway Documentation](https://flywaydb.org/documentation/)
- [PostgreSQL JDBC Connection](https://jdbc.postgresql.org/documentation/)
- [Spring Security JWT](https://spring.io/projects/spring-security)

---

## Soporte

Para dudas sobre configuración, revisar:
1. Este documento
2. `application-local.properties` (comentarios en el archivo)
3. Los logs de la aplicación (`logs/athletecore-local.log`)
````

## File: docker-compose.yml
````yaml
version: '3.8'

services:
  # ========================================
  # BASE DE DATOS POSTGRESQL
  # ========================================
  postgres:
    image: postgres:15-alpine
    container_name: athletecore-postgres
    restart: unless-stopped
    environment:
      POSTGRES_USER: ${DB_USER:-athletecore_user}
      POSTGRES_PASSWORD: ${DB_PASSWORD:-athletecore_pass}
      POSTGRES_DB: ${DB_NAME:-athletecore}
    ports:
      - "5432:5432"
    volumes:
      - pgdata:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${DB_USER:-athletecore_user} -d ${DB_NAME:-athletecore}"]
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 10s
    networks:
      - athletecore-network
    profiles:
      - default
      - full

  # ========================================
  # FLYWAY (MIGRACIONES)
  # ========================================
  flyway:
    image: flyway/flyway:10-alpine
    container_name: athletecore-flyway
    depends_on:
      postgres:
        condition: service_healthy
    command:
      - -userid=${DB_USER:-athletecore_user}
      - -password=${DB_PASSWORD:-athletecore_pass}
      - -url=jdbc:postgresql://postgres:5432/${DB_NAME:-athletecore}
      - -locations=filesystem:/flyway-local
      - migrate
    environment:
      - FLYWAY_SCHEMAS=public
      - FLYWAY_VALIDATE_ON_MIGRATE=true
      - FLYWAY_CLEAN_DISABLED=true
    volumes:
      # Mount source migration files directly from source
      - ./src/main/resources/db:/flyway-local:ro
    networks:
      - athletecore-network
    profiles:
      - migrate

  # ========================================
  # APLICACIÓN SPRING BOOT (DEVELOPMENT)
  # ========================================
  app-dev:
    image: athletecore-api-dev
    container_name: athletecore-api
    build:
      context: .
      dockerfile: Dockerfile
      target: runtime
    profiles:
      - development
    environment:
      SPRING_PROFILES_ACTIVE: docker
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/${DB_NAME:-athletecore}
      SPRING_DATASOURCE_USERNAME: ${DB_USER:-athletecore_user}
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD:-athletecore_pass}
      SPRING_JPA_HIBERNATE_DDL_AUTO: validate
      JWT_SECRET: ${JWT_SECRET:-your-secret-key-min-32-chars-change-in-production}
      JWT_EXPIRATION_MS: ${JWT_EXPIRATION_MS:-86400000}
      CORS_ALLOWED_ORIGINS: ${CORS_ALLOWED_ORIGINS:-http://localhost:3000,http://localhost:5173}
    ports:
      - "8080:8080"
    volumes:
      # Mount target/classes para hot-reload (necesita rebuild manual)
      - ./target/classes:/app/classes:ro
      # Mount fuente para herramientas como DevTools
      - ./src:/app/src:ro
    depends_on:
      postgres:
        condition: service_healthy
      flyway:
        condition: service_completed_successfully
    networks:
      - athletecore-network
    restart: unless-stopped

  # ========================================
  # APLICACIÓN SPRING BOOT (PRODUCCIÓN)
  # ========================================
  app:
    image: athletecore-api
    container_name: athletecore-api-prod
    build:
      context: .
      dockerfile: Dockerfile
      target: runtime
    profiles:
      - production
    environment:
      SPRING_PROFILES_ACTIVE: docker-prod
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/${DB_NAME:-athletecore}
      SPRING_DATASOURCE_USERNAME: ${DB_USER:-athletecore_user}
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD:-athletecore_pass}
      SPRING_JPA_HIBERNATE_DDL_AUTO: validate
      JWT_SECRET: ${JWT_SECRET}
      JWT_EXPIRATION_MS: ${JWT_EXPIRATION_MS:-86400000}
      CORS_ALLOWED_ORIGINS: ${CORS_ALLOWED_ORIGINS}
    ports:
      - "8080:8080"
    depends_on:
      postgres:
        condition: service_healthy
      flyway:
        condition: service_completed_successfully
    networks:
      - athletecore-network
    restart: unless-stopped

# ========================================
# VOLÚMENES PERSISTENTES
# ========================================
volumes:
  pgdata:
    driver: local
    name: athletecore_pgdata

# ========================================
# REDES
# ========================================
networks:
  athletecore-network:
    driver: bridge
    name: athletecore-network
````

## File: Dockerfile
````dockerfile
# ========================================
# ETAPA 1: BUILD (Compilación)
# ========================================
FROM eclipse-temurin:21-jdk-alpine AS build

# Variables de entorno para Maven
ENV MAVEN_VERSION=3.9.9
ENV USER_HOME_DIR=/home/appuser
ENV SPRING_PROFILE=docker
ENV DOCKER_BUILDKIT=1

# Directorio de trabajo
WORKDIR /build

# Instalar Maven desde wget (Alpine no tiene apt)
RUN apk add --no-cache curl

# Descargar y configurar Maven
RUN curl -fsSL https://archive.apache.org/dist/maven/maven-3/3.9.9/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz \
    | tar -xzf - -C /opt \
    && ln -s /opt/apache-maven-${MAVEN_VERSION}/bin/mvn /usr/bin/mvn

# Copiar archivos de configuración del proyecto
COPY mvnw .
COPY .mvn .mvn
COPY mvnw.cmd .
COPY pom.xml .

# Descargar dependencias de Maven (capa caché)
RUN ./mvnw dependency:go-offline -B

# Copiar el código fuente
COPY src ./src

# Compilar el proyecto (incluye recursos como migraciones Flyway)
RUN ./mvnw clean package -DskipTests -Dmaven.javadoc.skip=true -B

# ========================================
# ETAPA 2: RUNTIME (Ejecución)
# ========================================
FROM eclipse-temurin:21-jre-alpine AS runtime

# Variables de entorno
ENV JAVA_OPTIONS="-Xmx512m -Xms256m"
ENV SPRING_PROFILES_ACTIVE=docker
ENV APP_HOME=/app

# Crear usuario no root
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Directorio de trabajo
WORKDIR ${APP_HOME}

# Copiar JAR empaquetado desde la etapa de build
COPY --from=build /build/target/*.jar app.jar

# Establecer propiedad de archivos
RUN chown -R appuser:appgroup ${APP_HOME}

# Cambiar a usuario no root
USER appuser

# Exposición del puerto
EXPOSE 8080

# Healthcheck para Spring Boot
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Comando de entrada
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTIONS -jar app.jar"]

# ========================================
# ETAPA 3: DEV (Desarrollo con JLink)
# ========================================
FROM build AS dev

# Crear JRE optimizado (opcional para producción, pero útil para dev)
RUN java -jlink --add-modules ALL-MODULE-PATH \
    --strip-debug \
    --no-man-pages \
    --no-header-files \
    --output jre \
    --module-path lib \
    --add-modules java.base,java.sql,java.naming,jdk.unsupported

# Sobreescribe el JRE del sistema por el optimizado (opcional)
# Este paso es opcional y puede aumentar el tiempo de build
````

## File: README.md
````markdown
# AthleteCore API

API para gestionar el rendimiento deportivo y entrenamientos de atletas.

## Tecnologías

- **Java 21** con Spring Boot 3.5.6
- **PostgreSQL 15** para almacenamiento de datos
- **Flyway** para migraciones de base de datos
- **Docker & Docker Compose** para desarrollo y despliegue
- **Spring Security** con JWT para autenticación

## Requisitos Previos

- Docker y Docker Compose instalados
- Java 21 (para desarrollo local sin Docker)
- Maven 3.9+ (para desarrollo local sin Docker)

## Configuración Inicial

### 1. Variables de Entorno

Copia el archivo de ejemplo y configúralo para tu entorno local:

```bash
cp .env.example .env
```

**IMPORTANTE**: Edita `.env` con tus propias credenciales y **nunca** lo commits al repositorio.

### 2. Dependencias del Proyecto

El proyecto usa Spring Boot DevTools para desarrollo local. Las dependencias principales son:
- Spring Boot Starter Web
- Spring Boot Starter Data JPA
- Spring Boot Starter Security
- PostgreSQL Driver
- Flyway (para migraciones)

## Desarrollo con Docker Compose

### Levantar el entorno completo

```bash
# Levantar solo la base de datos
docker-compose up -d postgres

# Levantar base de datos + ejecutar migraciones Flyway
docker-compose --profile migrate up -d flyway

# Levantar todo (postgres + flyway + aplicación en desarrollo)
docker-compose --profile development up -d

# Levantar para producción
docker-compose --profile production up -d
```

### Detener el entorno

```bash
# Detener todos los servicios
docker-compose down

# Detener y eliminar volúmenes (¡PERDERÁS LOS DATOS!)
docker-compose down -v
```

### Ver logs

```bash
# Ver logs de todos los servicios
docker-compose logs -f

# Ver logs de un servicio específico
docker-compose logs -f postgres
docker-compose logs -f app-dev
docker-compose logs -f flyway
```

## Acceso a Servicios

### Base de Datos PostgreSQL

- **Host**: localhost
- **Puerto**: 5432
- **Usuario**: athletecore_user (o el que definas en .env)
- **Contraseña**: athletecore_pass (o el que definas en .env)
- **Base de datos**: athletecore

**Conexión con psql**:
```bash
psql -h localhost -p 5432 -U athletecore_user -d athletecore
```

**Conexión con Docker**:
```bash
docker exec -it athletecore-postgres psql -U athletecore_user -d athletecore
```

### API REST

- **Endpoint**: http://localhost:8080
- **Health Check**: http://localhost:8080/actuator/health
- **Documentación**: http://localhost:8080/swagger-ui.html (si está configurado)

## Migraciones de Base de Datos

Las migraciones se ejecutan automáticamente con Flyway cuando levantas el servicio:

```bash
# Ejecutar migraciones manualmente
docker-compose --profile migrate up flyway

# Verificar estado de migraciones
docker exec -it athletecore-postgres psql -U athletecore_user -d athletecore -c "SELECT * FROM flyway_schema_history ORDER BY installed_rank;"
```

## Configuración de Aplicación

### Perfiles de Spring

- **docker**: Configuracion básica para desarrollo con Docker
- **docker-prod**: Configuración optimizada para producción

### Variables de Entorno Principales

| Variable | Descripción | Default |
|----------|-------------|---------|
| `SPRING_PROFILES_ACTIVE` | Perfiles de Spring | `docker` |
| `JWT_SECRET` | Clave secreta para JWT | (requerido) |
| `JWT_EXPIRATION_MS` | Expiración del token (ms) | `86400000` (24h) |
| `CORS_ALLOWED_ORIGINS` | Orígenes permitidos CORS | `http://localhost:3000` |

## Despliegue en Producción

### Construir imágenes

```bash
# Construir imagen para producción
docker-compose --profile production build app
```

### Levantar en producción

```bash
# Asegurar que no existe un .env con credenciales reales antes de desplegar
docker-compose --profile production up -d --build
```

### Actualización de imagen

```bash
# Tirar contenedor actual y renovar con nueva imagen
docker-compose --profile production down
docker-compose --profile production pull
docker-compose --profile production up -d
```

## Solución de Problemas

### Puerto 5432 ocupado

Si el puerto de PostgreSQL ya está en uso:

```bash
# Verificar qué está usando el puerto
lsof -i :5432

# Opción 1: Matar el proceso que usa el puerto
kill -9 <PID>

# Opción 2: Cambiar el puerto en docker-compose.yml y .env
# DB_PORT_HOST=5433 (en .env)
# "5433:5432" (en docker-compose.yml)
```

### Puerto 8080 ocupado

```bash
# Verificar qué está usando el puerto
lsof -i :8080

# Matar el proceso
kill -9 <PID>

# O cambiar el puerto en docker-compose.yml (ej: "8081:8080")
```

### Problemas de permisos en volúmenes

Si los archivos mount tienen problemas de permisos:

```bash
# Crear volumen manualmente
docker volume create athletecore_pgdata

# Ver permisos del volumen
docker run --rm -v athletecore_pgdata:/data alpine ls -la /data
```

### Base de datos no inicia

```bash
# Ver logs de postgres
docker-compose logs -f postgres

# Reiniciar el contenedor
docker-compose restart postgres

# Eliminar y recrear volumen (¡PERDERÁS DATOS!)
docker-compose down -v
docker-compose up -d postgres
```

### Flyway no encuentra migraciones

```bash
# Verificar que las migraciones existen
ls src/main/resources/db/migration

# Reconstruir clases para Docker
docker-compose build --no-cache
```

### Aplicación no arranca

```bash
# Ver logs de la aplicación
docker-compose logs -f app-dev

# Verificar variables de entorno
docker-compose config

# Validar que postgres esté saludable
docker-compose ps
```

### Memoria insuficiente

Ajusta los límites en Dockerfile o docker-compose.yml:

```bash
# Ejemplo en docker-compose.yml para app-dev
mem_limit: 1g
memswap_limit: 1g
```

## Development Local (Sin Docker)

Para desarrollo directo en tu máquina:

### 1. Instalar y ejecutar PostgreSQL localmente

```bash
# macOS
brew install postgresql@15
brew services start postgresql@15

# Ubuntu/Debian
sudo apt-get install postgresql-15
sudo systemctl start postgresql
```

### 2. Crear usuario y base de datos

```bash
sudo -u postgres psql
CREATE USER athletecore_user WITH PASSWORD 'athletecore_pass';
CREATE DATABASE athletecore OWNER athletecore_user;
GRANT ALL PRIVILEGES ON DATABASE athletecore TO athletecore_user;
\q
```

### 3. Ejecutar migraciones

```bash
cd src/main/resources
flyway migrate
```

### 4. Ejecutar la aplicación

```bash
# Configurar .env con variables locales
cp .env.example .env
# Edita .env con configuración local

# Ejecutar con Maven
./mvnw spring-boot:run
```

## Estructura del Proyecto

```
athletecore-api/
├── docker-compose.yml     # Orquestación de servicios Docker
├── Dockerfile            # Build multi-stage para la aplicación
├── .env.example          # Plantilla de variables de entorno
├── .gitignore           # Archivos ignorados en git
├── pom.xml              # Dependencias Maven
└── src/
    ├── main/
    │   ├── java/        # Código fuente Java
    │   └── resources/
    │       ├── db/
    │       │   └── migration/  # Migraciones Flyway
    │       └── application.properties
    └── test/            # Tests
```

## Contribuir

1. Crea una feature branch
2. Haz tus cambios
3. Ejecuta tests: `./mvnw test`
4. Haz PR

## Licencia

Propietario y confidencial.
````

## File: .claude/agents/athlete-domain.md
````markdown
---
name: athlete-domain
description: |
  Agente del módulo deportistas (athlete) para AthleteCore API. Gestiona el registro,
  perfil antropométrico, foto de perfil y soft delete de deportistas. Especializado
  en la extensibilidad del modelo de deportes/disciplinas.ttools:
tools:
  - Read
  - Edit
  - Write
  - Bash
  - Agent
---

# Rol

Eres el agente especializado `athlete-domain` para el proyecto `athletecore-api`.

## Dominio

- Tecnología principal: Java 21, Spring Boot 3.5.6
- Stack complementario: PostgreSQL, JPA/Hibernate, Flyway, Lombok
- Dominio de negocio: Gestión de deportistas, datos sociodemográficos, perfil antropométrico (peso, talla, envergadura, IMC), fotos de perfil

## Responsabilidades

1. Implementar el módulo `athlete/` con capas Controller → Service → Repository usando DTOs obligatorios.
2. Gestionar datos sociodemográficos: nombre, fecha de nacimiento, documento, género, contacto.
3. Gestionar perfil antropométrico: peso, talla, envergadura, IMC calculado automáticamente.
4. Soportar subida y almacenamiento de foto de perfil del deportista.
5. Implementar soft delete aprovechando `BaseEntity` (herencia obligatoria).
6. Diseñar el modelo de deportes/disciplinas de forma genérica para soportar deportes distintos a natación en el futuro.

## Reglas de trabajo

- Aplica siempre `core/principles.md` como pilar base, además de las reglas específicas de este agente.
- **Nunca expongas entidades JPA directamente en respuestas HTTP**. Usa DTOs de request y response.
- Todas las entidades deben extender `BaseEntity` para auditoría y soft delete.
- El cálculo de IMC debe ser determinístico y testeable (método puro sin dependencias externas).
- Fotos de perfil: almacenar referencia/URL, no el binario en base de datos (salvo que se decida explícitamente).
- Diseñar el modelo de disciplinas como entidad separada y genérica, vinculada al deportista mediante relación configurable.
- Consultar con `backend-architect` para la creación de migraciones Flyway del módulo.
- Indexar columnas de búsqueda frecuente (documento, nombre) en migraciones.

## Formato de salida obligatorio

```
## Resumen
[En una línea: qué hiciste o decidiste.]

## Detalle
[Lo necesario para que otro agente o el usuario entienda el razonamiento.]

## Pendientes / Bloqueos
[Si algo quedó fuera de alcance, bloqueado o requiere decisión externa.]
```

## Alcance

- Este agente cubre exclusivamente el módulo `athlete/` y sus submódulos relacionados.
- Este agente **no** implementa entrenamientos, asistencia, chequeos ni reportes.
- Este agente **no** interviene en decisiones de la capa meta (`genesis/`).
````

## File: .claude/agents/backend-architect.md
````markdown
---
name: backend-architect
description: |
  Agente de arquitectura transversal para AthleteCore API. Gestiona configuraciones
  globales (seguridad, JPA, Flyway, Docker Compose), infraestructura compartida y
  esquema de base de datos. No implementa lógica de negocio de dominio.
tools:
  - Read
  - Edit
  - Write
  - Bash
  - Agent
---

# Rol

Eres el agente especializado `backend-architect` para el proyecto `athletecore-api`.

## Dominio

- Tecnología principal: Java 21, Spring Boot 3.5.6
- Stack complementario: PostgreSQL, Flyway, Spring Security (JWT stateless), Docker Compose, Lombok
- Dominio de negocio: Gestión deportiva (natación inicial, extensible a otros deportes)

## Responsabilidades

1. Configuración transversal: `SecurityConfig`, `JpaConfig`, manejadores globales (`@RestControllerAdvice`), CORS, filtros.
2. Infraestructura de base de datos: migraciones Flyway, esquema compartido, índices, constraints.
3. Configuración de entornos: `application.properties` / `application-local.properties`, variables de entorno, Docker Compose.
4. Supervisar que las entidades extiendan `BaseEntity` (auditoría y soft delete) y que usen `@SQLRestriction` (no `@Where`).
5. Asegurar que ningún endpoint sensible sea público sin autenticación; revisar reglas `@PreAuthorize`.
6. Mantener consistencia en la estructura de módulos y convenciones de nombres del proyecto.

## Reglas de trabajo

- Aplica siempre `core/principles.md` como pilar base, además de las reglas específicas de este agente.
- **Nunca uses `ddl-auto=create` ni `ddl-auto=update`** en entornos que no sean desarrollo inicial. El esquema se gestiona con Flyway.
- Toda migración Flyway debe ser versionada y reversible conceptualmente (script de rollback documentado).
- Credenciales y secrets solo via variables de entorno; nunca hardcodeadas.
- `SecurityConfig` debe limitar `permitAll()` únicamente a endpoints de autenticación (`/api/v1/auth/**`).
- `@RestControllerAdvice` centraliza excepciones; evita `catch (Exception e)` genéricos en controllers.
- Revisar que DTOs de respuesta no expongan entidades JPA directamente.
- Las entidades deben extender `BaseEntity` para tener `createdAt`, `updatedAt`, `deletedAt`. Usar `@SQLRestriction("deleted_at IS NULL")`.

## Formato de salida obligatorio

```
## Resumen
[En una línea: qué hiciste o decidiste.]

## Detalle
[Lo necesario para que otro agente o el usuario entienda el razonamiento.]

## Pendientes / Bloqueos
[Si algo quedó fuera de alcance, bloqueado o requiere decisión externa.]
```

## Alcance

- Este agente **no** implementa lógica de negocio de dominio (deportistas, entrenamientos, chequeos).
- Este agente **no** interviene en decisiones de la capa meta (`genesis/`); si detecta que el proyecto necesita regenerarse desde cero, propónlo como `change` de OpenSpec, no como acción inmediata.
- Antes de modificar configuraciones globales que afecten a otros módulos, consulta con los agentes de dominio afectados.
````

## File: .claude/agents/checkup-domain.md
````markdown
---
name: checkup-domain
description: |
  Agente del módulo chequeos mensuales para AthleteCore API. Gestiona el registro de
  tiempos de prueba, comparación con tabla nacional de referencia, proyección de
  medallería y clasificación del deportista.tools:
tools:
  - Read
  - Edit
  - Write
  - Bash
  - Agent
---

# Rol

Eres el agente especializado `checkup-domain` para el proyecto `athletecore-api`.

## Dominio

- Tecnología principal: Java 21, Spring Boot 3.5.6
- Stack complementario: PostgreSQL, JPA/Hibernate, Flyway, Lombok
- Dominio de negocio: Evaluación mensual de rendimiento deportivo mediante tiempos de prueba y comparación con referencias nacionales

## Responsabilidades

1. Implementar el módulo `checkup/` con capas Controller → Service → Repository usando DTOs obligatorios.
2. Registrar tiempos de prueba (estilo + distancia) por deportista en chequeo mensual.
3. Almacenar tiempos en segundos decimales; mostrar en formato `mm:ss.ms` en DTOs de respuesta.
4. Comparar tiempos registrados con tabla de tiempos nacionales de referencia (1°, 2°, 3° puesto).
5. Calcular y exponer proyección de medallería: diferencia de tiempo respecto a 1°, 2° y 3° puesto.
6. Asignar clasificación relativa: **Por encima del podio**, **Cercano a medallería**, **Fuera de rango**.
7. Gestionar la tabla de tiempos nacionales de referencia (CRUD administrativo, accesible solo para roles autorizados).
8. Coordinar con `backend-architect` para migraciones Flyway e índices.

## Reglas de trabajo

- Aplica siempre `core/principles.md` como pilar base, además de las reglas específicas de este agente.
- **Nunca expongas entidades JPA directamente en respuestas HTTP**. Usa DTOs de request y response.
- Todas las entidades deben extender `BaseEntity` para auditoría y soft delete.
- Los cálculos de comparación deben ejecutarse síncronamente y retornar en < 500ms para un equipo de hasta 50 deportistas. Optimizar queries e indices.
- El formato de presentación `mm:ss.ms` debe ser responsabilidad de la capa de presentación (DTO/Formatter), no de la entidad.
- Los tiempos de referencia nacional deben ser editables solo por roles con permisos administrativos (validar en controller con `@PreAuthorize`).
- La clasificación relativa debe ser determinística y testeable con datos de prueba fijos.

## Formato de salida obligatorio

```
## Resumen
[En una línea: qué hiciste o decidiste.]

## Detalle
[Lo necesario para que otro agente o el usuario entienda el razonamiento.]

## Pendientes / Bloqueos
[Si algo quedó fuera de alcance, bloqueado o requiere decisión externa.]
```

## Alcance

- Este agente cubre exclusivamente el módulo `checkup/` y la tabla de tiempos nacionales de referencia.
- Este agente **no** implementa deportistas, entrenamientos, asistencia ni reportes.
- Este agente **no** interviene en decisiones de la capa meta (`genesis/`).
````

## File: .claude/agents/quality-guardian.md
````markdown
---
name: quality-guardian
description: |
  Agente de calidad y revisión para AthleteCore API. Revisa calidad de código,
  cobertura de tests, deuda técnica, aplicación de principios SOLID y
  consistencia con las convenciones del proyecto. No implementa features.
tools:
  - Read
  - Edit
  - Write
  - Bash
  - Agent
---

# Rol

Eres el agente especializado `quality-guardian` para el proyecto `athletecore-api`.

## Dominio

- Tecnología principal: Java 21, Spring Boot 3.5.6
- Stack complementario: Maven, JUnit 5, Mockito, Spring Boot Test
- Dominio de negocio: Gestión deportiva (natación inicial, extensible)

## Responsabilidades

1. Revisar que toda lógica de negocio no trivial tenga tests automáticos (JUnit 5 + Mockito).
2. Verificar que no haya entidades JPA expuestas directamente en respuestas HTTP (uso de DTOs).
3. Revisar que todas las entidades extiendan `BaseEntity` y usen `@SQLRestriction` (no `@Where`).
4. Validar manejo centralizado de excepciones con `@RestControllerAdvice`.
5. Revisar que `SecurityConfig` no exponga endpoints sensibles con `permitAll()`.
6. Verificar que no haya credenciales hardcodeadas ni secrets en código fuente.
7. Revisar cobertura de caminos de error (nulls, excepciones, validaciones fallidas).
8. Verificar consistencia de naming, formateo y estructura de carpetas del proyecto.
9. Documentar hallazgos de deuda técnica y proponer prioridades de corrección.

## Reglas de trabajo

- Aplica siempre `core/principles.md` como pilar base, además de las reglas específicas de este agente.
- **No implementar features** ni refactoring directo sin instrucción explícita del usuario o de otro agente. Tu rol es auditor y advisor.
- Priorizar hallazgos por impacto (seguridad > integridad de datos > mantenibilidad > estilo).
- Usar evidencia concreta (archivo, línea, snippet) al reportar problemas.
- Si detectas un problema crítico de seguridad, reportarlo inmediatamente como bloqueo.
- Antes de reportar una inconsistencia, verifica si es intencional (deja un comentario de duda antes de concluir).

## Formato de salida obligatorio

```
## Resumen
[En una línea: estado de calidad general o decisión tomada.]

## Detalle
[Hallazgos concretos, evidencia y severidad.]

## Pendientes / Bloqueos
[Si algo requiere decisión externa o escapa a tu alcance.]
```

## Alcance

- Este agente **no** implementa lógica de negocio de dominio.
- Este agente **no** modifica configuraciones globales (eso corresponde a `backend-architect`).
- Este agente **no** interviene en decisiones de la capa meta (`genesis/`).
- Este agente puede revisar trabajo de cualquier otro agente especializado del proyecto.
````

## File: .claude/agents/report-domain.md
````markdown
---
name: report-domain
description: |
  Agente del modulo reportes para AthleteCore API. Gestiona la generacion de reportes
  individuales y generales, exportacion a PDF y visualizacion grafica de evolucion
  de tiempos. Integra datos de deportistas, entrenamientos y chequeos.
tools:
  - Read
  - Edit
  - Write
  - Bash
  - Agent
---

# Rol

Eres el agente especializado `report-domain` para el proyecto `athletecore-api`.

## Dominio

- Tecnologia principal: Java 21, Spring Boot 3.5.6
- Stack complementario: PostgreSQL, JPA/Hibernate, Flyway, biblioteca de generacion de PDF (a definir/evaluar), Lombok
- Dominio de negocio: Reportes de rendimiento deportivo, exportacion PDF, graficas de evolucion

## Responsabilidades

1. Implementar el modulo `report/` con capas Controller → Service → Repository usando DTOs obligatorios.
2. Generar reporte individual de deportista: evolucion de tiempos, asistencia, lesiones.
3. Generar reporte general del equipo: comparativo de rendimiento por prueba.
4. Exportar reportes en formato PDF.
5. Preparar estructura para visualizacion grafica de evolucion de tiempos (datos estructurados para el frontend futuro).
6. Consultar datos de otros modulos (athlete, training, checkup, injuries) para armar reportes agregados.
7. Coordinar con `backend-architect` para la integracion de la biblioteca PDF y migraciones si es necesario.

## Reglas de trabajo

- Aplica siempre `core/principles.md` como pilar base, ademas de las reglas especificas de este agente.
- Nunca expongas entidades JPA directamente en respuestas HTTP. Usa DTOs de request y response.
- Reportes PDF: priorizar bibliotecas modernas y mantenidas para Java (ej. OpenPDF, iText 7 Open Source, o JasperReports si es necesario).
- Los datos para reportes deben obtenerse a traves de servicios de los otros modulos, no accediendo directamente a sus repositorios (respetar fronteras de modulo).
- La generacion de PDF puede ser sincrona para reportes pequenos (< 50 deportistas) o preparar estructura asincrona si se anticipa crecimiento.
- Datos graficos: exponer endpoints que devuelvan series temporales estructuradas; la renderizacion grafica sera responsabilidad del frontend futuro.

## Formato de salida obligatorio

```
## Resumen
[En una linea: que hiciste o decidiste.]

## Detalle
[Lo necesario para que otro agente o el usuario entienda el razonamiento.]

## Pendientes / Bloqueos
[Si algo quedo fuera de alcance, bloqueado o requiere decision externa.]
```

## Alcance

- Este agente cubre exclusivamente el modulo `report/`.
- Este agente no implementa deportistas, entrenamientos, chequeos ni logica de autenticacion.
- Este agente no interviene en decisiones de la capa meta (genesis/).
````

## File: .claude/agents/training-domain.md
````markdown
---
name: training-domain
description: |
  Agente del módulo entrenamientos y planificación para AthleteCore API. Gestiona
  sesiones de entrenamiento, jerarquía de ciclos (sesión → microciclo → mesociclo →
  plan anual), control de asistencia y alertas por ausencias consecutivas.tools:
tools:
  - Read
  - Edit
  - Write
  - Bash
  - Agent
---

# Rol

Eres el agente especializado `training-domain` para el proyecto `athletecore-api`.

## Dominio

- Tecnología principal: Java 21, Spring Boot 3.5.6
- Stack complementario: PostgreSQL, JPA/Hibernate, Flyway, Lombok
- Dominio de negocio: Planificación y ejecución de entrenamientos deportivos, control de asistencia

## Responsabilidades

1. Implementar el módulo `training/` con capas Controller → Service → Repository usando DTOs obligatorios.
2. Gestionar la jerarquía de planificación: `Sesión → Microciclo → Mesociclo → Plan Anual`.
3. Registrar sesiones con fecha, hora, observaciones, estado (programada, ejecutada, cancelada).
4. Registrar por sesión: volumen (metros/repeticiones), intensidad (%), distancia, estilo/disciplina y observaciones.
5. Implementar control de asistencia por deportista por sesión (presente, ausente, justificado).
6. Implementar alerta automática ante N ausencias consecutivas configurables.
7. Coordinar con `backend-architect` para migraciones Flyway e índices de consultas frecuentes (sesiones por ciclo, asistencia).

## Reglas de trabajo

- Aplica siempre `core/principles.md` como pilar base, además de las reglas específicas de este agente.
- **Nunca expongas entidades JPA directamente en respuestas HTTP**. Usa DTOs de request y response.
- Todas las entidades deben extender `BaseEntity` para auditoría y soft delete.
- La lógica de alertas por ausencias debe ser testeable de forma determinista (sin depender de `now()` directamente; usar `Clock` inyectado o similar).
- Las consultas frecuentes (listado de sesiones por ciclo, asistencia por deportista) deben estar indexadas.
- El estado de una sesión debe ser un enum con valores finitos y bien definidos.
- El volumen e intensidad deben validarse contra rangos razonables según el deporte.

## Formato de salida obligatorio

```
## Resumen
[En una línea: qué hiciste o decidiste.]

## Detalle
[Lo necesario para que otro agente o el usuario entienda el razonamiento.]

## Pendientes / Bloqueos
[Si algo quedó fuera de alcance, bloqueado o requiere decisión externa.]
```

## Alcance

- Este agente cubre exclusivamente el módulo `training/` y su submódulo de asistencia.
- Este agente **no** implementa deportistas, chequeos, tiempos de referencia ni reportes.
- Este agente **no** interviene en decisiones de la capa meta (`genesis/`).
````

## File: .claude/agents/user-security.md
````markdown
---
name: user-security
description: |
  Agente de autenticación, autorización y gestión de usuarios para AthleteCore API.
  Gestiona JWT, roles (RBAC), flujos de registro/login, activación/desactivación de cuentas.
  Especializado en el módulo `user/` y su interacción con Spring Security.
tools:
  - Read
  - Edit
  - Write
  - Bash
  - Agent
---

# Rol

Eres el agente especializado `user-security` para el proyecto `athletecore-api`.

## Dominio

- Tecnología principal: Java 21, Spring Boot 3.5.6, Spring Security
- Stack complementario: PostgreSQL, JWT (stateless), BCrypt, JPA/Hibernate, Lombok
- Dominio de negocio: Gestión de usuarios del sistema y control de acceso basado en roles

## Responsabilidades

1. Implementar y mantener autenticación stateless con JWT (login, registro, refresh token).
2. Gestionar roles: Deportista, Entrenador, Metodólogo Deportivo, Presidencia/Administración.
3. Asegurar que `UserController` nunca exponga la entidad `User` directamente; usar DTOs de respuesta.
4. Validar reglas de negocio en `UserService` (confirmación de contraseña, unicidad, campos obligatorios).
5. Integrar `@PreAuthorize` en endpoints según roles necesarios.
6. Mantener `RoleRepository` y semilla inicial de roles si es necesario.

## Reglas de trabajo

- Aplica siempre `core/principles.md` como pilar base, además de las reglas específicas de este agente.
- **Nunca serializar `password` en respuestas JSON**. Siempre usar `UserResponse` u otro DTO sin el campo.
- `SecurityConfig` administra los endpoints públicos; tu responsabilidad es asegurar que los propios endpoints usen DTOs y validaciones correctas.
- `CreateUserRequest` debe incluir todos los campos obligatorios de la entidad, incluyendo `firstName` y `lastName`.
- La validación `isPasswordConfirmed()` debe ser invocada explícitamente en `UserService.createUser()`.
- `User` debe extender `BaseEntity`. Si aún no lo hace, coordina con `backend-architect` para corregirlo.
- Tokens JWT deben tener expiración configurada. Secrets via variables de entorno.

## Formato de salida obligatorio

```
## Resumen
[En una línea: qué hiciste o decidiste.]

## Detalle
[Lo necesario para que otro agente o el usuario entienda el razonamiento.]

## Pendientes / Bloqueos
[Si algo quedó fuera de alcance, bloqueado o requiere decisión externa.]
```

## Alcance

- Este agente cubre el módulo `user/` y toda la seguridad de autenticación/autorización.
- Este agente **no** gestiona deportistas, entrenamientos ni chequeos (esos corresponden a otros agentes).
- Este agente **no** interviene en decisiones de la capa meta (`genesis/`).
````

## File: .claude/commands/opsx/apply.md
````markdown
---
name: "OPSX: Apply"
description: Implement tasks from an OpenSpec change (Experimental)
allowed-tools: Bash(openspec:*)
category: Workflow
tags: [workflow, artifacts, experimental]
---

Implement tasks from an OpenSpec change.

**Store selection:** If the user names a store (a store is a standalone OpenSpec repo registered on this machine) or the work lives in one, run `openspec store list --json` to discover registered store ids, then pass `--store <id>` on the commands that read or write specs and changes (`new change`, `status`, `instructions`, `list`, `show`, `validate`, `archive`, `doctor`, `context`). Other commands do not take the flag. Hints printed by commands already carry the flag; keep it on follow-ups. Without a store, commands act on the nearest local `openspec/` root.

**Input**: Optionally specify a change name (e.g., `/opsx:apply add-auth`). If omitted, check if it can be inferred from conversation context. If vague or ambiguous you MUST prompt for available changes.

**Steps**

1. **Select the change**

   If a name is provided, use it. Otherwise:
   - Infer from conversation context if the user mentioned a change
   - Auto-select if only one active change exists
   - If ambiguous, run `openspec list --json` to get available changes and use the **AskUserQuestion tool** to let the user select

   Always announce: "Using change: <name>" and how to override (e.g., `/opsx:apply <other>`).

2. **Check status to understand the schema**
   ```bash
   openspec status --change "<name>" --json
   ```
   Parse the JSON to understand:
   - `schemaName`: The workflow being used (e.g., "spec-driven")
   - `planningHome`, `changeRoot`, and `actionContext`: planning scope and edit constraints
   - Which artifact contains the tasks (typically "tasks" for spec-driven, check status for others)

3. **Get apply instructions**

   ```bash
   openspec instructions apply --change "<name>" --json
   ```

   This returns:
   - `contextFiles`: artifact ID -> array of concrete file paths (varies by schema)
   - Progress (total, complete, remaining)
   - Task list with status
   - Dynamic instruction based on current state

   **Handle states:**
   - If `state: "blocked"` (missing artifacts): show message, suggest using `/opsx:continue`
   - If `state: "all_done"`: congratulate, suggest archive
   - Otherwise: proceed to implementation

4. **Read context files**

   Read every file path listed under `contextFiles` from the apply instructions output.
   The files depend on the schema being used:
   - **spec-driven**: proposal, specs, design, tasks
   - Other schemas: follow the contextFiles from CLI output

5. **Show current progress**

   Display:
   - Schema being used
   - Progress: "N/M tasks complete"
   - Remaining tasks overview
   - Dynamic instruction from CLI

6. **Implement tasks (loop until done or blocked)**

   For each pending task:
   - Show which task is being worked on
   - Make the code changes required
   - Keep changes minimal and focused
   - Mark task complete in the tasks file: `- [ ]` → `- [x]`
   - Continue to next task

   **Pause if:**
   - Task is unclear → ask for clarification
   - Implementation reveals a design issue → suggest updating artifacts
   - Error or blocker encountered → report and wait for guidance
   - User interrupts

7. **On completion or pause, show status**

   Display:
   - Tasks completed this session
   - Overall progress: "N/M tasks complete"
   - If all done: suggest archive
   - If paused: explain why and wait for guidance

**Output During Implementation**

```
## Implementing: <change-name> (schema: <schema-name>)

Working on task 3/7: <task description>
[...implementation happening...]
✓ Task complete

Working on task 4/7: <task description>
[...implementation happening...]
✓ Task complete
```

**Output On Completion**

```
## Implementation Complete

**Change:** <change-name>
**Schema:** <schema-name>
**Progress:** 7/7 tasks complete ✓

### Completed This Session
- [x] Task 1
- [x] Task 2
...

All tasks complete! You can archive this change with `/opsx:archive`.
```

**Output On Pause (Issue Encountered)**

```
## Implementation Paused

**Change:** <change-name>
**Schema:** <schema-name>
**Progress:** 4/7 tasks complete

### Issue Encountered
<description of the issue>

**Options:**
1. <option 1>
2. <option 2>
3. Other approach

What would you like to do?
```

**Guardrails**
- Keep going through tasks until done or blocked
- Always read context files before starting (from the apply instructions output)
- If task is ambiguous, pause and ask before implementing
- If implementation reveals issues, pause and suggest artifact updates
- Keep code changes minimal and scoped to each task
- Update task checkbox immediately after completing each task
- Pause on errors, blockers, or unclear requirements - don't guess
- Use contextFiles from CLI output, don't assume specific file names

**Fluid Workflow Integration**

This skill supports the "actions on a change" model:

- **Can be invoked anytime**: Before all artifacts are done (if tasks exist), after partial implementation, interleaved with other actions
- **Allows artifact updates**: If implementation reveals design issues, suggest updating artifacts - not phase-locked, work fluidly
````

## File: .claude/commands/opsx/archive.md
````markdown
---
name: "OPSX: Archive"
description: Archive a completed change in the experimental workflow
allowed-tools: Bash(openspec:*)
category: Workflow
tags: [workflow, archive, experimental]
---

Archive a completed change in the experimental workflow.

**Store selection:** If the user names a store (a store is a standalone OpenSpec repo registered on this machine) or the work lives in one, run `openspec store list --json` to discover registered store ids, then pass `--store <id>` on the commands that read or write specs and changes (`new change`, `status`, `instructions`, `list`, `show`, `validate`, `archive`, `doctor`, `context`). Other commands do not take the flag. Hints printed by commands already carry the flag; keep it on follow-ups. Without a store, commands act on the nearest local `openspec/` root.

**Input**: Optionally specify a change name after `/opsx:archive` (e.g., `/opsx:archive add-auth`). If omitted, check if it can be inferred from conversation context. If vague or ambiguous you MUST prompt for available changes.

**Steps**

1. **If no change name provided, prompt for selection**

   Run `openspec list --json` to get available changes. Use the **AskUserQuestion tool** to let the user select.

   Show only active changes (not already archived).
   Include the schema used for each change if available.

   **IMPORTANT**: Do NOT guess or auto-select a change. Always let the user choose.

2. **Check artifact completion status**

   Run `openspec status --change "<name>" --json` to check artifact completion.

   Parse the JSON to understand:
   - `schemaName`: The workflow being used
   - `planningHome`, `changeRoot`, `artifactPaths`, and `actionContext`: path and scope context
   - `artifacts`: List of artifacts with their status (`done` or other)

   **If any artifacts are not `done`:**
   - Display warning listing incomplete artifacts
   - Prompt user for confirmation to continue
   - Proceed if user confirms

3. **Check task completion status**

   Read the tasks file (typically `tasks.md`) to check for incomplete tasks.

   Count tasks marked with `- [ ]` (incomplete) vs `- [x]` (complete).

   **If incomplete tasks found:**
   - Display warning showing count of incomplete tasks
   - Prompt user for confirmation to continue
   - Proceed if user confirms

   **If no tasks file exists:** Proceed without task-related warning.

4. **Assess delta spec sync state**

   Use `artifactPaths.specs.existingOutputPaths` from status JSON to check for delta specs. If none exist, proceed without sync prompt.

   **If delta specs exist:**
   - Compare each delta spec with its corresponding main spec at `openspec/specs/<capability>/spec.md`
   - Determine what changes would be applied (adds, modifications, removals, renames)
   - Show a combined summary before prompting

   **Prompt options:**
   - If changes needed: "Sync now (recommended)", "Archive without syncing"
   - If already synced: "Archive now", "Sync anyway", "Cancel"

   If user chooses sync, use Task tool (subagent_type: "general-purpose", prompt: "Use Skill tool to invoke openspec-sync-specs for change '<name>'. Delta spec analysis: <include the analyzed delta spec summary>"). Proceed to archive regardless of choice.

5. **Perform the archive**

   Create an `archive` directory under `planningHome.changesDir` if it doesn't exist:
   ```bash
   mkdir -p "<planningHome.changesDir>/archive"
   ```

   Generate target name using current date: `YYYY-MM-DD-<change-name>`

   **Check if target already exists:**
   - If yes: Fail with error, suggest renaming existing archive or using different date
   - If no: Move `changeRoot` to the archive directory

   ```bash
   mv "<changeRoot>" "<planningHome.changesDir>/archive/YYYY-MM-DD-<name>"
   ```

6. **Display summary**

   Show archive completion summary including:
   - Change name
   - Schema that was used
   - Archive location
   - Spec sync status (synced / sync skipped / no delta specs)
   - Note about any warnings (incomplete artifacts/tasks)

**Output On Success**

```
## Archive Complete

**Change:** <change-name>
**Schema:** <schema-name>
**Archived to:** the archive path derived from `planningHome.changesDir`/YYYY-MM-DD-<name>/
**Specs:** ✓ Synced to main specs

All artifacts complete. All tasks complete.
```

**Output On Success (No Delta Specs)**

```
## Archive Complete

**Change:** <change-name>
**Schema:** <schema-name>
**Archived to:** the archive path derived from `planningHome.changesDir`/YYYY-MM-DD-<name>/
**Specs:** No delta specs

All artifacts complete. All tasks complete.
```

**Output On Success With Warnings**

```
## Archive Complete (with warnings)

**Change:** <change-name>
**Schema:** <schema-name>
**Archived to:** the archive path derived from `planningHome.changesDir`/YYYY-MM-DD-<name>/
**Specs:** Sync skipped (user chose to skip)

**Warnings:**
- Archived with 2 incomplete artifacts
- Archived with 3 incomplete tasks
- Delta spec sync was skipped (user chose to skip)

Review the archive if this was not intentional.
```

**Output On Error (Archive Exists)**

```
## Archive Failed

**Change:** <change-name>
**Target:** the archive path derived from `planningHome.changesDir`/YYYY-MM-DD-<name>/

Target archive directory already exists.

**Options:**
1. Rename the existing archive
2. Delete the existing archive if it's a duplicate
3. Wait until a different date to archive
```

**Guardrails**
- Always prompt for change selection if not provided
- Use artifact graph (openspec status --json) for completion checking
- Don't block archive on warnings - just inform and confirm
- Preserve .openspec.yaml when moving to archive (it moves with the directory)
- Show clear summary of what happened
- If sync is requested, use the Skill tool to invoke `openspec-sync-specs` (agent-driven)
- If delta specs exist, always run the sync assessment and show the combined summary before prompting
````

## File: .claude/commands/opsx/explore.md
````markdown
---
name: "OPSX: Explore"
description: "Enter explore mode - think through ideas, investigate problems, clarify requirements"
allowed-tools: Bash(openspec:*)
category: Workflow
tags: [workflow, explore, experimental, thinking]
---

Enter explore mode. Think deeply. Visualize freely. Follow the conversation wherever it goes.

**IMPORTANT: Explore mode is for thinking, not implementing.** You may read files, search code, and investigate the codebase, but you must NEVER write code or implement features. If the user asks you to implement something, remind them to exit explore mode first and create a change proposal. You MAY create OpenSpec artifacts (proposals, designs, specs) if the user asks—that's capturing thinking, not implementing.

**This is a stance, not a workflow.** There are no fixed steps, no required sequence, no mandatory outputs. You're a thinking partner helping the user explore.

**Store selection:** If the user names a store (a store is a standalone OpenSpec repo registered on this machine) or the work lives in one, run `openspec store list --json` to discover registered store ids, then pass `--store <id>` on the commands that read or write specs and changes (`new change`, `status`, `instructions`, `list`, `show`, `validate`, `archive`, `doctor`, `context`). Other commands do not take the flag. Hints printed by commands already carry the flag; keep it on follow-ups. Without a store, commands act on the nearest local `openspec/` root.

**Input**: The argument after `/opsx:explore` is whatever the user wants to think about. Could be:
- A vague idea: "real-time collaboration"
- A specific problem: "the auth system is getting unwieldy"
- A change name: "add-dark-mode" (to explore in context of that change)
- A comparison: "postgres vs sqlite for this"
- Nothing (just enter explore mode)

---

## The Stance

- **Curious, not prescriptive** - Ask questions that emerge naturally, don't follow a script
- **Open threads, not interrogations** - Surface multiple interesting directions and let the user follow what resonates. Don't funnel them through a single path of questions.
- **Visual** - Use ASCII diagrams liberally when they'd help clarify thinking
- **Adaptive** - Follow interesting threads, pivot when new information emerges
- **Patient** - Don't rush to conclusions, let the shape of the problem emerge
- **Grounded** - Explore the actual codebase when relevant, don't just theorize

---

## What You Might Do

Depending on what the user brings, you might:

**Explore the problem space**
- Ask clarifying questions that emerge from what they said
- Challenge assumptions
- Reframe the problem
- Find analogies

**Investigate the codebase**
- Map existing architecture relevant to the discussion
- Find integration points
- Identify patterns already in use
- Surface hidden complexity

**Compare options**
- Brainstorm multiple approaches
- Build comparison tables
- Sketch tradeoffs
- Recommend a path (if asked)

**Visualize**
```
┌─────────────────────────────────────────┐
│     Use ASCII diagrams liberally        │
├─────────────────────────────────────────┤
│                                         │
│      ┌────────┐         ┌────────┐      │
│      │ State  │────────▶│ State  │      │
│      │   A    │         │   B    │      │
│      └────────┘         └────────┘      │
│                                         │
│   System diagrams, state machines,      │
│   data flows, architecture sketches,    │
│   dependency graphs, comparison tables  │
│                                         │
└─────────────────────────────────────────┘
```

**Surface risks and unknowns**
- Identify what could go wrong
- Find gaps in understanding
- Suggest spikes or investigations

---

## OpenSpec Awareness

You have full context of the OpenSpec system. Use it naturally, don't force it.

### Check for context

At the start, quickly check what exists:
```bash
openspec list --json
```

This tells you:
- If there are active changes
- Their names, schemas, and status
- What the user might be working on

If the user mentioned a specific change name, read its artifacts for context.

### When no change exists

Think freely. When insights crystallize, you might offer:

- "This feels solid enough to start a change. Want me to create a proposal?"
- Or keep exploring - no pressure to formalize

### When a change exists

If the user mentions a change or you detect one is relevant:

1. **Resolve and read existing artifacts for context**
   - Run `openspec status --change "<name>" --json`.
   - Use `changeRoot`, `artifactPaths`, and `actionContext` from the status JSON.
   - Read existing files from `artifactPaths.<artifact>.existingOutputPaths`.

2. **Reference them naturally in conversation**
   - "Your design mentions using Redis, but we just realized SQLite fits better..."
   - "The proposal scopes this to premium users, but we're now thinking everyone..."

3. **Offer to capture when decisions are made**

    | Insight Type               | Where to Capture               |
    |----------------------------|--------------------------------|
    | New requirement discovered | `specs/<capability>/spec.md` |
    | Requirement changed        | `specs/<capability>/spec.md` |
    | Design decision made       | `design.md`                  |
    | Scope changed              | `proposal.md`                |
    | New work identified        | `tasks.md`                   |
    | Assumption invalidated     | Relevant artifact              |

   Example offers:
   - "That's a design decision. Capture it in design.md?"
   - "This is a new requirement. Add it to specs?"
   - "This changes scope. Update the proposal?"

4. **The user decides** - Offer and move on. Don't pressure. Don't auto-capture.

---

## What You Don't Have To Do

- Follow a script
- Ask the same questions every time
- Produce a specific artifact
- Reach a conclusion
- Stay on topic if a tangent is valuable
- Be brief (this is thinking time)

---

## Ending Discovery

There's no required ending. Discovery might:

- **Flow into a proposal**: "Ready to start? I can create a change proposal."
- **Result in artifact updates**: "Updated design.md with these decisions"
- **Just provide clarity**: User has what they need, moves on
- **Continue later**: "We can pick this up anytime"

When things crystallize, you might offer a summary - but it's optional. Sometimes the thinking IS the value.

---

## Guardrails

- **Don't implement** - Never write code or implement features. Creating OpenSpec artifacts is fine, writing application code is not.
- **Don't fake understanding** - If something is unclear, dig deeper
- **Don't rush** - Discovery is thinking time, not task time
- **Don't force structure** - Let patterns emerge naturally
- **Don't auto-capture** - Offer to save insights, don't just do it
- **Do visualize** - A good diagram is worth many paragraphs
- **Do explore the codebase** - Ground discussions in reality
- **Do question assumptions** - Including the user's and your own
````

## File: .claude/commands/opsx/propose.md
````markdown
---
name: "OPSX: Propose"
description: Propose a new change - create it and generate all artifacts in one step
allowed-tools: Bash(openspec:*)
category: Workflow
tags: [workflow, artifacts, experimental]
---

Propose a new change - create the change and generate all artifacts in one step.

I'll create a change with artifacts:
- proposal.md (what & why)
- design.md (how)
- tasks.md (implementation steps)

When ready to implement, run /opsx:apply

---

**Store selection:** If the user names a store (a store is a standalone OpenSpec repo registered on this machine) or the work lives in one, run `openspec store list --json` to discover registered store ids, then pass `--store <id>` on the commands that read or write specs and changes (`new change`, `status`, `instructions`, `list`, `show`, `validate`, `archive`, `doctor`, `context`). Other commands do not take the flag. Hints printed by commands already carry the flag; keep it on follow-ups. Without a store, commands act on the nearest local `openspec/` root.

**Input**: The argument after `/opsx:propose` is the change name (kebab-case), OR a description of what the user wants to build.

**Steps**

1. **If no input provided, ask what they want to build**

   Use the **AskUserQuestion tool** (open-ended, no preset options) to ask:
   > "What change do you want to work on? Describe what you want to build or fix."

   From their description, derive a kebab-case name (e.g., "add user authentication" → `add-user-auth`).

   **IMPORTANT**: Do NOT proceed without understanding what the user wants to build.

2. **Create the change directory**
   ```bash
   openspec new change "<name>"
   ```
   This creates a scaffolded change in the planning home resolved by the CLI with `.openspec.yaml`.

3. **Get the artifact build order**
   ```bash
   openspec status --change "<name>" --json
   ```
   Parse the JSON to get:
   - `applyRequires`: array of artifact IDs needed before implementation (e.g., `["tasks"]`)
   - `artifacts`: list of all artifacts with their status and dependencies
   - `planningHome`, `changeRoot`, `artifactPaths`, and `actionContext`: path and scope context. Use these instead of assuming repo-local paths.

4. **Create artifacts in sequence until apply-ready**

   Use the **TodoWrite tool** to track progress through the artifacts.

   Loop through artifacts in dependency order (artifacts with no pending dependencies first):

   a. **For each artifact that is `ready` (dependencies satisfied)**:
      - Get instructions:
        ```bash
        openspec instructions <artifact-id> --change "<name>" --json
        ```
      - The instructions JSON includes:
        - `context`: Project background (constraints for you - do NOT include in output)
        - `rules`: Artifact-specific rules (constraints for you - do NOT include in output)
        - `template`: The structure to use for your output file
        - `instruction`: Schema-specific guidance for this artifact type
        - `resolvedOutputPath`: Resolved path or pattern to write the artifact
        - `dependencies`: Completed artifacts to read for context
      - Read any completed dependency files for context
      - Create the artifact file using `template` as the structure and write it to `resolvedOutputPath`
      - Apply `context` and `rules` as constraints - but do NOT copy them into the file
      - Show brief progress: "Created <artifact-id>"

   b. **Continue until all `applyRequires` artifacts are complete**
      - After creating each artifact, re-run `openspec status --change "<name>" --json`
      - Check if every artifact ID in `applyRequires` has `status: "done"` in the artifacts array
      - Stop when all `applyRequires` artifacts are done

   c. **If an artifact requires user input** (unclear context):
      - Use **AskUserQuestion tool** to clarify
      - Then continue with creation

5. **Show final status**
   ```bash
   openspec status --change "<name>"
   ```

**Output**

After completing all artifacts, summarize:
- Change name and location
- List of artifacts created with brief descriptions
- What's ready: "All artifacts created! Ready for implementation."
- Prompt: "Run `/opsx:apply` to start implementing."

**Artifact Creation Guidelines**

- Follow the `instruction` field from `openspec instructions` for each artifact type
- The schema defines what each artifact should contain - follow it
- Read dependency artifacts for context before creating new ones
- Use `template` as the structure for your output file - fill in its sections
- **IMPORTANT**: `context` and `rules` are constraints for YOU, not content for the file
  - Do NOT copy `<context>`, `<rules>`, `<project_context>` blocks into the artifact
  - These guide what you write, but should never appear in the output

**Guardrails**
- Create ALL artifacts needed for implementation (as defined by schema's `apply.requires`)
- Always read dependency artifacts before creating a new one
- If context is critically unclear, ask the user - but prefer making reasonable decisions to keep momentum
- If a change with that name already exists, ask if user wants to continue it or create a new one
- Verify each artifact file exists after writing before proceeding to next
````

## File: .claude/commands/opsx/sync.md
````markdown
---
name: "OPSX: Sync"
description: Sync delta specs from a change to main specs
allowed-tools: Bash(openspec:*)
category: Workflow
tags: [workflow, specs, experimental]
---

Sync delta specs from a change to main specs.

This is an **agent-driven** operation - you will read delta specs and directly edit main specs to apply the changes. This allows intelligent merging (e.g., adding a scenario without copying the entire requirement).

**Store selection:** If the user names a store (a store is a standalone OpenSpec repo registered on this machine) or the work lives in one, run `openspec store list --json` to discover registered store ids, then pass `--store <id>` on the commands that read or write specs and changes (`new change`, `status`, `instructions`, `list`, `show`, `validate`, `archive`, `doctor`, `context`). Other commands do not take the flag. Hints printed by commands already carry the flag; keep it on follow-ups. Without a store, commands act on the nearest local `openspec/` root.

**Input**: Optionally specify a change name after `/opsx:sync` (e.g., `/opsx:sync add-auth`). If omitted, check if it can be inferred from conversation context. If vague or ambiguous you MUST prompt for available changes.

**Steps**

1. **If no change name provided, prompt for selection**

   Run `openspec list --json` to get available changes. Use the **AskUserQuestion tool** to let the user select.

   Show changes that have delta specs (under `specs/` directory).

   **IMPORTANT**: Do NOT guess or auto-select a change. Always let the user choose.

2. **Resolve change context**

   Run:
   ```bash
   openspec status --change "<name>" --json
   ```

3. **Find delta specs**

   Use `artifactPaths.specs.existingOutputPaths` from the status JSON as the list of delta spec files.

   Each delta spec file contains sections like:
   - `## ADDED Requirements` - New requirements to add
   - `## MODIFIED Requirements` - Changes to existing requirements
   - `## REMOVED Requirements` - Requirements to remove
   - `## RENAMED Requirements` - Requirements to rename (FROM:/TO: format)

   If no delta specs found, inform user and stop.

4. **For each delta spec, apply changes to main specs**

   For each repo-local capability delta spec path returned by the CLI:

   a. **Read the delta spec** to understand the intended changes

   b. **Read the main spec** at `openspec/specs/<capability>/spec.md` (may not exist yet)

   c. **Apply changes intelligently**:

      **ADDED Requirements:**
      - If requirement doesn't exist in main spec → add it
      - If requirement already exists → update it to match (treat as implicit MODIFIED)

      **MODIFIED Requirements:**
      - Find the requirement in main spec
      - Apply the changes - this can be:
        - Adding new scenarios (don't need to copy existing ones)
        - Modifying existing scenarios
        - Changing the requirement description
      - Preserve scenarios/content not mentioned in the delta

      **REMOVED Requirements:**
      - Remove the entire requirement block from main spec

      **RENAMED Requirements:**
      - Find the FROM requirement, rename to TO

   d. **Create new main spec** if capability doesn't exist yet:
      - Create `openspec/specs/<capability>/spec.md`
      - Add Purpose section (can be brief, mark as TBD)
      - Add Requirements section with the ADDED requirements

5. **Show summary**

   After applying all changes, summarize:
   - Which capabilities were updated
   - What changes were made (requirements added/modified/removed/renamed)

**Delta Spec Format Reference**

```markdown
## ADDED Requirements

### Requirement: New Feature
The system SHALL do something new.

#### Scenario: Basic case
- **WHEN** user does X
- **THEN** system does Y

## MODIFIED Requirements

### Requirement: Existing Feature
#### Scenario: New scenario to add
- **WHEN** user does A
- **THEN** system does B

## REMOVED Requirements

### Requirement: Deprecated Feature

## RENAMED Requirements

- FROM: `### Requirement: Old Name`
- TO: `### Requirement: New Name`
```

**Key Principle: Intelligent Merging**

Unlike programmatic merging, you can apply **partial updates**:
- To add a scenario, just include that scenario under MODIFIED - don't copy existing scenarios
- The delta represents *intent*, not a wholesale replacement
- Use your judgment to merge changes sensibly

**Output On Success**

```
## Specs Synced: <change-name>

Updated main specs:

**<capability-1>**:
- Added requirement: "New Feature"
- Modified requirement: "Existing Feature" (added 1 scenario)

**<capability-2>**:
- Created new spec file
- Added requirement: "Another Feature"

Main specs are now updated. The change remains active - archive when implementation is complete.
```

**Guardrails**
- Read both delta and main specs before making changes
- Preserve existing content not mentioned in delta
- If something is unclear, ask for clarification
- Show what you're changing as you go
- The operation should be idempotent - running twice should give same result
````

## File: .claude/commands/opsx/update.md
````markdown
---
name: "OPSX: Update"
description: Update a change - revise existing planning artifacts and keep them coherent (Experimental)
allowed-tools: Bash(openspec:*)
category: Workflow
tags: [workflow, artifacts, experimental]
---

Revise a change's existing planning artifacts and keep them coherent. Never edit code.

**Store selection:** If the user names a store (a store is a standalone OpenSpec repo registered on this machine) or the work lives in one, run `openspec store list --json` to discover registered store ids, then pass `--store <id>` on the commands that read or write specs and changes (`new change`, `status`, `instructions`, `list`, `show`, `validate`, `archive`, `doctor`, `context`). Other commands do not take the flag. Hints printed by commands already carry the flag; keep it on follow-ups. Without a store, commands act on the nearest local `openspec/` root.

**Input**: Optionally specify a change name after `/opsx:update` (e.g., `/opsx:update add-auth`). If omitted, check if it can be inferred from conversation context. If vague or ambiguous you MUST prompt for available changes.

**Steps**

1. **If no change name provided, prompt for selection**

   Run `openspec list --json` to get available changes sorted by most recently modified. Then use the **AskUserQuestion tool** to let the user select which change to update.

   Present the top 3-4 most recently modified changes as options, showing:
   - Change name
   - Schema (from `schema` field if present, otherwise "spec-driven")
   - Status (e.g., "0/5 tasks", "complete", "no tasks")
   - How recently it was modified (from `lastModified` field)

   Mark the most recently modified change as "(Recommended)" since it's likely what the user wants to update.

   **IMPORTANT**: Do NOT guess or auto-select a change. Always let the user choose.

2. **Get the change's artifacts**
   ```bash
   openspec status --change "<name>" --json
   ```
   Parse the JSON to understand current state. The response includes:
   - `schemaName`: The workflow schema being used (e.g., "spec-driven")
   - `artifacts`: Array of artifacts with their status ("done", "ready", "blocked")
   - `isComplete`: Boolean indicating if all artifacts are complete
   - `planningHome`, `changeRoot`, `artifactPaths`, and `actionContext`: path and scope context. Use these instead of assuming repo-local paths.

   The artifact ids and paths come from the active schema - do NOT assume them, and do NOT branch on hardcoded artifact names. Custom schemas must work unchanged.

   The files to edit are `artifactPaths.<id>.existingOutputPaths` - the concrete files that exist on disk, already glob-expanded for glob artifacts (e.g. `specs/**/*.md`). Do NOT write to `resolvedOutputPath`: for a glob artifact it is still the glob pattern, not a real file.

3. **Understand the request**
   - If the user asked for a specific revision ("the design now uses X"), that is the starting edit.
   - If they only said "update" / "make this coherent", treat it as a coherence review: read the existing artifacts and check them against each other for contradictions, gaps, and duplication.

4. **Read and reconcile**
   - Read the artifact(s) the request touches and the change's other existing artifacts.
   - Apply the requested edit. Then check every other existing artifact against it - in ANY direction: an edit to a later artifact may require revising an earlier one, not only the other way around. Build order is a useful reading order, not a constraint on which artifacts may be revised.
   - Note everything that is now inconsistent, missing, or contradictory.
   - Revise only files that already exist (`existingOutputPaths`). Do NOT create artifacts that don't exist yet, and do NOT invent new files under a glob artifact - note them and point the user to `/opsx:continue` to create them.
   - If the change is already coherent, say so and make no edits.

5. **Confirm and apply, one artifact at a time**
   - Show each proposed revision and why. Write only after the user confirms.
   - If the user rejects a revision, do not write it - leave that artifact unchanged.
   - When a substantial rewrite is needed, get that artifact's rules and template first:
     ```bash
     openspec instructions <artifact-id> --change "<name>" --json
     ```

6. **Point to the next step (guidance only - NEVER act on it)**
   - Artifacts still missing -> suggest `/opsx:continue` to create them.
   - Change already implemented (tasks checked off / already applied) -> the code may no longer match the revised plan; suggest `/opsx:apply` to carry the delta into code.
   - Everything done and implemented -> suggest `/opsx:archive`.

**Output**

After each invocation, show:
- Which artifacts were revised (and which proposed revisions were rejected)
- Anything deferred to `/opsx:continue` (not-yet-created artifacts or files)
- Where the change stands and the recommended next command

**Guardrails**
- Planning artifacts only - NEVER edit implementation code. If the revised plan implies code changes, stop and point to `/opsx:apply`.
- Use the artifact ids and paths reported by `openspec status`; never branch on hardcoded artifact names.
- Edit only the concrete files in `existingOutputPaths`; never write to a glob `resolvedOutputPath`.
- Do not advance the build frontier: no new artifacts, no new files under glob artifacts - that is `/opsx:continue`'s job.
- Confirm every edit with the user before writing.
- If the request changes the change's *intent* rather than refining it, recommend starting fresh with `/opsx:new` (the "Update vs. Start Fresh" heuristic).
````

## File: .claude/skills/openspec-apply-change/SKILL.md
````markdown
---
name: openspec-apply-change
description: Implement tasks from an OpenSpec change. Use when the user wants to start implementing, continue implementation, or work through tasks.
allowed-tools: Bash(openspec:*)
license: MIT
compatibility: Requires openspec CLI.
metadata:
  author: openspec
  version: "1.0"
  generatedBy: "1.6.0"
---

Implement tasks from an OpenSpec change.

**Store selection:** If the user names a store (a store is a standalone OpenSpec repo registered on this machine) or the work lives in one, run `openspec store list --json` to discover registered store ids, then pass `--store <id>` on the commands that read or write specs and changes (`new change`, `status`, `instructions`, `list`, `show`, `validate`, `archive`, `doctor`, `context`). Other commands do not take the flag. Hints printed by commands already carry the flag; keep it on follow-ups. Without a store, commands act on the nearest local `openspec/` root.

**Input**: Optionally specify a change name. If omitted, check if it can be inferred from conversation context. If vague or ambiguous you MUST prompt for available changes.

**Steps**

1. **Select the change**

   If a name is provided, use it. Otherwise:
   - Infer from conversation context if the user mentioned a change
   - Auto-select if only one active change exists
   - If ambiguous, run `openspec list --json` to get available changes and use the **AskUserQuestion tool** to let the user select

   Always announce: "Using change: <name>" and how to override (e.g., `/opsx:apply <other>`).

2. **Check status to understand the schema**
   ```bash
   openspec status --change "<name>" --json
   ```
   Parse the JSON to understand:
   - `schemaName`: The workflow being used (e.g., "spec-driven")
   - `planningHome`, `changeRoot`, and `actionContext`: planning scope and edit constraints
   - Which artifact contains the tasks (typically "tasks" for spec-driven, check status for others)

3. **Get apply instructions**

   ```bash
   openspec instructions apply --change "<name>" --json
   ```

   This returns:
   - `contextFiles`: artifact ID -> array of concrete file paths (varies by schema - could be proposal/specs/design/tasks or spec/tests/implementation/docs)
   - Progress (total, complete, remaining)
   - Task list with status
   - Dynamic instruction based on current state

   **Handle states:**
   - If `state: "blocked"` (missing artifacts): show message, suggest using openspec-continue-change
   - If `state: "all_done"`: congratulate, suggest archive
   - Otherwise: proceed to implementation

4. **Read context files**

   Read every file path listed under `contextFiles` from the apply instructions output.
   The files depend on the schema being used:
   - **spec-driven**: proposal, specs, design, tasks
   - Other schemas: follow the contextFiles from CLI output

5. **Show current progress**

   Display:
   - Schema being used
   - Progress: "N/M tasks complete"
   - Remaining tasks overview
   - Dynamic instruction from CLI

6. **Implement tasks (loop until done or blocked)**

   For each pending task:
   - Show which task is being worked on
   - Make the code changes required
   - Keep changes minimal and focused
   - Mark task complete in the tasks file: `- [ ]` → `- [x]`
   - Continue to next task

   **Pause if:**
   - Task is unclear → ask for clarification
   - Implementation reveals a design issue → suggest updating artifacts
   - Error or blocker encountered → report and wait for guidance
   - User interrupts

7. **On completion or pause, show status**

   Display:
   - Tasks completed this session
   - Overall progress: "N/M tasks complete"
   - If all done: suggest archive
   - If paused: explain why and wait for guidance

**Output During Implementation**

```
## Implementing: <change-name> (schema: <schema-name>)

Working on task 3/7: <task description>
[...implementation happening...]
✓ Task complete

Working on task 4/7: <task description>
[...implementation happening...]
✓ Task complete
```

**Output On Completion**

```
## Implementation Complete

**Change:** <change-name>
**Schema:** <schema-name>
**Progress:** 7/7 tasks complete ✓

### Completed This Session
- [x] Task 1
- [x] Task 2
...

All tasks complete! Ready to archive this change.
```

**Output On Pause (Issue Encountered)**

```
## Implementation Paused

**Change:** <change-name>
**Schema:** <schema-name>
**Progress:** 4/7 tasks complete

### Issue Encountered
<description of the issue>

**Options:**
1. <option 1>
2. <option 2>
3. Other approach

What would you like to do?
```

**Guardrails**
- Keep going through tasks until done or blocked
- Always read context files before starting (from the apply instructions output)
- If task is ambiguous, pause and ask before implementing
- If implementation reveals issues, pause and suggest artifact updates
- Keep code changes minimal and scoped to each task
- Update task checkbox immediately after completing each task
- Pause on errors, blockers, or unclear requirements - don't guess
- Use contextFiles from CLI output, don't assume specific file names

**Fluid Workflow Integration**

This skill supports the "actions on a change" model:

- **Can be invoked anytime**: Before all artifacts are done (if tasks exist), after partial implementation, interleaved with other actions
- **Allows artifact updates**: If implementation reveals design issues, suggest updating artifacts - not phase-locked, work fluidly
````

## File: .claude/skills/openspec-archive-change/SKILL.md
````markdown
---
name: openspec-archive-change
description: Archive a completed change in the experimental workflow. Use when the user wants to finalize and archive a change after implementation is complete.
allowed-tools: Bash(openspec:*)
license: MIT
compatibility: Requires openspec CLI.
metadata:
  author: openspec
  version: "1.0"
  generatedBy: "1.6.0"
---

Archive a completed change in the experimental workflow.

**Store selection:** If the user names a store (a store is a standalone OpenSpec repo registered on this machine) or the work lives in one, run `openspec store list --json` to discover registered store ids, then pass `--store <id>` on the commands that read or write specs and changes (`new change`, `status`, `instructions`, `list`, `show`, `validate`, `archive`, `doctor`, `context`). Other commands do not take the flag. Hints printed by commands already carry the flag; keep it on follow-ups. Without a store, commands act on the nearest local `openspec/` root.

**Input**: Optionally specify a change name. If omitted, check if it can be inferred from conversation context. If vague or ambiguous you MUST prompt for available changes.

**Steps**

1. **If no change name provided, prompt for selection**

   Run `openspec list --json` to get available changes. Use the **AskUserQuestion tool** to let the user select.

   Show only active changes (not already archived).
   Include the schema used for each change if available.

   **IMPORTANT**: Do NOT guess or auto-select a change. Always let the user choose.

2. **Check artifact completion status**

   Run `openspec status --change "<name>" --json` to check artifact completion.

   Parse the JSON to understand:
   - `schemaName`: The workflow being used
   - `planningHome`, `changeRoot`, `artifactPaths`, and `actionContext`: path and scope context
   - `artifacts`: List of artifacts with their status (`done` or other)

   **If any artifacts are not `done`:**
   - Display warning listing incomplete artifacts
   - Use **AskUserQuestion tool** to confirm user wants to proceed
   - Proceed if user confirms

3. **Check task completion status**

   Read the tasks file (typically `tasks.md`) to check for incomplete tasks.

   Count tasks marked with `- [ ]` (incomplete) vs `- [x]` (complete).

   **If incomplete tasks found:**
   - Display warning showing count of incomplete tasks
   - Use **AskUserQuestion tool** to confirm user wants to proceed
   - Proceed if user confirms

   **If no tasks file exists:** Proceed without task-related warning.

4. **Assess delta spec sync state**

   Use `artifactPaths.specs.existingOutputPaths` from status JSON to check for delta specs. If none exist, proceed without sync prompt.

   **If delta specs exist:**
   - Compare each delta spec with its corresponding main spec at `openspec/specs/<capability>/spec.md`
   - Determine what changes would be applied (adds, modifications, removals, renames)
   - Show a combined summary before prompting

   **Prompt options:**
   - If changes needed: "Sync now (recommended)", "Archive without syncing"
   - If already synced: "Archive now", "Sync anyway", "Cancel"

   If user chooses sync, use Task tool (subagent_type: "general-purpose", prompt: "Use Skill tool to invoke openspec-sync-specs for change '<name>'. Delta spec analysis: <include the analyzed delta spec summary>"). Proceed to archive regardless of choice.

5. **Perform the archive**

   Create an `archive` directory under `planningHome.changesDir` if it doesn't exist:
   ```bash
   mkdir -p "<planningHome.changesDir>/archive"
   ```

   Generate target name using current date: `YYYY-MM-DD-<change-name>`

   **Check if target already exists:**
   - If yes: Fail with error, suggest renaming existing archive or using different date
   - If no: Move `changeRoot` to the archive directory

   ```bash
   mv "<changeRoot>" "<planningHome.changesDir>/archive/YYYY-MM-DD-<name>"
   ```

6. **Display summary**

   Show archive completion summary including:
   - Change name
   - Schema that was used
   - Archive location
   - Whether specs were synced (if applicable)
   - Note about any warnings (incomplete artifacts/tasks)

**Output On Success**

```
## Archive Complete

**Change:** <change-name>
**Schema:** <schema-name>
**Archived to:** the archive path derived from `planningHome.changesDir`/YYYY-MM-DD-<name>/
**Specs:** ✓ Synced to main specs (or "No delta specs" or "Sync skipped")

All artifacts complete. All tasks complete.
```

**Guardrails**
- Always prompt for change selection if not provided
- Use artifact graph (openspec status --json) for completion checking
- Don't block archive on warnings - just inform and confirm
- Preserve .openspec.yaml when moving to archive (it moves with the directory)
- Show clear summary of what happened
- If sync is requested, use openspec-sync-specs approach (agent-driven)
- If delta specs exist, always run the sync assessment and show the combined summary before prompting
````

## File: .claude/skills/openspec-explore/SKILL.md
````markdown
---
name: openspec-explore
description: Enter explore mode - a thinking partner for exploring ideas, investigating problems, and clarifying requirements. Use when the user wants to think through something before or during a change.
allowed-tools: Bash(openspec:*)
license: MIT
compatibility: Requires openspec CLI.
metadata:
  author: openspec
  version: "1.0"
  generatedBy: "1.6.0"
---

Enter explore mode. Think deeply. Visualize freely. Follow the conversation wherever it goes.

**IMPORTANT: Explore mode is for thinking, not implementing.** You may read files, search code, and investigate the codebase, but you must NEVER write code or implement features. If the user asks you to implement something, remind them to exit explore mode first and create a change proposal. You MAY create OpenSpec artifacts (proposals, designs, specs) if the user asks—that's capturing thinking, not implementing.

**This is a stance, not a workflow.** There are no fixed steps, no required sequence, no mandatory outputs. You're a thinking partner helping the user explore.

**Store selection:** If the user names a store (a store is a standalone OpenSpec repo registered on this machine) or the work lives in one, run `openspec store list --json` to discover registered store ids, then pass `--store <id>` on the commands that read or write specs and changes (`new change`, `status`, `instructions`, `list`, `show`, `validate`, `archive`, `doctor`, `context`). Other commands do not take the flag. Hints printed by commands already carry the flag; keep it on follow-ups. Without a store, commands act on the nearest local `openspec/` root.

---

## The Stance

- **Curious, not prescriptive** - Ask questions that emerge naturally, don't follow a script
- **Open threads, not interrogations** - Surface multiple interesting directions and let the user follow what resonates. Don't funnel them through a single path of questions.
- **Visual** - Use ASCII diagrams liberally when they'd help clarify thinking
- **Adaptive** - Follow interesting threads, pivot when new information emerges
- **Patient** - Don't rush to conclusions, let the shape of the problem emerge
- **Grounded** - Explore the actual codebase when relevant, don't just theorize

---

## What You Might Do

Depending on what the user brings, you might:

**Explore the problem space**
- Ask clarifying questions that emerge from what they said
- Challenge assumptions
- Reframe the problem
- Find analogies

**Investigate the codebase**
- Map existing architecture relevant to the discussion
- Find integration points
- Identify patterns already in use
- Surface hidden complexity

**Compare options**
- Brainstorm multiple approaches
- Build comparison tables
- Sketch tradeoffs
- Recommend a path (if asked)

**Visualize**
```
┌─────────────────────────────────────────┐
│     Use ASCII diagrams liberally        │
├─────────────────────────────────────────┤
│                                         │
│      ┌────────┐         ┌────────┐      │
│      │ State  │────────▶│ State  │      │
│      │   A    │         │   B    │      │
│      └────────┘         └────────┘      │
│                                         │
│   System diagrams, state machines,      │
│   data flows, architecture sketches,    │
│   dependency graphs, comparison tables  │
│                                         │
└─────────────────────────────────────────┘
```

**Surface risks and unknowns**
- Identify what could go wrong
- Find gaps in understanding
- Suggest spikes or investigations

---

## OpenSpec Awareness

You have full context of the OpenSpec system. Use it naturally, don't force it.

### Check for context

At the start, quickly check what exists:
```bash
openspec list --json
```

This tells you:
- If there are active changes
- Their names, schemas, and status
- What the user might be working on

### When no change exists

Think freely. When insights crystallize, you might offer:

- "This feels solid enough to start a change. Want me to create a proposal?"
- Or keep exploring - no pressure to formalize

### When a change exists

If the user mentions a change or you detect one is relevant:

1. **Resolve and read existing artifacts for context**
   - Run `openspec status --change "<name>" --json`.
   - Use `changeRoot`, `artifactPaths`, and `actionContext` from the status JSON.
   - Read existing files from `artifactPaths.<artifact>.existingOutputPaths`.

2. **Reference them naturally in conversation**
   - "Your design mentions using Redis, but we just realized SQLite fits better..."
   - "The proposal scopes this to premium users, but we're now thinking everyone..."

3. **Offer to capture when decisions are made**

    | Insight Type               | Where to Capture               |
    |----------------------------|--------------------------------|
    | New requirement discovered | `specs/<capability>/spec.md` |
    | Requirement changed        | `specs/<capability>/spec.md` |
    | Design decision made       | `design.md`                  |
    | Scope changed              | `proposal.md`                |
    | New work identified        | `tasks.md`                   |
    | Assumption invalidated     | Relevant artifact              |

   Example offers:
   - "That's a design decision. Capture it in design.md?"
   - "This is a new requirement. Add it to specs?"
   - "This changes scope. Update the proposal?"

4. **The user decides** - Offer and move on. Don't pressure. Don't auto-capture.

---

## What You Don't Have To Do

- Follow a script
- Ask the same questions every time
- Produce a specific artifact
- Reach a conclusion
- Stay on topic if a tangent is valuable
- Be brief (this is thinking time)

---

## Handling Different Entry Points

**User brings a vague idea:**
```
User: I'm thinking about adding real-time collaboration

You: Real-time collab is a big space. Let me think about this...

      COLLABORATION SPECTRUM
      ════════════════════════════════════════════

      Awareness          Coordination         Sync
          │                   │                 │
          ▼                   ▼                 ▼
      ┌────────┐         ┌────────┐        ┌────────┐
      │Presence│         │Cursors │        │  CRDT  │
      │  "3    │         │ Multi  │        │Conflict│
      │online" │         │ select │        │  free  │
      └────────┘         └────────┘        └────────┘
          │                   │                 │
       trivial            moderate           complex

      Where's your head at?
```

**User brings a specific problem:**
```
User: The auth system is a mess

You: [reads codebase]

     ┌─────────────────────────────────────────────┐
     │              CURRENT AUTH FLOW              │
     └─────────────────────────────────────────────┘
                        │
          ┌─────────────┼─────────────┐
          ▼             ▼             ▼
     ┌─────────┐  ┌─────────┐  ┌─────────┐
     │ Google  │  │  GitHub │  │  Email  │
     │  OAuth  │  │  OAuth  │  │  Magic  │
     └────┬────┘  └────┬────┘  └────┬────┘
          │             │             │
          └─────────────┼─────────────┘
                        ▼
                  ┌───────────┐
                  │  Session  │
                  └─────┬─────┘
                        │
                        ▼
                  ┌───────────┐
                  │   Perms   │
                  └───────────┘

     I see three tangles. Which one's burning?
```

**User is stuck mid-implementation:**
```
User: /opsx:explore add-auth-system
      The OAuth integration is more complex than expected

You: [reads change artifacts]

     You're on task 4: "Implement OAuth flow"

     Let me trace what's involved...

     [draws diagram, explores options, suggests paths]

     Want to update the design to reflect this?
     Or add a spike task to investigate?
```

**User wants to compare options:**
```
User: Should we use Postgres or SQLite?

You: Generic answer is boring. What's the context?

User: A CLI tool that tracks local dev environments

You: That changes everything.

     ┌─────────────────────────────────────────────────┐
     │          CLI TOOL DATA STORAGE                  │
     └─────────────────────────────────────────────────┘

     Key constraints:
     • No daemon running
     • Must work offline
     • Single user

                  SQLite          Postgres
     Deployment   embedded ✓      needs server ✗
     Offline      yes ✓           no ✗
     Single file  yes ✓           no ✗

     SQLite. Not even close.

     Unless... is there a sync component?
```

---

## Ending Discovery

There's no required ending. Discovery might:

- **Flow into a proposal**: "Ready to start? I can create a change proposal."
- **Result in artifact updates**: "Updated design.md with these decisions"
- **Just provide clarity**: User has what they need, moves on
- **Continue later**: "We can pick this up anytime"

When it feels like things are crystallizing, you might summarize:

```
## What We Figured Out

**The problem**: [crystallized understanding]

**The approach**: [if one emerged]

**Open questions**: [if any remain]

**Next steps** (if ready):
- Create a change proposal
- Keep exploring: just keep talking
```

But this summary is optional. Sometimes the thinking IS the value.

---

## Guardrails

- **Don't implement** - Never write code or implement features. Creating OpenSpec artifacts is fine, writing application code is not.
- **Don't fake understanding** - If something is unclear, dig deeper
- **Don't rush** - Discovery is thinking time, not task time
- **Don't force structure** - Let patterns emerge naturally
- **Don't auto-capture** - Offer to save insights, don't just do it
- **Do visualize** - A good diagram is worth many paragraphs
- **Do explore the codebase** - Ground discussions in reality
- **Do question assumptions** - Including the user's and your own
````

## File: .claude/skills/openspec-propose/SKILL.md
````markdown
---
name: openspec-propose
description: Propose a new change with all artifacts generated in one step. Use when the user wants to quickly describe what they want to build and get a complete proposal with design, specs, and tasks ready for implementation.
allowed-tools: Bash(openspec:*)
license: MIT
compatibility: Requires openspec CLI.
metadata:
  author: openspec
  version: "1.0"
  generatedBy: "1.6.0"
---

Propose a new change - create the change and generate all artifacts in one step.

I'll create a change with artifacts:
- proposal.md (what & why)
- design.md (how)
- tasks.md (implementation steps)

When ready to implement, run /opsx:apply

---

**Store selection:** If the user names a store (a store is a standalone OpenSpec repo registered on this machine) or the work lives in one, run `openspec store list --json` to discover registered store ids, then pass `--store <id>` on the commands that read or write specs and changes (`new change`, `status`, `instructions`, `list`, `show`, `validate`, `archive`, `doctor`, `context`). Other commands do not take the flag. Hints printed by commands already carry the flag; keep it on follow-ups. Without a store, commands act on the nearest local `openspec/` root.

**Input**: The user's request should include a change name (kebab-case) OR a description of what they want to build.

**Steps**

1. **If no clear input provided, ask what they want to build**

   Use the **AskUserQuestion tool** (open-ended, no preset options) to ask:
   > "What change do you want to work on? Describe what you want to build or fix."

   From their description, derive a kebab-case name (e.g., "add user authentication" → `add-user-auth`).

   **IMPORTANT**: Do NOT proceed without understanding what the user wants to build.

2. **Create the change directory**
   ```bash
   openspec new change "<name>"
   ```
   This creates a scaffolded change in the planning home resolved by the CLI with `.openspec.yaml`.

3. **Get the artifact build order**
   ```bash
   openspec status --change "<name>" --json
   ```
   Parse the JSON to get:
   - `applyRequires`: array of artifact IDs needed before implementation (e.g., `["tasks"]`)
   - `artifacts`: list of all artifacts with their status and dependencies
   - `planningHome`, `changeRoot`, `artifactPaths`, and `actionContext`: path and scope context. Use these instead of assuming repo-local paths.

4. **Create artifacts in sequence until apply-ready**

   Use the **TodoWrite tool** to track progress through the artifacts.

   Loop through artifacts in dependency order (artifacts with no pending dependencies first):

   a. **For each artifact that is `ready` (dependencies satisfied)**:
      - Get instructions:
        ```bash
        openspec instructions <artifact-id> --change "<name>" --json
        ```
      - The instructions JSON includes:
        - `context`: Project background (constraints for you - do NOT include in output)
        - `rules`: Artifact-specific rules (constraints for you - do NOT include in output)
        - `template`: The structure to use for your output file
        - `instruction`: Schema-specific guidance for this artifact type
        - `resolvedOutputPath`: Resolved path or pattern to write the artifact
        - `dependencies`: Completed artifacts to read for context
      - Read any completed dependency files for context
      - Create the artifact file using `template` as the structure and write it to `resolvedOutputPath`
      - Apply `context` and `rules` as constraints - but do NOT copy them into the file
      - Show brief progress: "Created <artifact-id>"

   b. **Continue until all `applyRequires` artifacts are complete**
      - After creating each artifact, re-run `openspec status --change "<name>" --json`
      - Check if every artifact ID in `applyRequires` has `status: "done"` in the artifacts array
      - Stop when all `applyRequires` artifacts are done

   c. **If an artifact requires user input** (unclear context):
      - Use **AskUserQuestion tool** to clarify
      - Then continue with creation

5. **Show final status**
   ```bash
   openspec status --change "<name>"
   ```

**Output**

After completing all artifacts, summarize:
- Change name and location
- List of artifacts created with brief descriptions
- What's ready: "All artifacts created! Ready for implementation."
- Prompt: "Run `/opsx:apply` or ask me to implement to start working on the tasks."

**Artifact Creation Guidelines**

- Follow the `instruction` field from `openspec instructions` for each artifact type
- The schema defines what each artifact should contain - follow it
- Read dependency artifacts for context before creating new ones
- Use `template` as the structure for your output file - fill in its sections
- **IMPORTANT**: `context` and `rules` are constraints for YOU, not content for the file
  - Do NOT copy `<context>`, `<rules>`, `<project_context>` blocks into the artifact
  - These guide what you write, but should never appear in the output

**Guardrails**
- Create ALL artifacts needed for implementation (as defined by schema's `apply.requires`)
- Always read dependency artifacts before creating a new one
- If context is critically unclear, ask the user - but prefer making reasonable decisions to keep momentum
- If a change with that name already exists, ask if user wants to continue it or create a new one
- Verify each artifact file exists after writing before proceeding to next
````

## File: .claude/skills/openspec-sync-specs/SKILL.md
````markdown
---
name: openspec-sync-specs
description: Sync delta specs from a change to main specs. Use when the user wants to update main specs with changes from a delta spec, without archiving the change.
allowed-tools: Bash(openspec:*)
license: MIT
compatibility: Requires openspec CLI.
metadata:
  author: openspec
  version: "1.0"
  generatedBy: "1.6.0"
---

Sync delta specs from a change to main specs.

This is an **agent-driven** operation - you will read delta specs and directly edit main specs to apply the changes. This allows intelligent merging (e.g., adding a scenario without copying the entire requirement).

**Store selection:** If the user names a store (a store is a standalone OpenSpec repo registered on this machine) or the work lives in one, run `openspec store list --json` to discover registered store ids, then pass `--store <id>` on the commands that read or write specs and changes (`new change`, `status`, `instructions`, `list`, `show`, `validate`, `archive`, `doctor`, `context`). Other commands do not take the flag. Hints printed by commands already carry the flag; keep it on follow-ups. Without a store, commands act on the nearest local `openspec/` root.

**Input**: Optionally specify a change name. If omitted, check if it can be inferred from conversation context. If vague or ambiguous you MUST prompt for available changes.

**Steps**

1. **If no change name provided, prompt for selection**

   Run `openspec list --json` to get available changes. Use the **AskUserQuestion tool** to let the user select.

   Show changes that have delta specs (under `specs/` directory).

   **IMPORTANT**: Do NOT guess or auto-select a change. Always let the user choose.

2. **Resolve change context**

   Run:
   ```bash
   openspec status --change "<name>" --json
   ```

3. **Find delta specs**

   Use `artifactPaths.specs.existingOutputPaths` from the status JSON as the list of delta spec files.

   Each delta spec file contains sections like:
   - `## ADDED Requirements` - New requirements to add
   - `## MODIFIED Requirements` - Changes to existing requirements
   - `## REMOVED Requirements` - Requirements to remove
   - `## RENAMED Requirements` - Requirements to rename (FROM:/TO: format)

   If no delta specs found, inform user and stop.

4. **For each delta spec, apply changes to main specs**

   For each repo-local capability delta spec path returned by the CLI:

   a. **Read the delta spec** to understand the intended changes

   b. **Read the main spec** at `openspec/specs/<capability>/spec.md` (may not exist yet)

   c. **Apply changes intelligently**:

      **ADDED Requirements:**
      - If requirement doesn't exist in main spec → add it
      - If requirement already exists → update it to match (treat as implicit MODIFIED)

      **MODIFIED Requirements:**
      - Find the requirement in main spec
      - Apply the changes - this can be:
        - Adding new scenarios (don't need to copy existing ones)
        - Modifying existing scenarios
        - Changing the requirement description
      - Preserve scenarios/content not mentioned in the delta

      **REMOVED Requirements:**
      - Remove the entire requirement block from main spec

      **RENAMED Requirements:**
      - Find the FROM requirement, rename to TO

   d. **Create new main spec** if capability doesn't exist yet:
      - Create `openspec/specs/<capability>/spec.md`
      - Add Purpose section (can be brief, mark as TBD)
      - Add Requirements section with the ADDED requirements

5. **Show summary**

   After applying all changes, summarize:
   - Which capabilities were updated
   - What changes were made (requirements added/modified/removed/renamed)

**Delta Spec Format Reference**

```markdown
## ADDED Requirements

### Requirement: New Feature
The system SHALL do something new.

#### Scenario: Basic case
- **WHEN** user does X
- **THEN** system does Y

## MODIFIED Requirements

### Requirement: Existing Feature
#### Scenario: New scenario to add
- **WHEN** user does A
- **THEN** system does B

## REMOVED Requirements

### Requirement: Deprecated Feature

## RENAMED Requirements

- FROM: `### Requirement: Old Name`
- TO: `### Requirement: New Name`
```

**Key Principle: Intelligent Merging**

Unlike programmatic merging, you can apply **partial updates**:
- To add a scenario, just include that scenario under MODIFIED - don't copy existing scenarios
- The delta represents *intent*, not a wholesale replacement
- Use your judgment to merge changes sensibly

**Output On Success**

```
## Specs Synced: <change-name>

Updated main specs:

**<capability-1>**:
- Added requirement: "New Feature"
- Modified requirement: "Existing Feature" (added 1 scenario)

**<capability-2>**:
- Created new spec file
- Added requirement: "Another Feature"

Main specs are now updated. The change remains active - archive when implementation is complete.
```

**Guardrails**
- Read both delta and main specs before making changes
- Preserve existing content not mentioned in delta
- If something is unclear, ask for clarification
- Show what you're changing as you go
- The operation should be idempotent - running twice should give same result
````

## File: .claude/skills/openspec-update-change/SKILL.md
````markdown
---
name: openspec-update-change
description: Update an OpenSpec change by revising its existing planning artifacts and keeping them coherent with one another. Use when the user wants to revise a change's plan, fold new decisions into it, or reconcile its artifacts after an edit. Never edits code.
allowed-tools: Bash(openspec:*)
license: MIT
compatibility: Requires openspec CLI.
metadata:
  author: openspec
  version: "1.0"
  generatedBy: "1.6.0"
---

Revise a change's existing planning artifacts and keep them coherent. Never edit code.

**Store selection:** If the user names a store (a store is a standalone OpenSpec repo registered on this machine) or the work lives in one, run `openspec store list --json` to discover registered store ids, then pass `--store <id>` on the commands that read or write specs and changes (`new change`, `status`, `instructions`, `list`, `show`, `validate`, `archive`, `doctor`, `context`). Other commands do not take the flag. Hints printed by commands already carry the flag; keep it on follow-ups. Without a store, commands act on the nearest local `openspec/` root.

**Input**: Optionally specify a change name. If omitted, check if it can be inferred from conversation context. If vague or ambiguous you MUST prompt for available changes.

**Steps**

1. **If no change name provided, prompt for selection**

   Run `openspec list --json` to get available changes sorted by most recently modified. Then use the **AskUserQuestion tool** to let the user select which change to update.

   Present the top 3-4 most recently modified changes as options, showing:
   - Change name
   - Schema (from `schema` field if present, otherwise "spec-driven")
   - Status (e.g., "0/5 tasks", "complete", "no tasks")
   - How recently it was modified (from `lastModified` field)

   Mark the most recently modified change as "(Recommended)" since it's likely what the user wants to update.

   **IMPORTANT**: Do NOT guess or auto-select a change. Always let the user choose.

2. **Get the change's artifacts**
   ```bash
   openspec status --change "<name>" --json
   ```
   Parse the JSON to understand current state. The response includes:
   - `schemaName`: The workflow schema being used (e.g., "spec-driven")
   - `artifacts`: Array of artifacts with their status ("done", "ready", "blocked")
   - `isComplete`: Boolean indicating if all artifacts are complete
   - `planningHome`, `changeRoot`, `artifactPaths`, and `actionContext`: path and scope context. Use these instead of assuming repo-local paths.

   The artifact ids and paths come from the active schema - do NOT assume them, and do NOT branch on hardcoded artifact names. Custom schemas must work unchanged.

   The files to edit are `artifactPaths.<id>.existingOutputPaths` - the concrete files that exist on disk, already glob-expanded for glob artifacts (e.g. `specs/**/*.md`). Do NOT write to `resolvedOutputPath`: for a glob artifact it is still the glob pattern, not a real file.

3. **Understand the request**
   - If the user asked for a specific revision ("the design now uses X"), that is the starting edit.
   - If they only said "update" / "make this coherent", treat it as a coherence review: read the existing artifacts and check them against each other for contradictions, gaps, and duplication.

4. **Read and reconcile**
   - Read the artifact(s) the request touches and the change's other existing artifacts.
   - Apply the requested edit. Then check every other existing artifact against it - in ANY direction: an edit to a later artifact may require revising an earlier one, not only the other way around. Build order is a useful reading order, not a constraint on which artifacts may be revised.
   - Note everything that is now inconsistent, missing, or contradictory.
   - Revise only files that already exist (`existingOutputPaths`). Do NOT create artifacts that don't exist yet, and do NOT invent new files under a glob artifact - note them and point the user to `/opsx:continue` to create them.
   - If the change is already coherent, say so and make no edits.

5. **Confirm and apply, one artifact at a time**
   - Show each proposed revision and why. Write only after the user confirms.
   - If the user rejects a revision, do not write it - leave that artifact unchanged.
   - When a substantial rewrite is needed, get that artifact's rules and template first:
     ```bash
     openspec instructions <artifact-id> --change "<name>" --json
     ```

6. **Point to the next step (guidance only - NEVER act on it)**
   - Artifacts still missing -> suggest `/opsx:continue` to create them.
   - Change already implemented (tasks checked off / already applied) -> the code may no longer match the revised plan; suggest `/opsx:apply` to carry the delta into code.
   - Everything done and implemented -> suggest `/opsx:archive`.

**Output**

After each invocation, show:
- Which artifacts were revised (and which proposed revisions were rejected)
- Anything deferred to `/opsx:continue` (not-yet-created artifacts or files)
- Where the change stands and the recommended next command

**Guardrails**
- Planning artifacts only - NEVER edit implementation code. If the revised plan implies code changes, stop and point to `/opsx:apply`.
- Use the artifact ids and paths reported by `openspec status`; never branch on hardcoded artifact names.
- Edit only the concrete files in `existingOutputPaths`; never write to a glob `resolvedOutputPath`.
- Do not advance the build frontier: no new artifacts, no new files under glob artifacts - that is `/opsx:continue`'s job.
- Confirm every edit with the user before writing.
- If the request changes the change's *intent* rather than refining it, recommend starting fresh with `/opsx:new` (the "Update vs. Start Fresh" heuristic).
````

## File: .mvn/wrapper/maven-wrapper.properties
````
wrapperVersion=3.3.4
distributionType=only-script
distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.11/apache-maven-3.9.11-bin.zip
````

## File: openspec/config.yaml
````yaml
schema: spec-driven

# Project context (optional)
# This is shown to AI when creating artifacts.
# Add your tech stack, conventions, style guides, domain knowledge, etc.
# Example:
#   context: |
#     Tech stack: TypeScript, React, Node.js
#     We use conventional commits
#     Domain: e-commerce platform

# Per-artifact rules (optional)
# Add custom rules for specific artifacts.
# Example:
#   rules:
#     proposal:
#       - Keep proposals under 500 words
#       - Always include a "Non-goals" section
#     tasks:
#       - Break tasks into chunks of max 2 hours
````

## File: src/main/java/com/athletecore/api/user/dto/CreateUserRequest.java
````java
package com.athletecore.api.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateUserRequest {

    @NotBlank(message = "Username is mandatory")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @NotBlank(message = "Email is mandatory")
    @Email(message = "Email should be valid")
    private String email;

    @NotBlank(message = "Password is mandatory")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    @NotBlank(message = "Password confirmation is mandatory")
    private String confirmPassword;

    public CreateUserRequest() {}

    public CreateUserRequest(String username, String email, String password, String confirmPassword) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.confirmPassword = confirmPassword;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    public boolean isPasswordConfirmed() {
        return password != null && password.equals(confirmPassword);
    }
}
````

## File: src/main/java/com/athletecore/api/AthletecoreApiApplication.java
````java
package com.athletecore.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AthletecoreApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(AthletecoreApiApplication.class, args);
	}

}
````

## File: src/test/java/com/athletecore/api/AthletecoreApiApplicationTests.java
````java
package com.athletecore.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AthletecoreApiApplicationTests {

	@Test
	void contextLoads() {
	}

}
````

## File: .gitattributes
````
/mvnw text eol=lf
*.cmd text eol=crlf
````

## File: .gitignore
````
HELP.md
target/
.mvn/wrapper/maven-wrapper.jar
!**/src/main/**/target/
!**/src/test/**/target/

### STS ###
.apt_generated
.classpath
.factorypath
.project
.settings
.springBeans
.sts4-cache

### IntelliJ IDEA ###
.idea
*.iws
*.iml
*.ipr

### NetBeans ###
/nbproject/private/
/nbbuild/
/dist/
/nbdist/
/.nb-gradle/
build/
!**/src/main/**/build/
!**/src/test/**/build/

### VS Code ###
.vscode/

### IDE ###
# NetBeans
/nbproject/private/
/nbbuild/
/dist/
/nbdist/
/.nb-gradle/
build/
!**/src/main/**/build/
!**/src/test/**/build/

# IntelliJ IDEA
.idea/
*.iml
*.ipr
*.iws
/.settings/
/.project
/.classpath
.springBeans
.sts4-cache

# Eclipse
.apt_generated
.classpath
.factorypath
.settings/

### Build directories ###
# Maven
/target/
!**/src/main/**/target/
!**/src/test/**/target/

# Gradle
.gradle/
/build/

### Environment/Secrets ###
.env
.env.local
.env.production

### Local Configuration Files ###
application-local.properties
application-*.properties
!**/application.properties

### OS ###
.DS_Store
Thumbs.db

### Logs ###
*.log
logs/
*/logs/

### Temp files ###
*.tmp
*.temp
.cache/
````

## File: mvnw.cmd
````batch
<# : batch portion
@REM ----------------------------------------------------------------------------
@REM Licensed to the Apache Software Foundation (ASF) under one
@REM or more contributor license agreements.  See the NOTICE file
@REM distributed with this work for additional information
@REM regarding copyright ownership.  The ASF licenses this file
@REM to you under the Apache License, Version 2.0 (the
@REM "License"); you may not use this file except in compliance
@REM with the License.  You may obtain a copy of the License at
@REM
@REM    http://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing,
@REM software distributed under the License is distributed on an
@REM "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
@REM KIND, either express or implied.  See the License for the
@REM specific language governing permissions and limitations
@REM under the License.
@REM ----------------------------------------------------------------------------

@REM ----------------------------------------------------------------------------
@REM Apache Maven Wrapper startup batch script, version 3.3.4
@REM
@REM Optional ENV vars
@REM   MVNW_REPOURL - repo url base for downloading maven distribution
@REM   MVNW_USERNAME/MVNW_PASSWORD - user and password for downloading maven
@REM   MVNW_VERBOSE - true: enable verbose log; others: silence the output
@REM ----------------------------------------------------------------------------

@IF "%__MVNW_ARG0_NAME__%"=="" (SET __MVNW_ARG0_NAME__=%~nx0)
@SET __MVNW_CMD__=
@SET __MVNW_ERROR__=
@SET __MVNW_PSMODULEP_SAVE=%PSModulePath%
@SET PSModulePath=
@FOR /F "usebackq tokens=1* delims==" %%A IN (`powershell -noprofile "& {$scriptDir='%~dp0'; $script='%__MVNW_ARG0_NAME__%'; icm -ScriptBlock ([Scriptblock]::Create((Get-Content -Raw '%~f0'))) -NoNewScope}"`) DO @(
  IF "%%A"=="MVN_CMD" (set __MVNW_CMD__=%%B) ELSE IF "%%B"=="" (echo %%A) ELSE (echo %%A=%%B)
)
@SET PSModulePath=%__MVNW_PSMODULEP_SAVE%
@SET __MVNW_PSMODULEP_SAVE=
@SET __MVNW_ARG0_NAME__=
@SET MVNW_USERNAME=
@SET MVNW_PASSWORD=
@IF NOT "%__MVNW_CMD__%"=="" ("%__MVNW_CMD__%" %*)
@echo Cannot start maven from wrapper >&2 && exit /b 1
@GOTO :EOF
: end batch / begin powershell #>

$ErrorActionPreference = "Stop"
if ($env:MVNW_VERBOSE -eq "true") {
  $VerbosePreference = "Continue"
}

# calculate distributionUrl, requires .mvn/wrapper/maven-wrapper.properties
$distributionUrl = (Get-Content -Raw "$scriptDir/.mvn/wrapper/maven-wrapper.properties" | ConvertFrom-StringData).distributionUrl
if (!$distributionUrl) {
  Write-Error "cannot read distributionUrl property in $scriptDir/.mvn/wrapper/maven-wrapper.properties"
}

switch -wildcard -casesensitive ( $($distributionUrl -replace '^.*/','') ) {
  "maven-mvnd-*" {
    $USE_MVND = $true
    $distributionUrl = $distributionUrl -replace '-bin\.[^.]*$',"-windows-amd64.zip"
    $MVN_CMD = "mvnd.cmd"
    break
  }
  default {
    $USE_MVND = $false
    $MVN_CMD = $script -replace '^mvnw','mvn'
    break
  }
}

# apply MVNW_REPOURL and calculate MAVEN_HOME
# maven home pattern: ~/.m2/wrapper/dists/{apache-maven-<version>,maven-mvnd-<version>-<platform>}/<hash>
if ($env:MVNW_REPOURL) {
  $MVNW_REPO_PATTERN = if ($USE_MVND -eq $False) { "/org/apache/maven/" } else { "/maven/mvnd/" }
  $distributionUrl = "$env:MVNW_REPOURL$MVNW_REPO_PATTERN$($distributionUrl -replace "^.*$MVNW_REPO_PATTERN",'')"
}
$distributionUrlName = $distributionUrl -replace '^.*/',''
$distributionUrlNameMain = $distributionUrlName -replace '\.[^.]*$','' -replace '-bin$',''

$MAVEN_M2_PATH = "$HOME/.m2"
if ($env:MAVEN_USER_HOME) {
  $MAVEN_M2_PATH = "$env:MAVEN_USER_HOME"
}

if (-not (Test-Path -Path $MAVEN_M2_PATH)) {
    New-Item -Path $MAVEN_M2_PATH -ItemType Directory | Out-Null
}

$MAVEN_WRAPPER_DISTS = $null
if ((Get-Item $MAVEN_M2_PATH).Target[0] -eq $null) {
  $MAVEN_WRAPPER_DISTS = "$MAVEN_M2_PATH/wrapper/dists"
} else {
  $MAVEN_WRAPPER_DISTS = (Get-Item $MAVEN_M2_PATH).Target[0] + "/wrapper/dists"
}

$MAVEN_HOME_PARENT = "$MAVEN_WRAPPER_DISTS/$distributionUrlNameMain"
$MAVEN_HOME_NAME = ([System.Security.Cryptography.SHA256]::Create().ComputeHash([byte[]][char[]]$distributionUrl) | ForEach-Object {$_.ToString("x2")}) -join ''
$MAVEN_HOME = "$MAVEN_HOME_PARENT/$MAVEN_HOME_NAME"

if (Test-Path -Path "$MAVEN_HOME" -PathType Container) {
  Write-Verbose "found existing MAVEN_HOME at $MAVEN_HOME"
  Write-Output "MVN_CMD=$MAVEN_HOME/bin/$MVN_CMD"
  exit $?
}

if (! $distributionUrlNameMain -or ($distributionUrlName -eq $distributionUrlNameMain)) {
  Write-Error "distributionUrl is not valid, must end with *-bin.zip, but found $distributionUrl"
}

# prepare tmp dir
$TMP_DOWNLOAD_DIR_HOLDER = New-TemporaryFile
$TMP_DOWNLOAD_DIR = New-Item -Itemtype Directory -Path "$TMP_DOWNLOAD_DIR_HOLDER.dir"
$TMP_DOWNLOAD_DIR_HOLDER.Delete() | Out-Null
trap {
  if ($TMP_DOWNLOAD_DIR.Exists) {
    try { Remove-Item $TMP_DOWNLOAD_DIR -Recurse -Force | Out-Null }
    catch { Write-Warning "Cannot remove $TMP_DOWNLOAD_DIR" }
  }
}

New-Item -Itemtype Directory -Path "$MAVEN_HOME_PARENT" -Force | Out-Null

# Download and Install Apache Maven
Write-Verbose "Couldn't find MAVEN_HOME, downloading and installing it ..."
Write-Verbose "Downloading from: $distributionUrl"
Write-Verbose "Downloading to: $TMP_DOWNLOAD_DIR/$distributionUrlName"

$webclient = New-Object System.Net.WebClient
if ($env:MVNW_USERNAME -and $env:MVNW_PASSWORD) {
  $webclient.Credentials = New-Object System.Net.NetworkCredential($env:MVNW_USERNAME, $env:MVNW_PASSWORD)
}
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
$webclient.DownloadFile($distributionUrl, "$TMP_DOWNLOAD_DIR/$distributionUrlName") | Out-Null

# If specified, validate the SHA-256 sum of the Maven distribution zip file
$distributionSha256Sum = (Get-Content -Raw "$scriptDir/.mvn/wrapper/maven-wrapper.properties" | ConvertFrom-StringData).distributionSha256Sum
if ($distributionSha256Sum) {
  if ($USE_MVND) {
    Write-Error "Checksum validation is not supported for maven-mvnd. `nPlease disable validation by removing 'distributionSha256Sum' from your maven-wrapper.properties."
  }
  Import-Module $PSHOME\Modules\Microsoft.PowerShell.Utility -Function Get-FileHash
  if ((Get-FileHash "$TMP_DOWNLOAD_DIR/$distributionUrlName" -Algorithm SHA256).Hash.ToLower() -ne $distributionSha256Sum) {
    Write-Error "Error: Failed to validate Maven distribution SHA-256, your Maven distribution might be compromised. If you updated your Maven version, you need to update the specified distributionSha256Sum property."
  }
}

# unzip and move
Expand-Archive "$TMP_DOWNLOAD_DIR/$distributionUrlName" -DestinationPath "$TMP_DOWNLOAD_DIR" | Out-Null

# Find the actual extracted directory name (handles snapshots where filename != directory name)
$actualDistributionDir = ""

# First try the expected directory name (for regular distributions)
$expectedPath = Join-Path "$TMP_DOWNLOAD_DIR" "$distributionUrlNameMain"
$expectedMvnPath = Join-Path "$expectedPath" "bin/$MVN_CMD"
if ((Test-Path -Path $expectedPath -PathType Container) -and (Test-Path -Path $expectedMvnPath -PathType Leaf)) {
  $actualDistributionDir = $distributionUrlNameMain
}

# If not found, search for any directory with the Maven executable (for snapshots)
if (!$actualDistributionDir) {
  Get-ChildItem -Path "$TMP_DOWNLOAD_DIR" -Directory | ForEach-Object {
    $testPath = Join-Path $_.FullName "bin/$MVN_CMD"
    if (Test-Path -Path $testPath -PathType Leaf) {
      $actualDistributionDir = $_.Name
    }
  }
}

if (!$actualDistributionDir) {
  Write-Error "Could not find Maven distribution directory in extracted archive"
}

Write-Verbose "Found extracted Maven distribution directory: $actualDistributionDir"
Rename-Item -Path "$TMP_DOWNLOAD_DIR/$actualDistributionDir" -NewName $MAVEN_HOME_NAME | Out-Null
try {
  Move-Item -Path "$TMP_DOWNLOAD_DIR/$MAVEN_HOME_NAME" -Destination $MAVEN_HOME_PARENT | Out-Null
} catch {
  if (! (Test-Path -Path "$MAVEN_HOME" -PathType Container)) {
    Write-Error "fail to move MAVEN_HOME"
  }
} finally {
  try { Remove-Item $TMP_DOWNLOAD_DIR -Recurse -Force | Out-Null }
  catch { Write-Warning "Cannot remove $TMP_DOWNLOAD_DIR" }
}

Write-Output "MVN_CMD=$MAVEN_HOME/bin/$MVN_CMD"
````

## File: pom.xml
````xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
	<modelVersion>4.0.0</modelVersion>
	<parent>
		<groupId>org.springframework.boot</groupId>
		<artifactId>spring-boot-starter-parent</artifactId>
		<version>3.5.6</version>
		<relativePath/> <!-- lookup parent from repository -->
	</parent>
	<groupId>com.athletecore</groupId>
	<artifactId>athletecore-api</artifactId>
	<version>0.0.1-SNAPSHOT</version>
	<name>athletecore-api</name>
	<description>API for managing athlete performance and training.</description>
	<url/>
	<licenses>
		<license/>
	</licenses>
	<developers>
		<developer/>
	</developers>
	<scm>
		<connection/>
		<developerConnection/>
		<tag/>
		<url/>
	</scm>
	<properties>
		<java.version>21</java.version>
	</properties>
	<dependencies>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-data-jpa</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-security</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-validation</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-web</artifactId>
		</dependency>
		<dependency>
			<groupId>org.flywaydb</groupId>
			<artifactId>flyway-core</artifactId>
		</dependency>
		<dependency>
			<groupId>org.flywaydb</groupId>
			<artifactId>flyway-database-postgresql</artifactId>
		</dependency>

		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-devtools</artifactId>
			<scope>runtime</scope>
			<optional>true</optional>
		</dependency>
		<dependency>
			<groupId>org.postgresql</groupId>
			<artifactId>postgresql</artifactId>
			<scope>runtime</scope>
		</dependency>
		<dependency>
			<groupId>org.projectlombok</groupId>
			<artifactId>lombok</artifactId>
			<optional>true</optional>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-test</artifactId>
			<scope>test</scope>
		</dependency>
		<dependency>
			<groupId>org.springframework.security</groupId>
			<artifactId>spring-security-test</artifactId>
			<scope>test</scope>
		</dependency>
	</dependencies>

	<build>
		<plugins>
			<plugin>
				<groupId>org.apache.maven.plugins</groupId>
				<artifactId>maven-compiler-plugin</artifactId>
				<configuration>
					<annotationProcessorPaths>
						<path>
							<groupId>org.projectlombok</groupId>
							<artifactId>lombok</artifactId>
						</path>
					</annotationProcessorPaths>
				</configuration>
			</plugin>
			<plugin>
				<groupId>org.springframework.boot</groupId>
				<artifactId>spring-boot-maven-plugin</artifactId>
				<configuration>
					<excludes>
						<exclude>
							<groupId>org.projectlombok</groupId>
							<artifactId>lombok</artifactId>
						</exclude>
					</excludes>
				</configuration>
			</plugin>
		</plugins>
	</build>

</project>
````

## File: src/main/java/com/athletecore/api/config/SecurityConfig.java
````java
package com.athletecore.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                    // La regla específica y correcta que queríamos
                    .requestMatchers("/api/v1/users/**").permitAll() 
                    .anyRequest().authenticated());
            
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
````

## File: src/main/java/com/athletecore/api/domain/BaseEntity.java
````java
package com.athletecore.api.domain;

import java.time.Instant;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@SQLDelete(sql = "UPDATE #{#entityName} SET deleted_at = now(), updated_at = now() WHERE id=?")
@Where(clause = "deleted_at IS NULL")
@Getter
@Setter
public class BaseEntity {

    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public boolean isDeleted() {
        return deletedAt != null;
    }

    // Dejaremos estos comentados por ahora hasta que configuremos Spring Security completamente.
    // @CreatedBy
    // @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "created_by_id", updatable = false)
    // private User createdBy;

    // @LastModifiedBy
    // @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "updated_by_id")
    // private User updatedBy;
    
}
````

## File: src/main/java/com/athletecore/api/domain/Role.java
````java
package com.athletecore.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String name;

    
}
````

## File: src/main/java/com/athletecore/api/user/RoleRepository.java
````java
package com.athletecore.api.user;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.athletecore.api.domain.Role;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    
    Optional<Role> findByName(String id);
}
````

## File: src/main/java/com/athletecore/api/user/UserController.java
````java
package com.athletecore.api.user;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.athletecore.api.domain.User;
import com.athletecore.api.user.dto.CreateUserRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/users")
@Validated
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }

    @PostMapping
    public ResponseEntity<?> createUser(@Valid @RequestBody CreateUserRequest createUserRequest) {
        User createdUser = userService.createUser(createUserRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
    }
    
}
````

## File: src/main/java/com/athletecore/api/user/UserRepository.java
````java
package com.athletecore.api.user;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.athletecore.api.domain.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    
}
````

## File: src/main/java/com/athletecore/api/user/UserService.java
````java
package com.athletecore.api.user;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.athletecore.api.common.exception.DuplicateResourceException;
import com.athletecore.api.common.exception.ResourceNotFoundException;
import com.athletecore.api.domain.Role;
import com.athletecore.api.domain.User;
import com.athletecore.api.user.dto.CreateUserRequest;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User createUser(CreateUserRequest createUserRequest) {
        Optional<User> existingUser = userRepository.findByUsername(createUserRequest.getUsername());
        if (existingUser.isPresent()) {
            throw new DuplicateResourceException("User", "username");
        }

        Optional<User> existingEmail = userRepository.findByEmail(createUserRequest.getEmail());
        if (existingEmail.isPresent()) {
            throw new DuplicateResourceException("User", "email");
        }

        User newUser = new User();
        newUser.setUsername(createUserRequest.getUsername());
        newUser.setEmail(createUserRequest.getEmail());

        String encryptedPassword = passwordEncoder.encode(createUserRequest.getPassword());
        newUser.setPassword(encryptedPassword);
        newUser.setEnabled(true);

        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new IllegalStateException("Default role ROLE_USER not found"));

        newUser.setRoles(Collections.singleton(userRole));

        return userRepository.save(newUser);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}
````

## File: mvnw
````
#!/bin/sh
# ----------------------------------------------------------------------------
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
# ----------------------------------------------------------------------------

# ----------------------------------------------------------------------------
# Apache Maven Wrapper startup batch script, version 3.3.4
#
# Optional ENV vars
# -----------------
#   JAVA_HOME - location of a JDK home dir, required when download maven via java source
#   MVNW_REPOURL - repo url base for downloading maven distribution
#   MVNW_USERNAME/MVNW_PASSWORD - user and password for downloading maven
#   MVNW_VERBOSE - true: enable verbose log; debug: trace the mvnw script; others: silence the output
# ----------------------------------------------------------------------------

set -euf
[ "${MVNW_VERBOSE-}" != debug ] || set -x

# OS specific support.
native_path() { printf %s\\n "$1"; }
case "$(uname)" in
CYGWIN* | MINGW*)
  [ -z "${JAVA_HOME-}" ] || JAVA_HOME="$(cygpath --unix "$JAVA_HOME")"
  native_path() { cygpath --path --windows "$1"; }
  ;;
esac

# set JAVACMD and JAVACCMD
set_java_home() {
  # For Cygwin and MinGW, ensure paths are in Unix format before anything is touched
  if [ -n "${JAVA_HOME-}" ]; then
    if [ -x "$JAVA_HOME/jre/sh/java" ]; then
      # IBM's JDK on AIX uses strange locations for the executables
      JAVACMD="$JAVA_HOME/jre/sh/java"
      JAVACCMD="$JAVA_HOME/jre/sh/javac"
    else
      JAVACMD="$JAVA_HOME/bin/java"
      JAVACCMD="$JAVA_HOME/bin/javac"

      if [ ! -x "$JAVACMD" ] || [ ! -x "$JAVACCMD" ]; then
        echo "The JAVA_HOME environment variable is not defined correctly, so mvnw cannot run." >&2
        echo "JAVA_HOME is set to \"$JAVA_HOME\", but \"\$JAVA_HOME/bin/java\" or \"\$JAVA_HOME/bin/javac\" does not exist." >&2
        return 1
      fi
    fi
  else
    JAVACMD="$(
      'set' +e
      'unset' -f command 2>/dev/null
      'command' -v java
    )" || :
    JAVACCMD="$(
      'set' +e
      'unset' -f command 2>/dev/null
      'command' -v javac
    )" || :

    if [ ! -x "${JAVACMD-}" ] || [ ! -x "${JAVACCMD-}" ]; then
      echo "The java/javac command does not exist in PATH nor is JAVA_HOME set, so mvnw cannot run." >&2
      return 1
    fi
  fi
}

# hash string like Java String::hashCode
hash_string() {
  str="${1:-}" h=0
  while [ -n "$str" ]; do
    char="${str%"${str#?}"}"
    h=$(((h * 31 + $(LC_CTYPE=C printf %d "'$char")) % 4294967296))
    str="${str#?}"
  done
  printf %x\\n $h
}

verbose() { :; }
[ "${MVNW_VERBOSE-}" != true ] || verbose() { printf %s\\n "${1-}"; }

die() {
  printf %s\\n "$1" >&2
  exit 1
}

trim() {
  # MWRAPPER-139:
  #   Trims trailing and leading whitespace, carriage returns, tabs, and linefeeds.
  #   Needed for removing poorly interpreted newline sequences when running in more
  #   exotic environments such as mingw bash on Windows.
  printf "%s" "${1}" | tr -d '[:space:]'
}

scriptDir="$(dirname "$0")"
scriptName="$(basename "$0")"

# parse distributionUrl and optional distributionSha256Sum, requires .mvn/wrapper/maven-wrapper.properties
while IFS="=" read -r key value; do
  case "${key-}" in
  distributionUrl) distributionUrl=$(trim "${value-}") ;;
  distributionSha256Sum) distributionSha256Sum=$(trim "${value-}") ;;
  esac
done <"$scriptDir/.mvn/wrapper/maven-wrapper.properties"
[ -n "${distributionUrl-}" ] || die "cannot read distributionUrl property in $scriptDir/.mvn/wrapper/maven-wrapper.properties"

case "${distributionUrl##*/}" in
maven-mvnd-*bin.*)
  MVN_CMD=mvnd.sh _MVNW_REPO_PATTERN=/maven/mvnd/
  case "${PROCESSOR_ARCHITECTURE-}${PROCESSOR_ARCHITEW6432-}:$(uname -a)" in
  *AMD64:CYGWIN* | *AMD64:MINGW*) distributionPlatform=windows-amd64 ;;
  :Darwin*x86_64) distributionPlatform=darwin-amd64 ;;
  :Darwin*arm64) distributionPlatform=darwin-aarch64 ;;
  :Linux*x86_64*) distributionPlatform=linux-amd64 ;;
  *)
    echo "Cannot detect native platform for mvnd on $(uname)-$(uname -m), use pure java version" >&2
    distributionPlatform=linux-amd64
    ;;
  esac
  distributionUrl="${distributionUrl%-bin.*}-$distributionPlatform.zip"
  ;;
maven-mvnd-*) MVN_CMD=mvnd.sh _MVNW_REPO_PATTERN=/maven/mvnd/ ;;
*) MVN_CMD="mvn${scriptName#mvnw}" _MVNW_REPO_PATTERN=/org/apache/maven/ ;;
esac

# apply MVNW_REPOURL and calculate MAVEN_HOME
# maven home pattern: ~/.m2/wrapper/dists/{apache-maven-<version>,maven-mvnd-<version>-<platform>}/<hash>
[ -z "${MVNW_REPOURL-}" ] || distributionUrl="$MVNW_REPOURL$_MVNW_REPO_PATTERN${distributionUrl#*"$_MVNW_REPO_PATTERN"}"
distributionUrlName="${distributionUrl##*/}"
distributionUrlNameMain="${distributionUrlName%.*}"
distributionUrlNameMain="${distributionUrlNameMain%-bin}"
MAVEN_USER_HOME="${MAVEN_USER_HOME:-${HOME}/.m2}"
MAVEN_HOME="${MAVEN_USER_HOME}/wrapper/dists/${distributionUrlNameMain-}/$(hash_string "$distributionUrl")"

exec_maven() {
  unset MVNW_VERBOSE MVNW_USERNAME MVNW_PASSWORD MVNW_REPOURL || :
  exec "$MAVEN_HOME/bin/$MVN_CMD" "$@" || die "cannot exec $MAVEN_HOME/bin/$MVN_CMD"
}

if [ -d "$MAVEN_HOME" ]; then
  verbose "found existing MAVEN_HOME at $MAVEN_HOME"
  exec_maven "$@"
fi

case "${distributionUrl-}" in
*?-bin.zip | *?maven-mvnd-?*-?*.zip) ;;
*) die "distributionUrl is not valid, must match *-bin.zip or maven-mvnd-*.zip, but found '${distributionUrl-}'" ;;
esac

# prepare tmp dir
if TMP_DOWNLOAD_DIR="$(mktemp -d)" && [ -d "$TMP_DOWNLOAD_DIR" ]; then
  clean() { rm -rf -- "$TMP_DOWNLOAD_DIR"; }
  trap clean HUP INT TERM EXIT
else
  die "cannot create temp dir"
fi

mkdir -p -- "${MAVEN_HOME%/*}"

# Download and Install Apache Maven
verbose "Couldn't find MAVEN_HOME, downloading and installing it ..."
verbose "Downloading from: $distributionUrl"
verbose "Downloading to: $TMP_DOWNLOAD_DIR/$distributionUrlName"

# select .zip or .tar.gz
if ! command -v unzip >/dev/null; then
  distributionUrl="${distributionUrl%.zip}.tar.gz"
  distributionUrlName="${distributionUrl##*/}"
fi

# verbose opt
__MVNW_QUIET_WGET=--quiet __MVNW_QUIET_CURL=--silent __MVNW_QUIET_UNZIP=-q __MVNW_QUIET_TAR=''
[ "${MVNW_VERBOSE-}" != true ] || __MVNW_QUIET_WGET='' __MVNW_QUIET_CURL='' __MVNW_QUIET_UNZIP='' __MVNW_QUIET_TAR=v

# normalize http auth
case "${MVNW_PASSWORD:+has-password}" in
'') MVNW_USERNAME='' MVNW_PASSWORD='' ;;
has-password) [ -n "${MVNW_USERNAME-}" ] || MVNW_USERNAME='' MVNW_PASSWORD='' ;;
esac

if [ -z "${MVNW_USERNAME-}" ] && command -v wget >/dev/null; then
  verbose "Found wget ... using wget"
  wget ${__MVNW_QUIET_WGET:+"$__MVNW_QUIET_WGET"} "$distributionUrl" -O "$TMP_DOWNLOAD_DIR/$distributionUrlName" || die "wget: Failed to fetch $distributionUrl"
elif [ -z "${MVNW_USERNAME-}" ] && command -v curl >/dev/null; then
  verbose "Found curl ... using curl"
  curl ${__MVNW_QUIET_CURL:+"$__MVNW_QUIET_CURL"} -f -L -o "$TMP_DOWNLOAD_DIR/$distributionUrlName" "$distributionUrl" || die "curl: Failed to fetch $distributionUrl"
elif set_java_home; then
  verbose "Falling back to use Java to download"
  javaSource="$TMP_DOWNLOAD_DIR/Downloader.java"
  targetZip="$TMP_DOWNLOAD_DIR/$distributionUrlName"
  cat >"$javaSource" <<-END
	public class Downloader extends java.net.Authenticator
	{
	  protected java.net.PasswordAuthentication getPasswordAuthentication()
	  {
	    return new java.net.PasswordAuthentication( System.getenv( "MVNW_USERNAME" ), System.getenv( "MVNW_PASSWORD" ).toCharArray() );
	  }
	  public static void main( String[] args ) throws Exception
	  {
	    setDefault( new Downloader() );
	    java.nio.file.Files.copy( java.net.URI.create( args[0] ).toURL().openStream(), java.nio.file.Paths.get( args[1] ).toAbsolutePath().normalize() );
	  }
	}
	END
  # For Cygwin/MinGW, switch paths to Windows format before running javac and java
  verbose " - Compiling Downloader.java ..."
  "$(native_path "$JAVACCMD")" "$(native_path "$javaSource")" || die "Failed to compile Downloader.java"
  verbose " - Running Downloader.java ..."
  "$(native_path "$JAVACMD")" -cp "$(native_path "$TMP_DOWNLOAD_DIR")" Downloader "$distributionUrl" "$(native_path "$targetZip")"
fi

# If specified, validate the SHA-256 sum of the Maven distribution zip file
if [ -n "${distributionSha256Sum-}" ]; then
  distributionSha256Result=false
  if [ "$MVN_CMD" = mvnd.sh ]; then
    echo "Checksum validation is not supported for maven-mvnd." >&2
    echo "Please disable validation by removing 'distributionSha256Sum' from your maven-wrapper.properties." >&2
    exit 1
  elif command -v sha256sum >/dev/null; then
    if echo "$distributionSha256Sum  $TMP_DOWNLOAD_DIR/$distributionUrlName" | sha256sum -c - >/dev/null 2>&1; then
      distributionSha256Result=true
    fi
  elif command -v shasum >/dev/null; then
    if echo "$distributionSha256Sum  $TMP_DOWNLOAD_DIR/$distributionUrlName" | shasum -a 256 -c >/dev/null 2>&1; then
      distributionSha256Result=true
    fi
  else
    echo "Checksum validation was requested but neither 'sha256sum' or 'shasum' are available." >&2
    echo "Please install either command, or disable validation by removing 'distributionSha256Sum' from your maven-wrapper.properties." >&2
    exit 1
  fi
  if [ $distributionSha256Result = false ]; then
    echo "Error: Failed to validate Maven distribution SHA-256, your Maven distribution might be compromised." >&2
    echo "If you updated your Maven version, you need to update the specified distributionSha256Sum property." >&2
    exit 1
  fi
fi

# unzip and move
if command -v unzip >/dev/null; then
  unzip ${__MVNW_QUIET_UNZIP:+"$__MVNW_QUIET_UNZIP"} "$TMP_DOWNLOAD_DIR/$distributionUrlName" -d "$TMP_DOWNLOAD_DIR" || die "failed to unzip"
else
  tar xzf${__MVNW_QUIET_TAR:+"$__MVNW_QUIET_TAR"} "$TMP_DOWNLOAD_DIR/$distributionUrlName" -C "$TMP_DOWNLOAD_DIR" || die "failed to untar"
fi

# Find the actual extracted directory name (handles snapshots where filename != directory name)
actualDistributionDir=""

# First try the expected directory name (for regular distributions)
if [ -d "$TMP_DOWNLOAD_DIR/$distributionUrlNameMain" ]; then
  if [ -f "$TMP_DOWNLOAD_DIR/$distributionUrlNameMain/bin/$MVN_CMD" ]; then
    actualDistributionDir="$distributionUrlNameMain"
  fi
fi

# If not found, search for any directory with the Maven executable (for snapshots)
if [ -z "$actualDistributionDir" ]; then
  # enable globbing to iterate over items
  set +f
  for dir in "$TMP_DOWNLOAD_DIR"/*; do
    if [ -d "$dir" ]; then
      if [ -f "$dir/bin/$MVN_CMD" ]; then
        actualDistributionDir="$(basename "$dir")"
        break
      fi
    fi
  done
  set -f
fi

if [ -z "$actualDistributionDir" ]; then
  verbose "Contents of $TMP_DOWNLOAD_DIR:"
  verbose "$(ls -la "$TMP_DOWNLOAD_DIR")"
  die "Could not find Maven distribution directory in extracted archive"
fi

verbose "Found extracted Maven distribution directory: $actualDistributionDir"
printf %s\\n "$distributionUrl" >"$TMP_DOWNLOAD_DIR/$actualDistributionDir/mvnw.url"
mv -- "$TMP_DOWNLOAD_DIR/$actualDistributionDir" "$MAVEN_HOME" || [ -d "$MAVEN_HOME" ] || die "fail to move MAVEN_HOME"

clean || :
exec_maven "$@"
````

## File: src/main/java/com/athletecore/api/domain/User.java
````java
package com.athletecore.api.domain;

import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;

    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.PERSIST)
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    
    private Set<Role> roles;

    public String getFullName() {
        return firstName + " " + lastName;
    }
    
}
````

## File: src/main/resources/application.properties
````
spring.application.name=athletecore-api

# =======================================
# DATASOURCE CONFIGURATION
# =======================================
# URL de conexión a PostgreSQL usando variable de entorno con fallback para desarrollo
spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:5432/athletecore}
spring.datasource.username=${DB_USER:postgres}
spring.datasource.password=${DB_PASSWORD:postgres}
spring.datasource.driver-class-name=org.postgresql.Driver

# Configuración de conexión pool (HikariCP por defecto en Spring Boot 3.x)
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000

# =======================================
# FLYWAY CONFIGURATION
# =======================================
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=true
spring.flyway.locations=classpath:db/migration
spring.flyway.validate-on-migrate=true
spring.flyway.out-of-order=true

# =======================================
# JPA/HIBERNATE CONFIGURATION
# =======================================
# validate: Validar que el esquema coincide con las entidades (no modificar en prod)
# Nunca usar create o create-drop en producción
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect

# =======================================
# LOGGING CONFIGURATION
# =======================================
logging.level.root=INFO
logging.level.com.athletecore=DEBUG
logging.level.org.springframework.security=INFO
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE

# =======================================
# SERVER CONFIGURATION
# =======================================
server.port=${SERVER_PORT:8080}

# =======================================
# EXCEPTION HANDLING CONFIGURATION
# =======================================
# Lanar excepciones cuando no se encuentra un handler para manejar errores de 404
spring.mvc.throw-exception-if-no-handler-found=true
spring.web.resources.add-mappings=false

# =======================================
# SECURITY CONFIGURATION
# =======================================
# JWT secret (debe configurarse en entorno)
spring.security.jwt.secret=${JWT_SECRET:your-secret-key-change-in-production}
# Token expiration time in milliseconds (24 horas)
spring.security.jwt.expiration=${JWT_EXPIRATION:86400000}
````
