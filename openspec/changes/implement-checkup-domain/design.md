## Context

AthleteCore API cumple con dos módulos de negocio ya estabilizados: `athlete/` (v0.2.0) y `training/` (v0.3.0), ambos sobre `BaseEntity` (en `domain/`) con soft delete vía `@SQLDelete`/`@SQLRestriction` declarados en cada entidad concreta, validación Jakarta, DTOs locales en `<module>/dto/` y `Clock` inyectado para tests (patrón ya probado en `training/AlertService`). El módulo de Chequeos (v0.4.0) introduce evaluación de rendimiento mensual: registrar tiempos por estilo+distancia, compararlos contra una tabla nacional de tiempos de referencia y proyectar medallería con clasificación determinística.

El `Athlete` actual no modela categoría (solo `birthDate`, ver `src/main/java/com/athletecore/api/athlete/Athlete.java:73`). La tabla nacional de referencia debe matchear por (style, distance, category); sin categoría explícita el match colisiona. La decisión acordada en esta sesión: modelar `category` como string en `Checkup` y `NationalReferenceTime` (no tocar `Athlete`), con una futura derivación opcional `birthDate → categoría` cuando se consoliden reglas nacionales.

Stakeholders: backend-architect (migración V4 + índices), checkup-domain (entidades/repos/servicios/controller/DTOs/tests), quality-guardian (auditoría final). El módulo Reportes (v0.5.0) consumirá estos datos más adelante, pero `AthleteReportingService` (listado en `TASKS.md:34` como pendiente) NO es parte de este change: pertenece al Módulo 4 Reportes.

## Goals / Non-Goals

**Goals:**
- Esquema de datos V4 (`V4__checkup_schema.sql`) para `checkups`, `checkup_times`, `national_reference_times` y (si la implementación lo requiere) `medal_projections` — ver decisión D5 sobre `MedalProjection`.
- Entidades Java en `checkup/` que extiendan `BaseEntity`, con `@SQLDelete`/`@SQLRestriction` en cada clase concreta.
- DTOs de request/response (records) con formatter `mm:ss.ms` en la capa de presentación.
- Servicios: `CheckupService`, `NationalReferenceTimeService`, `TimeComparisonService`, `MedalProjectionService`, `ClassificationService` — un servicio por responsabilidad (no god service).
- Controller con `@PreAuthorize("hasRole('ADMIN')")` para escrituras de tabla nacional y rutas de consulta abiertas a roles operativos.
- Cobertura de tests > 80% con datos de prueba fijos y lógica determinística (sin `LocalDate.now()` directo).
- Comparación y proyección en < 500 ms para 50 deportistas, indexes backed.

**Non-Goals:**
- Persistencia de proyección de medallería (es una operación de lectura; ver decisión D5).
- Sincronización automática con federaciones externas o feeds de tiempos nacionales.
- Derivar categoría automáticamente desde `birthDate` (queda como futura mejora; ver D1).
- Tocar la entidad `Athlete` o cualquier módulo cerrado.
- Implementar `AthleteReportingService` (pertenece al Módulo 4 Reportes — `TASKS.md:34`).
- Documentación OpenAPI/Swagger o tests E2E (deuda técnica registrada en `TASKS.md:158,165`).

## Decisions

### D1: Categoría del atleta — string explícito, no derivación
**Decisión:** `category` se almacena como `VARCHAR(30) NOT NULL` en `Checkup` y `NationalReferenceTime` (validado contra un enum de categorías conocidas a nivel Java, sin restringirlo en DB para permitir extensión).

**Alternativas:**
- (a) Derivar categoría desde `birthDate` con reglas nacionales. Rechazada porque las reglas por federación varían y `Athlete` no las modela; precipitar derivación ahora acopla el módulo Checkup a criterios externos no acordados.
- (b) Agregar `category` a `Athlete`. Rechazada porque abre un módulo cerrado y mezcla responsabilidades: la categoría pertenece al contexto de competición, no a la identidad del deportista.

**Por qué:** Mantiene módulos desacoplados; deja la derivación opcional como capa superior sin tocar entidades estables. El enum Java (p.ej. `CheckupCategory`) valida input y normaliza strings sin fijar DB.

### D2: Almacenamiento de tiempos — segundos decimales, NO `INTERVAL`
**Decisión:** Los tiempos (de trial y de referencia) se guardan como `NUMERIC(10,3)` (segundos con hasta 3 decimales = milisegundos), nunca como `INTERVAL`. La presentación `mm:ss.ms` vive en un formatter (`TimeFormatter`) usado por los DTOs.

**Alternativas:**
- (a) `INTERVAL` de PostgreSQL. Rechazada: JPA/Hibernate no mapea `INTERVAL SECOND` de forma portátil y el cálculo de diferencias se vuelve verboso.
- (b) String `mm:ss.ms` en DB. Rechazada: pierde capacidad de ordenar/restar en SQL; obliga a parsers en toda consulta.
- (c) `BIGINT` de milisegundos. Viable pero menos legible en queries ad-hoc y consume validación de rango extra.

**Por qué:** `NUMERIC(10,3)` permite aritmética directa en SQL (diferencias), comparaciones (`<`) y read más simple; 3 decimales cubren milisegundos sin sobreingeniería.

### D3: Tabla nacional — 3 filas por triple (style, distance, category), no columnas `t1/t2/t3`
**Decisión:** `national_reference_times` guarda una fila por (style, distance, category, position), con `position SMALLINT CHECK (position IN (1,2,3))`.

**Alternativas:**
- (a) Columnas `time_gold`, `time_silver`, `time_bronze` en una sola fila. Rechazada: viola 1NF conceptual (3 valores del mismo atributo) y dificulta añadir un 4° puesto en el futuro.
- (b) JSONB con array posicional. Rechazada: pierde tipos y validación por columna.

**Por qué:** Normaliza el modelo y simplifica JOINs posteriores para Ranking; permite extensión a 4°/5° puesto sin migración de esquema.

### D4: Umbral "Cercano a medallería" — configuración por properties, no hardcodeado
**Decisión:** La cota se lee de `checkup.medal-proximity-threshold-seconds` (default p.ej. `1.500`); inyectada vía `@Value` en `ClassificationService` para permitir testing con valor fijo.

**Alternativas:**
- (a) Hardcodear 1.5 s. Rechazada: varía por deporte/distancia; requeriría recodificar.
- (b) Umbral relativo (% del 3° puesto). Pendiente para iteración futura; el umbral absoluto es más simple de comunicar a entrenadores y testeable con datos fijos.

**Por qué:** El patron ya existe en `AlertService` (N ausencias configurable); replica una convención probada en el repo.

### D5: `MedalProjection` no se persiste — row de cálculo en tiempo de consulta
**Decisión:** `MedalProjection` se modela como una **clase de dominio** (no `@Entity`): el Service la construye a partir de `CheckupTime` + `NationalReferenceTime` y la devuelve en DTO. No hay tabla `medal_projections`.

**Alternativas:**
- (a) Tabla persistente `medal_projections` cacheada. Rechazada: duplica lógica de derivación, obliga invalidación ante cualquier edit de `CheckupTime`/`NationalReferenceTime`, y la spec `medal-projection` exige "Projection does not mutate state". Performance objetivo (<500 ms / 50 atletas) se cubre con índices.
- (b) Una tabla cacheada con TTL. Complejidad innecesaria para escala actual.

**Consecuencia sobre `TASKS.md:78`:** La fila "Tabla `medal_projections`" queda como NON-GOAL de esta iteración y se documentará como deuda ajustada; el modelo JVM `MedalProjection` (record/POJO) mantiene el nombre pero no hay migración asociada. Si a futuro el Módulo Reportes necesita materializarla, ese change introducirá la tabla.

### D6: Lógica determinística — `Clock` inyectado, sin `LocalDate.now()` directo
**Decisión:** Cualquier referencia temporal (ej. "mes actual" en validaciones) se obtiene vía `Clock` inyectado en services. Tests fijan tiempo.

**Por qué:** Patrón idéntico a `AlertService` en `training/`; ya hay tests que lo demuestran.

### D7: Índices y performance — índices parciales `WHERE deleted_at IS NULL`
**Decisión:** En V4, índices de unicidad y de consulta se declaran como `UNIQUE (...) WHERE deleted_at IS NULL` (para uniqueness de filas activas sin colisión con filas soft-deleted) e índices `WHERE deleted_at IS NULL` para búsquedas frecuentes:
- `checkups`: `UNIQUE (athlete_id, year, month, category) WHERE deleted_at IS NULL`, `INDEX (athlete_id) WHERE deleted_at IS NULL`
- `checkup_times`: `UNIQUE (checkup_id, style, distance) WHERE deleted_at IS NULL`, `INDEX (checkup_id) WHERE deleted_at IS NULL`
- `national_reference_times`: `UNIQUE (style, distance, category, position) WHERE deleted_at IS NULL`, `INDEX (style, distance, category) WHERE deleted_at IS NULL`

**Por qué:** Consistenta con V1–V3 (índices parciales sobre `deleted_at`), resuelve unicidad que convive con soft delete, y mantiene el budget de 500 ms.

### D8: FKs `ON DELETE RESTRICT`, no cascada sobre atleta
**Decisión:** `checkups.athlete_id` (y FKs entre `checkup_times` y `checkups`) usan `ON DELETE RESTRICT`; el borrado físico nunca ocurre vía JPA (soft delete), por lo que RESTRICT protege consistencia si alguien ejecuta SQL directo (obliga a borrar hijos antes que el padre, sin propagación silenciosa).

**Nota sobre convención existente:** V3 (`V3__training_schema.sql`) usa en realidad `ON DELETE CASCADE` en sus FKs (ver líneas 162 y 166 de `V3__training_schema.sql`). V4 introduce el patrón más seguro `RESTRICT` como nueva convención a partir de este módulo; V3 queda como deuda técnica conocida (no se corrige en este change). Si se quiere uniformar, abrir change separado para migrar V3 a RESTRICT — fuera del scope aquí.

**Alternativas:**
- (a) `ON DELETE CASCADE` (como V3). Rechazada: propaga borrados físicos accidentales de `athletes` de forma silenciosa; incompatible con la disciplina de soft delete.
- (b) `ON DELETE SET NULL` para FKs opcionales (no aplica: `athlete_id` y `checkup_id` son `NOT NULL`).

### D9: SecurityConfig — no `permitAll()` para endpoints del módulo
**Decisión:** Todos los endpoints `/api/v1/checkups/**`, `/api/v1/athletes/{id}/checkups`, `/api/v1/athletes/{id}/projections`, `/api/v1/national-reference-times/**` requieren autenticación. La escritura de tabla nacional exige `hasRole('ADMIN')` vía `@PreAuthorize`. No se añaden reglas `permitAll()` en `SecurityConfig`; la regla por defecto (autenticado) cubre consulta.

**Por qué:** Consistencia con `training/` (no abrió endpoints nuevos como públicos) y cumplimiento del agente `backend-architect`.

## Risks / Trade-offs

- `[Category libre]` Permitir `category` como string validado por enum Java deja la puerta abierta a categorías no soportadas si el enum no se mantiene. **Mitigación:** Enum cerrado revisable, tests de validación de input, y mensaje 400 claro ante categoría desconocida.
- `[Performance de comparación con JOINs por position]` La comparación contra 3 filas `national_reference_times` por (style, distance, category) puede sumar N×3 filas. **Mitigación:** Índice `(style, distance, category)` y obtención por query única con `IN (1,2,3)` o `JOIN ... USING (...)`; tests de performance si el budget falla.
- `[MedalProjection no persistente → costo de cómputo cada consulta]` Cada petición recalcula proyecciones. **Mitigación:** A escala actual (<50 atletas) dentro del budget 500 ms; si se deteriora, introducir cache oulayer (no tabla persistente) o precomputar en Reportes (Módulo 4) con TTL.
- `[Umbral absoluto vs relativo]` El umbral configurable absoluto (D4) puede ser injusto entre distancias muy distintas (50m vs 1500m). **Mitigación:** Hoy threshold global; si se detecta dependencia por distancia, una iteración futura introduce preset por (style, distance, category) o umbral relativo %.
- `[Módulo ya cerrado (Athlete)]` Cualquier typo del agente `checkup-domain` que proponga añadir `category` a `Athlete` debe rechazarse en review. **Mitigación:** `quality-guardian` valida explícitamente que `Athlete.java` no fue modificado.
- `[Divergencia vs TASKS.md:78]` Dejar `medal_projections` sin tabla es deuda explícita contra `TASKS.md`. **Mitigación:** Se documenta en el `tasks.md` del OpenSpec change y `quality-guardian` lo valida en la auditoría final; `TASKS.md` se ajusta al final del módulo.

## Migration Plan

1. **Fase A — Esquema (backend-architect):** Crear `V4__checkup_schema.sql` con las 3 tablas (`checkups`, `checkup_times`, `national_reference_times`) + índices parciales + FKs `ON DELETE RESTRICT`. No incluye `medal_projections` (ver D5). Script reversible documentado (`DROP TABLE ... CASCADE` en rollback).
2. **Fase B — Entidades/Repos/DTOs (checkup-domain):** `Checkup`, `CheckupTime`, `NationalReferenceTime` extienden `BaseEntity`; `MedalProjection` como POJO/record. DTOs con records + `TimeFormatter` para `mm:ss.ms`.
3. **Fase C — Servicios + Controller (checkup-domain):** `CheckupService`, `NationalReferenceTimeService`, `TimeComparisonService`, `MedalProjectionService`, `ClassificationService`, `CheckupController`/`NationalReferenceTimeController` con `@PreAuthorize` según spec.
4. **Fase D — Tests + auditoría (checkup-domain + quality-guardian):** Tests unitarios JUnit5+Mockito en español, datos fijos, `Clock` mockeado, escenarios de las 4 specs cubiertos. Auditoría `quality-guardian` confirma `BaseEntity`, `@SQLRestriction`, DTOs, seguridad.
5. **Rollback:** `V4` revertible mediante `DROP TABLE checkup_times; DROP TABLE checkups; DROP TABLE national_reference_times;` respetando FK order (hijos antes que padres). No impacto en datos `Athlete`/`Training`.

## Open Questions

- **Categorías a soportar como enum inicial** (p.ej. `INFANTIL`, `JUVENIL`, `MAYOR`, `MASTER`) — se propondrá en `tasks.md` y lo confirma el usuario en la fase A. Si no se confirma, se arranca con `INFANTIL/JUVENIL/MAYOR` como default extensible.
- **Endpoint unificado de proyección de equipo vs por atleta**: spec `medal-projection` cubre por atleta; si se necesita endpoint `/api/v1/projections` global, se añade en fase C sin crear tabla.
- **¿Crear PR Feature contra `master` o branch `feature/implement-checkup-domain`?** El workflow del proyecto (ver `TASKS.md:213`) sugiere `feature/{módulo}-{feature}`. Se abre la rama antes del primer commit; la decisión final la toma el usuario.
