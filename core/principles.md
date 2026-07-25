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