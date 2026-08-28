## Why

Los módulos Athlete (v0.2.0) y Training (v0.3.0) ya cubren ficha deportiva y operación diaria, pero AthleteCore no muestra el cumplimiento mensual real: registrar tiempos de prueba por estilo+distancia, comparar contra la tabla nacional de referencia y proyectar medallería. El módulo de Chequeos es el siguiente paso (v0.4.0) porque transforma los datos operativos en una medida objetiva de rendimiento y priorización de deportistas de cara a competencias.

## What Changes

### Nuevas Funcionalidades:
- Chequeo mensual por deportista: agrupa tiempos de prueba de un mes (estilo + distancia)
- Registro de tiempos de prueba por estilo y distancia, almacenados en segundos decimales
- Presentación de tiempos en formato `mm:ss.ms` (responsabilidad de DTO/Formatter, no de la entidad)
- Tabla nacional de tiempos de referencia (1°, 2° y 3° puesto, por estilo + distancia + categoría)
- Comparación del tiempo registrado contra la tabla nacional (diferencia absoluta y relativa)
- Proyección de medallería: diferencia de tiempo respecto a 1°, 2° y 3° puesto
- Clasificación relativa determinística: **Por encima del podio**, **Cercano a medallería**, **Fuera de rango**
- Gestión administrativa (CRUD) de la tabla nacional de referencia, restringida a roles admin
- Soft delete para todas las entidades del módulo (extienden `BaseEntity`)

### Modificaciones:
- Migración Flyway `V4__checkup_schema.sql` (nueva)
- Entidades: `Checkup`, `CheckupTime`, `NationalReferenceTime`, `MedalProjection` (todas extienden `BaseEntity` con `@SQLDelete`/`@SQLRestriction` declarados en cada clase concreta)
- Servicios: `CheckupService`, `TimeComparisonService`, `MedalProjectionService`, `ClassificationService`
- Controller: `CheckupController` (CRUD chequeos + tabla nacional admin)
- DTOs de request/response (records), sin exponer entidades JPA
- Seguridad: endpoints sensibles con `@PreAuthorize("hasRole('ADMIN')")` para la tabla nacional; consulta de chequeos y proyecciones disponible para roles operativos

### BREAKING:
- No hay breaking changes en la API existente (solo se agrega funcionalidad nueva)

## Capabilities

### New Capabilities
- **checkup-registration**: Registro de chequeos mensuales y tiempos de prueba por estilo y distancia (almacenamiento en segundos, formato `mm:ss.ms` en presentación)
- **national-reference-times**: Gestión administrativa (CRUD) de la tabla nacional de tiempos de referencia por estilo + distancia + categoría, restringida a ADMIN
- **time-comparison**: Comparación de tiempos registrados contra la tabla nacional (diferencia absoluta y relativa respecto a 1°, 2° y 3° puesto)
- **medal-projection**: Proyección de medallería por deportista con clasificación relativa determinística (**Por encima del podio** / **Cercano a medallería** / **Fuera de rango**)

### Modified Capabilities
- Ninguno (módulo nuevo sin afectar requirements existentes)

## Impact

### Archivos Nuevos:
- **Migración**: `src/main/resources/db/migration/V4__checkup_schema.sql`
- **Entidades**:
  - `src/main/java/com/athletecore/api/checkup/Checkup.java`
  - `src/main/java/com/athletecore/api/checkup/CheckupTime.java`
  - `src/main/java/com/athletecore/api/checkup/NationalReferenceTime.java`
  - `src/main/java/com/athletecore/api/checkup/MedalProjection.java`
- **Repositorios**: `CheckupRepository`, `CheckupTimeRepository`, `NationalReferenceTimeRepository`, `MedalProjectionRepository`
- **Servicios**:
  - `src/main/java/com/athletecore/api/checkup/CheckupService.java` (registro de chequeos)
  - `src/main/java/com/athletecore/api/checkup/TimeComparisonService.java` (comparación con referencias nacionales)
  - `src/main/java/com/athletecore/api/checkup/MedalProjectionService.java` (proyección de medallería)
  - `src/main/java/com/athletecore/api/checkup/ClassificationService.java` (clasificación relativa por podio)
- **Controller**: `src/main/java/com/athletecore/api/checkup/CheckupController.java`
- **DTOs**: `src/main/java/com/athletecore/api/checkup/dto/` (request/response records por operación + formatter `mm:ss.ms`)
- **Tests**: `src/test/java/com/athletecore/api/checkup/`

### API Endpoints Nuevos (propuesta):
```
POST   /api/v1/checkups                                  # Registrar chequeo mensual con tiempos
GET    /api/v1/checkups                                  # Listar chequeos (filtros por mes/atleta)
GET    /api/v1/checkups/{id}                             # Detalle de un chequeo
DELETE /api/v1/checkups/{id}                             # Soft delete de chequeo
POST   /api/v1/athletes/{athleteId}/checkups             # Crear chequeo para un atleta
GET    /api/v1/athletes/{athleteId}/checkups             # Listar chequeos por atleta
GET    /api/v1/athletes/{athleteId}/projections          # Proyección de medallería del atleta
GET    /api/v1/checkups/{checkupId}/comparison           # Comparación de tiempos vs tabla nacional

# Tabla nacional de referencia (solo ADMIN)
POST   /api/v1/national-reference-times                  # Crear/actualizar tiempo de referencia
GET    /api/v1/national-reference-times                  # Listar tiempos de referencia
PUT    /api/v1/national-reference-times/{id}             # Actualizar tiempo de referencia
DELETE /api/v1/national-reference-times/{id}             # Soft delete de tiempo de referencia
```

### Dependencias:
- Aprovecha `BaseEntity` (en `domain/`) para auditoría y soft delete; `@SQLDelete`/`@SQLRestriction` se declaran en cada entidad concreta (no se heredan)
- Referencia a `Athlete` (módulo existente) vía FK `athlete_id` en `checkups`
- Jakarta Validation para request DTOs
- `Clock` inyectable para lógica determinista en tests (mismo patrón que `AlertService` del módulo training)

### Consideraciones:
- **Almacenamiento de tiempos**: segundos en `NUMERIC(10,3)` (3 decimales = milésimas) para soporte `mm:ss.ms`
- **Presentación `mm:ss.ms`**: responsabilidad exclusiva del DTO/Formatter; la entidad guarda segundos
- **Categoría**: categoría del atleta (ej: infantil, juvenil, mayor) — necesaria para match contra tabla nacional; se asume derivable de `Athlete` (a confirmar en design)
- **Tabla nacional**: 3 filas por (estilo + distancia + categoría) correspondientes a 1°, 2° y 3° puesto
- **Clasificación determinística**: la clasificación relativa se testea con datos fijos (sin `LocalDate.now()`)
- **Performance**: comparación en < 500 ms para 50 deportistas (requisito del agente checkup-domain); índices en `(style, distance, category, position)` en tabla nacional
- **Unicidad**: un chequeo por (athlete_id, mes, año); un `CheckupTime` por (checkup_id, style, distance)
