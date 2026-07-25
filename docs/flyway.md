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

- **Soft Delete**: Todas las tablas incluyen `deleted_at` para soporte de eliminación lógica
- **Auditoría**: `created_at` y `updated_at` en todas las tablas
- **NOTA**: La actualización automática de `updated_at` es manejada por Hibernate `@LastModifiedDate`
- **Índices**: Índices optimizados para consultas frecuentes
- **Comentarios**: Comentarios SQL para documentación del esquema

### Datos de referencia

**IMPORTANTE**: Esta migración V1 solo crea la **estructura de tablas**, NO incluye datos iniciales.
Los seeds (roles, usuarios) deben gestionarse mediante:
- Migración separada `V2__seed_data.sql` (si requiere datos iniciales)
- O `CommandLineRunner` / Data Seeder en Spring (recomendado)

Para crear roles de referencia en producción:
```sql
-- Crear en V2__seed_data.sql
INSERT INTO roles (name, created_at, updated_at) VALUES
    ('ROLE_ADMIN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('ROLE_USER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('ROLE_TRAINER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('ROLE_COACH', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
```

**NUNCA** incluir contraseñas hardcodeadas en migraciones. Usar variables de entorno o seeders de Spring.

## Configuración

### application.properties (Todos los entornos)

```properties
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=false    # Usar baseline solo en desarrollo local
spring.flyway.locations=classpath:db/migration
spring.flyway.validate-on-migrate=true
spring.flyway.out-of-order=true            # Permite agregar migraciones fuera de orden en desarrollo
```

### application-local.properties (Desarrollo)

```properties
spring.flyway.baseline-on-migrate=true     # Crear baseline solo la primera vez
```

### variables de entorno recomendadas

| Variable | Descripción | Default |
|----------|-------------|---------|
| `DB_URL` | URL PostgreSQL | `jdbc:postgresql://localhost:5432/athletecore` |
| `DB_USER` | Usuario DB | `postgres` |
| `DB_PASSWORD` | Contraseña DB | `postgres` |
| `JWT_SECRET` | Secret para JWT | **OBLIGATORIO en producción** |

## Uso

### Ejecutar migraciones

**Con Spring Boot (recomendado)**:
```bash
# Iniciar la aplicación ejecutará Flyway automáticamente
./mvnw spring-boot:run
# o
java -jar target/athletecore-api-0.0.1-SNAPSHOT.jar
```

**Con Maven plugin (desarrollo)**:
```bash
# Ejecutar migraciones
mvn flyway:migrate

# Ver estado de migraciones
mvn flyway:info

# Validar scripts
mvn flyway:validate
```

**Con script de utilidad (opcional)**:
```bash
./scripts/flyway.sh migrate      # Ejecutar migraciones
./scripts/flyway.sh validate     # Validar scripts
./scripts/flyway.sh info         # Ver estado
./scripts/flyway.sh baseline 1   # Crear baseline inicial
```

### Validar migraciones antes de producción

Antes de desplegar en producción:
```bash
# Validar que los scripts SQL son correctos
mvn flyway:validate

# O usar el script
./scripts/flyway.sh validate
```

### Baseline inicial (desarrollo)

Solo necesario la primera vez si la BD está vacía:
```bash
# Con Maven
mvn flyway:baseline -Dflyway.baselineVersion=1

# Con script
./scripts/flyway.sh baseline 1
```

**NOTA**: En producción, nunca usar baseline. Las migraciones deben ejecutarse en orden desde V1.

## Variables de entorno

| Variable | Descripción | Default |
|----------|-------------|---------|
| `DB_URL` | URL de conexión PostgreSQL | `jdbc:postgresql://localhost:5432/athletecore` |
| `DB_USER` | Usuario de PostgreSQL | `postgres` |
| `DB_PASSWORD` | Contraseña de PostgreSQL | `postgres` |

## Consideraciones importantes

### Baseline y Entornos

**Desarrollo Local:**
```properties
# application-local.properties
spring.flyway.baseline-on-migrate=true  # Solo primera vez
spring.flyway.validate-on-migrate=true
spring.flyway.out-of-order=true         # Permite migraciones nuevas sin orden
```

**Producción:**
```properties
# application.properties (sin overrides locales)
spring.flyway.baseline-on-migrate=false  # Nunca crear baseline en prod
spring.flyway.validate-on-migrate=true   # Validar antes de migrar
spring.flyway.out-of-order=false         # Estricto orden de migraciones en prod
```

### Migraciones de Esquema vs Datos

**Regla:** Flyway debe usarse SOLO para crear/actualizar esquema de tablas.

- ✅ `V1__initial_schema.sql` - Solo CREATE TABLE, ALTER TABLE
- ❌ **NO** INSERTs de datos aquí
- ✅ Crear migraciones separadas `V2__seed_data.sql` si se requieren datos iniciales
- ✅ O mejor: Usar `CommandLineRunner` de Spring para seeders

### Seguridad

**Credenciales:**
```properties
# application.properties
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USER}
spring.datasource.password=${DB_PASSWORD}

# JWT
spring.security.jwt.secret=${JWT_SECRET}  # Sin fallback hardcoded!
```

**NUNCA:**
- ❌ Hardcodear passwords
- ❌ Usar fallbacks inseguros (`your-secret-key-change-in-production`)
- ❌ Incluir credenciales en migraciones o seeds

### Verificación de Esquema

Antes de producción, verificar que el esquema de BD coincide con entidades:
```bash
mvn spring-boot:run  # Inicia con validate-on-migrate=true
```

Si hay discrepancias, verificar:
1. Migraciones Flyway están completas
2. Entidades Java coinciden con tablas
3. `ddl-auto=validate` en producción

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
