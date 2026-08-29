## Context

AthleteCore API cumple con tres módulos de negocio estabilizados — `athlete/` (v0.2.0), `training/` (v0.3.0) y `checkup/` (v0.4.0) — todos sobre `BaseEntity` (en `domain/`) con soft delete vía `@SQLDelete`/`@SQLRestriction` declarados en cada entidad concreta, validación Jakarta, DTOs locales en `<module>/dto/` y `Clock` inyectado para tests (patrón probado en `training/AlertService` y `user/security/JwtService`). El módulo de Reportes (v0.5.0) consolida esos datos en artefactos distribuibles: reportes individuales y generales exportables a PDF, con programación automática recurrente.

Los datos que consume el reporte ya existen y son accesibles vía servicios públicos de otros módulos:

- **Evolución de tiempos** (`checkup/`): `Checkup` + `CheckupTime` (style, distance, `timeSeconds` NUMERIC(10,3)) agrupados por (athlete, year, month, category); formateo `mm:ss.ms` responsabilidad de `TimeFormatter`.
- **Asistencia y alertas** (`training/`): `AttendanceService` (por sesión/deportista), `AlertService.getActiveAlerts()` (racha de ausencias consecutivas con `Clock`).
- **Proyección de medallería** (`checkup/`): `MedalProjectionService.getProjectionsForAthlete(athleteId)` devuelve `List<MedalProjection>` (record no persistido, decisión D5 del módulo checkup) con `classification` + `diffVsBronzeSeconds`.
- **Perfil y deportes** (`athlete/`): `AthleteProfileService`, `AthleteSportService` (perfil antropométrico y deportes asignados).

Stakeholders: backend-architect (migración V5 + dependencia PDF + `@EnableScheduling`), report-domain (entidades/repos/servicios/controllers/DTOs/tests), quality-guardian (auditoría final).

## Goals / Non-Goals

**Goals:**
- Esquema de datos V5 (`V5__reports_schema.sql`) para `reports`, `report_schedules` y `report_exports`.
- Entidades Java en `report/` que extiendan `BaseEntity`, con `@SQLDelete`/`@SQLRestriction` en cada clase concreta.
- DTOs de request/response (records), never exposing JPA entities.
- Servicios single-purpose: `AthleteReportingService`, `TeamReportingService`, `ReportGenerationService`, `ExportService`, `ReportScheduleService`.
- Generación de PDF (OpenPDF) síncrona + descarga del artefacto persistido.
- Programación automática con `@Scheduled` persistida en `report_schedules`.
- Cobertura de tests > 80% con datos fijos y `Clock` mockeado (determinístico).
- Reporte individual y general ensamblados en < 500 ms para 50 deportistas.

**Non-Goals:**
- Materializar una tabla `medal_projections` (ver decisión D3).
- Acceder a repositorios de otros módulos (frontera de módulo: solo servicios públicos).
- Rendering gráfico en el backend: los endpoints devuelven series temporales estructuradas; la renderización es responsabilidad del frontend futuro.
- Procesamiento asíncrono de reportes pesados (se registra como deuda en `TASKS.md:145`); la generación es síncrona a la escala actual.
- Integración con almacenamiento de objetos/sistema de archivos para PDFs (queda como futura mejora; hoy `BYTEA`).
- Quartz o gestores de cron externos (se usa `@Scheduled`).
- Documentación OpenAPI/Swagger (deuda técnica registrada en `TASKS.md:158`).

## Decisions

### D1: Librería PDF — OpenPDF
**Decisión:** Se usa `com.github.librepdf:openpdf` (rama mantenida de iText 4/5, licencia MPL/LGPL) para generar PDFs de forma programática.

**Alternativas:**
- (a) Apache PDFBox (Apache-2.0). Rechazada: muy low-level; construir tablas/layout verboso y propenso a errores.
- (b) iText 7 (AGPL/comercial). Rechazada: licencia AGPL fuerza open-source del producto o pago.
- (c) JasperReports. Rechazada: pensado para plantillas `.jrxml` y reporting enterprise; overkill para tablas simples.

**Por qué:** OpenPDF es ligero, sin restricciones de licencia problemáticas, suficiente para reportes tabulares (tablas de evolución de tiempos, asistencia y proyección), y el agente `report-domain` lo lista como opción preferida.

### D2: Programación — Spring `@Scheduled`, no Quartz
**Decisión:** La programación recurrente se implementa con `@EnableScheduling` + `@Scheduled`, usando `CronExpression` de Spring para parsear/calcular la siguiente ejecución. El estado (cron, active, last_run_at, next_run_at) se persiste en `report_schedules`.

**Alternativas:**
- (a) Quartz Scheduler. Rechazada: añade dependencias y configuración (job stores, clustering) innecesarias a escala < 50 deportistas y sin requisito de durabilidad de jobs acotada.
- (b) Cron a nivel de SO / K8s CronJob. Rechazada: desacopla la programación del modelo de dominio y de la API de gestión de schedules.

**Por qué:** `@Scheduled` es parte de Spring, sin dependencias nuevas, y al persistir los schedules en DB la API puede gestionarlos (CRUD). El scheduler sondea los schedules activos cuyo `next_run_at <= now` y recalcula el siguiente con `CronExpression.next()`.

### D3: Deuda D5 del checkup (`medal_projections`) — NO se materializa tabla
**Decisión:** El Módulo Reportes NO introduce la tabla `medal_projections`. Consume la proyección como derivación read-only vía `MedalProjectionService.getProjectionsForAthlete(...)` (decisión D5 del módulo checkup: "Projection does not mutate state").

**Alternativas:**
- (a) Materializar `medal_projections` en V5 (cache pre-calculada). Rechazada: duplicaría la lógica de derivación y obligaría a invalidar ante cualquier edit de `CheckupTime`/`NationalReferenceTime`; contradice la spec `medal-projection` existente. El PDF snapshot captura el resultado en el momento de generar, sin necesidad de persistir la proyección como fila.
- (b) Cache en memoria con TTL. Complejidad innecesaria a la escala actual.

**Consecuencia sobre `TASKS.md`:** permanece la deuda D5 documentada; no se agrega migración para `medal_projections`. El reporte es una vista consolidada, no un origen de verdad derivado.

### D4: Almacenamiento del export — `BYTEA` autocontenido
**Decisión:** El contenido PDF generado se guarda como `BYTEA` en `report_exports` (con `file_name`, `content_type` y `file_size_bytes`), permitiendo descarga repetida sin regenerar.

**Alternativas:**
- (a) Path en sistema de archivos / object storage. Rechazada para esta iteración: introduce gestión de rutas, limpieza y permisos sin beneficio a la escala actual.
- (b) No persistir (regenerar en cada descarga). Rechazada: PDF determinístico pero costoso de regenerar; perder auditabilidad del artefacto exportado.

**Por qué:** `BYTEA` es autocontenido, transaccional con el reporte y suficiente para PDFs pequeños (< 50 deportistas); la migración a object storage queda como mejora futura registrada.

### D5: Frontera de módulo — leer vía servicios, no repositorios
**Decisión:** Los servicios de `report/` obtienen datos exclusivamente a través de los servicios públicos de otros módulos (`MedalProjectionService`, `TimeComparisonService`, `ClassificationService`, `CheckupService`, `AlertService`, `AttendanceService`, `AthleteProfileService`, `AthleteSportService`), nunca inyectando sus repositorios. Referencia a `Athlete` vía FK `athlete_id` y `AthleteRepository` propia solo para validar existencia/resolver nombre (lectura de identidad compartida, ya usado por training/checkup).

**Alternativas:**
- (a) Inyectar `CheckupRepository`/`AttendanceRepository`/`NationalReferenceTimeRepository` en `report/`. Rechazada: viola `core/principles.md` (hexagonal/capas) y la regla del agente `report-domain` ("respetar fronteras de módulo").

**Por qué:** Mantiene desacoplado el módulo y evita acoplar report a detalles de persistencia de módulos cerrados.

### D6: FKs `ON DELETE RESTRICT`; `athlete_id` opcional
**Decisión:** FKs `NOT NULL` (`report_exports.report_id`) usan `ON DELETE RESTRICT`. Las FKs opcionales (`reports.athlete_id`, `report_schedules.athlete_id`) son `NULL`-ables y referencian `athletes(id) ON DELETE RESTRICT`; el borrado físico nunca ocurre vía JPA (soft delete), RESTRICT protege frente a SQL directo. Consistente con la convención V4 (D8 del checkup).

**Por qué:** Un reporte general no tiene deportista único; un reporte individual sí. La nulidad modela ambos casos sin dos tablas.

### D7: Lógica determinística — `Clock` inyectado
**Decisión:** El cómputo de `next_run_at`, la marca "generado hoy/ahora" y cualquier fecha relativa usan `Clock` inyectado (mismo patrón que `AlertService`). Tests fijan el tiempo.

**Por qué:** `ReportScheduleService` (scheduling) y `ReportGenerationService` (auditoría de generación) dependen de "ahora"; el `Clock` hace ambos deterministas y testeables.

### D8: Seguridad — no `permitAll()`; escrituras con rol
**Decisión:** Todos los endpoints `/api/v1/reports/**`, `/api/v1/athletes/{athleteId}/report`, `/api/v1/report-schedules/**` requieren autenticación. Generación y soft delete de reportes (`POST/DELETE /reports`) requieren `@PreAuthorize("hasAnyRole('ADMIN', 'COACH')")`; la gestión de programaciones (`POST/PUT/DELETE /report-schedules`) está restringida a `@PreAuthorize("hasRole('ADMIN')")` (consistente con la spec `report-scheduling` "Schedule management is restricted to ADMIN" y con la tabla nacional del módulo checkup); las lecturas (descarga incluida) quedan autenticadas por `anyRequest().authenticated()`. No se añaden reglas `permitAll()`.

**Por qué:** Consistencia con `checkup/` y `training/`; la descarga de PDF requiere autenticación (no es recurso público).

### D9: Ciclo de vida del reporte — `status` PENDING → GENERATED/FAILED
**Decisión:** `Report` persiste metadatos del reporte y un `status` (PENDING/GENERATED/FAILED) con `error_message` opcional. `ReportGenerationService` crea el `Report` en PENDING, ensambla datos, persiste el `ReportExport` y cierra en GENERATED; ante error lo marca FAILED con mensaje. La generación es idempotente en cuanto a metadatos (nuevo `Report` por petición; el export se adjunta).

**Por qué:** Da trazabilidad de fallos sin lógica de reintento compleja; el export es el artefacto consumible final.

## Risks / Trade-offs

- `[BYTEA en DB]` Crecimiento de `pg` por PDFs acumulados. **Mitigación:** escala acotada (<50 deportistas); migración a object storage documentada como mejora.
- `[Renderer síncrono]` PDF grande puede bloquear el hilo. **Mitigación:** reportes acotados; async/actuator queda como deuda (`TASKS.md:145`).
- `[@Scheduled single-node]` En replicas múltiples habría ejecución duplicada. **Mitigación:** hoy deployment single-node; si escala, mover a Quartz/leader election (documentado).
- `[CronExpression parsing]` Cron inválido debe rechazarse con 400. **Mitigación:** validación en `ReportScheduleService` al crear/actualizar (`ValidationException`).
- `[Frontera de módulo]` Cualquier intento del agente `report-domain` de inyectar repos ajenos debe rechazarse. **Mitigación:** `quality-guardian` valida imports y ausencia de modificaciones a módulos cerrados.
- `[N+1 en ensamblado]` Recorrer atletas × tiempos × referencias. **Mitigación:** reutilizar `MedalProjectionService` (que ya cachea triples) y `findActive...` con índices; budget 500 ms/50 atletas.

## Migration Plan

1. **Fase A — Esquema (backend-architect):** Crear `V5__reports_schema.sql` con 3 tablas + índices parciales + FKs `ON DELETE RESTRICT` + `COMMENT ON` + rollback documentado. Añadir dependencia OpenPDF en `pom.xml`; habilitar `@EnableScheduling` (AppConfig o configuración dedicada); propiedad `report.scheduling.*` en `application.properties`.
2. **Fase B — Entidades/Repos/Enums/DTOs (report-domain):** `Report`, `ReportSchedule`, `ReportExport` extienden `BaseEntity`; enums `ReportType`, `ReportStatus`; 3 repos; DTOs records en `report/dto/`.
3. **Fase C — Servicios + Controllers (report-domain):** `AthleteReportingService`, `TeamReportingService`, `ReportGenerationService`, `ExportService`, `ReportScheduleService`; `ReportController` + `ReportScheduleController` con `@PreAuthorize` según spec.
4. **Fase D — Tests + auditoría (report-domain + quality-guardian):** tests unitarios JUnit5+Mockito en español, datos fijos, `Clock` mockeado; tests e2e `@WebMvcTest`/`@DataJpaTest`; auditoría `quality-guardian` confirmando `BaseEntity`, `@SQLRestriction`, DTOs, seguridad, fronteras de módulo.
5. **Rollback:** `V5` revertible vía `DROP TABLE report_exports; DROP TABLE report_schedules; DROP TABLE reports;` respetando orden FK. Sin impacto en datos de módulos previos.

## Open Questions

- **Alcance de `generated_by`:** trazar qué usuario generó cada reporte (FK a `users`). Propuesto como opcional y diferido (YAGNI) salvo que se requiera audit trail; se decide en Fase A.
- **`report_schedules` y reporte general por categoría:** si el "general" debe filtrar por `category` (ya modelada en `reports.category` y `report_schedules.category`) o aceptar también `discipline_id`/`sport_id`. Se arranca con `category` (consistente con checkup).
- **Frecuencia de sondeo del scheduler** (`report.scheduling.poll-interval-ms`): default sugerido 60s; confirmar en Fase A.
- **¿Nueva config `@EnableScheduling` centralizada o dedicada?** Se propone AppConfig (ya es el bean config transversal); el agente backend-architect decide en Fase A.