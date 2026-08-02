## Context

### Background
El proyecto AthleteCore API tiene una base limpia (Fase 0) con:
- User y Role implementados con seguridad JWT
- Flyway configurado para migraciones
- BaseEntity con soft delete y auditoría
- Core principles documentados

### Current State
- Base de datos con tablas: roles, users, user_roles
- Endpoints de usuario básicos (registro, list)
- Sin módulos de negocio implementados

### Constraints
- Todo debe extender BaseEntity (soft delete + auditoría)
- Usar DTOs en respuestas (no exponer entidades JPA)
- Jakarta Validation para validaciones
- JWT authentication para endpoints sensibles
- PostgreSQL con Flyway para esquema

---

## Goals / Non-Goals

**Goals:**
- Implementar módulo completo de deportistas (CRUD)
- Perfil antropométrico con cálculo automático de IMC
- Gestión de deportes/disciplinas (modelo genérico)
- Foto de perfil (URL, no binario)
- Soft delete para todos los recursos
- Validación completa de datos
- Tests unitarios con >80% cobertura

**Non-Goals:**
- Implementación de entrenamiento (siguiente módulo)
- Chequeos mensuales (siguiente módulo)
- Reportes (siguiente módulo)
- Autenticación/autorización adicional (ya existe)
- Frontend (solo API)
- Procesamiento asíncrono
- Cacheo con Redis

---

## Decisions

### 1. Arquitectura en Capas

**Decisión:** Controller → Service → Repository con DTOs

**Racional:**
- Sigue el principio de principios.md: "entities-first" + "No exponer entidades"
- Separación clara de responsabilidades
- Facilita testing
- Consistente con User/Module existente

**Alternativa considerada:** DDD puro con aggregates
**Por qué no:** Over-engineering para este nivel, DDD completo viene en fases posteriores

### 2. Soft Delete con BaseEntity

**Decisión:** Heredar BaseEntity y usar @SQLRestriction

**Racional:**
- Ya implementado y funcionando en User/Role
- @SQLRestriction ("deleted_at IS NULL") filtra automáticamente
- @SQLDelete hace "deleted_at = now()"

**Código:**
```java
public class Athlete extends BaseEntity { }
```

### 3. Perfil Antropométrico - Relación 1:1

**Decisión:** Athlete tiene un AthleteProfile (relación @OneToOne)

**Racional:**
- Cada atleta tiene exactamente un perfil antropométrico
- Facilita consultas (no necesita JOIN en muchos casos)
- El perfil es información detallada que complementa al atleta

**Alternativa:** Campo embedded en Athlete
**Por qué no:** Mejor separación de responsabilidades, perfil puede crecer (futuros campos)

### 4. Deporte - Disciplina - Jerarquía

**Decisión:** Modelo genérico con relación Muchos-A-Muchos para deportes

**Racional:**
```
Athlete (id)
  ↕ Many-to-Many
Sport (id, name, description)
  ↓ One-to-Many
Discipline (id, sport_id, name, description)
```

- Un atleta puede tener múltiples deportes
- Un deporte puede tener múltiples disciplinas
- Modelo extensible para nuevos deportes

**Alternativa:** Deporte como campo String en Athlete
**Por qué no:** No permite gestión de deportes, no es escalable

### 5. Foto de Perfil - URL Referencial

**Decisión:** Solo almacenar URL, no binary data

**Racional:**
- Menor uso de almacenamiento
- Imágenes pueden servir desde CDN/S3
- Fácil migración a storage externo en el futuro
- Patrón común en sistemas modernos

**Código:**
```java
@Column(name = "photo_url", nullable = true)
private String photoUrl;
```

**Alternativa:** Base64 en base de datos
**Por qué no:** Sobrecarga en transferencias, almacenamiento ineficiente

### 6. Cálculo de IMC - Método Puro

**Decisión:** Método de instancia `calculateBMI()` en AthleteProfile

**Racional:**
```java
public double calculateBMI() {
    return weightKgs / (heightMeters * heightMeters);
}
```

- Determinista, testeable
- Sin dependencias externas
- Fácil de modificar fórmula si cambia

**Alternativa:** Trigger SQL u otros
**Por qué no:** Lógica de negocio en capa de aplicación (mejor práctica)

### 7. DTOs Separados para Request y Response

**Decisión:** Crear DTOs específicos para cada operación

**Racional:**
```java
// Request
CreateAthleteRequest { username, email, firstName, lastName... }

// Response
AthleteResponse { id, username, email, fullName, photoUrl... }
```

- Control explícito de qué campos se exponen
- Separación de request/response
- Fácil evolución de API

**Alternativa:** Mapear entidades directamente
**Por qué no:** Violación de principios (exposición de entidades JPA)

### 8. Endpoints RESTful

**Decisión:** REST estándar con HTTP semantics

**Racional:**
```
POST   /api/v1/athletes              → Crear
GET    /api/v1/athletes              → Listar (paginado)
GET    /api/v1/athletes/{id}         → Obtener
PUT    /api/v1/athletes/{id}         → Reemplazar completa
PATCH  /api/v1/athletes/{id}         → Actualizar parcial
DELETE /api/v1/athletes/{id}         → Eliminar (soft)
POST   /api/v1/athletes/{id}/profile → Actualizar perfil
POST   /api/v1/athletes/{id}/sports  → Asignar deporte
```

- Semántica HTTP clara
- Fácil de descubrir
- Estandarizado

### 9. Validaciones - Jakarta Validation + Service Layer

**Decisión:** Validaciones en DTOs con @NotBlank, @Email, @Size

**Racional:**
```java
@NotBlank(message = "Username is required")
@Size(min = 3, max = 255)
private String username;
```

- Validaciones tempranas (antes de llegar a repositorio)
- Mensajes de error amigables
- Validaciones en entity también (defensa en profundidad)

**Alternativa:** Validaciones solo en entity
**Por qué no:** Mejor separar request validation de entity constraints

### 10. Índices de Base de Datos

**Decisión:** Índices en campos de búsqueda frecuente

**Código SQL:**
```sql
CREATE INDEX idx_athletes_username ON athletes(username) WHERE deleted_at IS NULL;
CREATE INDEX idx_athletes_email ON athletes(email) WHERE deleted_at IS NULL;
CREATE INDEX idx_athletes_fullname ON athletes(last_name, first_name) WHERE deleted_at IS NULL;
```

- Búsqueda rápida por username/email
- Búsqueda por nombre completo
- Filter con deleted_at para soft delete

**Alternativa:** No índices / índices automáticos
**Por qué no:** Performance en búsquedas es crítico para UX

---

## Risks / Trade-offs

| Risk | Impact | Mitigation |
|------|--------|------------|
| IMC cálculo puede dar valores raros con datos incorrectos | Medición errónea | Validar rangos (weight 1-500kg, height 0.1-3m) |
| URL foto puede quebrar o tener CORS | Imagen no carga | No validar URL en backend, cliente maneja |
| Modelo genérico de deportes puede necesitar evolución | Rediseño futuro | Diseño flexible, agregar campos opcionales si es necesario |
| Soft delete en muchas consultas | Performance en joins grandes | Índices con WHERE deleted_at IS NULL |
| DTOs duplicados (request/response) | Mantenimiento | Pattern común en Spring Boot, usar builder pattern |

---

## Migration Plan

### Pre-Deployment
1. **Backup:** Backup de DB antes de migración
2. **Pruebas:** Tests en desarrollo local

### Deploy Steps
1. **Ejecutar migración:** `mvn flyway:migrate`
2. **Verificar tabla:** `SELECT * FROM athletes LIMIT 1;`
3. **Deploy API:** `java -jar target/*.jar`
4. **Health check:** `curl http://localhost:8080/actuator/health`

### Rollback Plan
1. **Migración inversa:** Crear V-1__rollback.sql si es necesario
2. **Revertir API:** Deploy versión anterior
3. **DB restore:** Si hay problemas graves de datos

---

## Open Questions

1. **Paginación:** ¿Default page size? ¿Qué parámetros para filtrado en lista?
2. **Sorteo por defecto:** ¿Ordenar por nombre, creado_at?
3. **Search full-text:** ¿Buscar por nombre completo con full-text search PostgreSQL?
4. **Audit log:** ¿Registrar cambios de perfil antropométrico separatamente?
5. **Import/Export:** ¿Necesitar bulk import de atletas desde CSV en inicio?

**Decisiones tomadas:**
- Paginación: Spring Data Pageable (page, size, sort)
- Sort default: by `created_at DESC`
- Search: username/email exact match, no full-text por ahora
