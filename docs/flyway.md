# Configuración Flyway - AthleteCore API

## Resumen

Este documento describe la configuración de Flyway para la gestión de migraciones de base de datos en AthleteCore API.

## Estructura de archivos

```
src/main/resources/db/migration/
├── V1__initial_schema.sql              # roles, users, user_roles
├── V2__athletes_schema.sql             # deportistas + seed de roles base y deportes
├── V3__training_schema.sql             # entrenamientos + ROLE_COACH
├── V4__checkup_schema.sql              # chequeos
├── V5__reports_schema.sql              # reportes
└── V6__seed_admin_usuario_inicial.sql  # admin inicial (placeholders de entorno)
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

---

## Migración V6: seed del administrador inicial

**Archivo**: `V6__seed_admin_usuario_inicial.sql`

Con el registro público cerrado (`POST /api/v1/users` requiere `ROLE_ADMIN`), un
entorno nuevo necesita un ADMIN para poder operar. La migración lo siembra a
partir de **placeholders de Flyway** alimentados por variables de entorno:

| Placeholder | Variable de entorno | Default |
|---|---|---|
| `spring.flyway.placeholders.admin_username` | `ADMIN_USERNAME` | `admin` |
| `spring.flyway.placeholders.admin_email` | `ADMIN_EMAIL` | `admin@athletecore.local` |
| `spring.flyway.placeholders.admin_password_hash` | `ADMIN_PASSWORD_HASH` | **sin default** (fail-fast) |

### Generar el hash

El password se guarda como hash **BCrypt**, nunca en claro:

```bash
htpasswd -bnBC 12 "" 'mi-password' | tr -d ':\n'
# $2y$12$...
```

En producción, definir `ADMIN_PASSWORD_HASH` con ese valor. En local, el ejemplo
versionado `src/main/resources/application-local.properties.example` trae el hash
de desarrollo documentado (`admin` / `admin123`); cópialo o mézclalo en tu
`application-local.properties` (gitignored) para no depender de variables
externas.

### Comportamiento

- **Fail-fast**: si falta `ADMIN_PASSWORD_HASH`, Spring no arranca (placeholder no
  resuelto). Si está definido pero vacío o no es un BCrypt válido (60 chars,
  `$2a/$2b/$2y`), la migración aborta con un error accionable. Deliberado: evita
  desplegar sin ninguna vía de administración o con una credencial inválida.
- **No intrusiva**: si el username ya existe, solo se añade `ROLE_ADMIN` cuando la
  cuenta coincide con el hash configurado, está `enabled` y sin soft delete. Si
  no, la migración aborta: **nunca promueve** una cuenta preexistente (por
  ejemplo, un `admin` registrado por el antiguo registro público) ni sobrescribe
  su password, estado o roles.
- **Email en uso**: si `ADMIN_EMAIL` pertenece a otro usuario, aborta con un
  mensaje que pide configurar otro email.
- **Vínculo de rol**: si la cuenta sembrada ya existe pero le falta `ROLE_ADMIN`,
  solo inserta ese vínculo.
- **Rotación de contraseña**: Flyway no re-ejecuta `V6`; cambiar el hash en el
  entorno no cambia la contraseña ya sembrada. La rotación es un `UPDATE` manual
  documentado (o el futuro endpoint de password, deuda T8).
- **Username soft-deleted**: la migración aborta con un mensaje accionable;
  restaurar la cuenta o elegir otro `ADMIN_USERNAME`.

### Verificación post-deploy

```bash
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"<la del hash>"}'
# 200 con user.roles = ["ROLE_ADMIN"]
```

---

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
| `ADMIN_USERNAME` | Username del admin inicial (V6) | `admin` |
| `ADMIN_EMAIL` | Email del admin inicial (V6) | `admin@athletecore.local` |
| `ADMIN_PASSWORD_HASH` | Hash BCrypt del admin inicial (V6) | **OBLIGATORIO salvo perfil local** |

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
| `ADMIN_USERNAME` | Username del admin inicial (V6) | `admin` |
| `ADMIN_EMAIL` | Email del admin inicial (V6) | `admin@athletecore.local` |
| `ADMIN_PASSWORD_HASH` | Hash BCrypt del admin inicial (V6) | **obligatorio** (local tiene default de desarrollo) |

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

**Regla:** las migraciones pueden incluir datos **de referencia o de arranque**
(catálogos y bootstrap), siempre que sean idempotentes y no destructivas. Los
datos de negocio volátiles no van aquí.

- ✅ `V1__initial_schema.sql` — solo esquema.
- ✅ `V2`/`V3` — esquema + seed de catálogos estables (`ROLE_*`, deportes).
- ✅ `V6` — bootstrap del admin inicial desde placeholders de entorno, idempotente
  (`ON CONFLICT ... DO NOTHING`).
- ❌ Datos de negocio, fixtures de demo o cualquier seed que pise filas existentes.
- ❌ Contraseñas en claro: siempre hash BCrypt vía `ADMIN_PASSWORD_HASH`.

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
- ❌ Hardcodear passwords (en migraciones, seeds, código o docs)
- ❌ Usar fallbacks inseguros (`your-secret-key-change-in-production`)
- ❌ Incluir credenciales **en claro** en migraciones o seeds
- ✅ Sembrar **hashes** BCrypt vía variables de entorno (`ADMIN_PASSWORD_HASH`), como
  hace `V6`

### Verificación de Esquema

Antes de producción, verificar que el esquema de BD coincide con entidades:
```bash
mvn spring-boot:run  # Inicia con validate-on-migrate=true
```

Si hay discrepancias, verificar:
1. Migraciones Flyway están completas
2. Entidades Java coinciden con tablas
3. `ddl-auto=validate` en producción

## Migraciones aplicadas

El roadmap de esquema ya está completo hasta `V6`:

- **V1** — roles, users, user_roles
- **V2** — deportistas (+ catálogo de roles y deportes)
- **V3** — entrenamientos (+ `ROLE_COACH`)
- **V4** — chequeos mensuales
- **V5** — reportes
- **V6** — bootstrap del admin inicial

Las próximas migraciones (`V7+`) se crean con el mismo patrón: archivo versionado,
idempotente y con `created_at`/`updated_at`/`deleted_at` cuando aplique.

## Referencias

- [Flyway Documentation](https://flywaydb.org/documentation/)
- [Spring Boot Flyway](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#data.sql.datasource.flyway)
- [PostgreSQL Best Practices](https://www.postgresql.org/docs/current/tutorial.html)
