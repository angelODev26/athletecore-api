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
