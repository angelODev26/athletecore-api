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
