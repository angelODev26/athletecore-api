## 1. Migración, dependencia y config (subagente: backend-architect)

- [x] 1.1 Crear `V5__reports_schema.sql` con tablas `reports`, `report_schedules`, `report_exports`, campos `created_at`/`updated_at`/`deleted_at` (`TIMESTAMP WITH TIME ZONE`), constraints CHECK de soft delete y `COMMENT ON` siguiendo el patrón V4
- [x] 1.2 Definir `reports`: `id BIGSERIAL PK`, `report_type VARCHAR(20) NOT NULL CHECK (report_type IN ('INDIVIDUAL','GENERAL'))`, `athlete_id BIGINT NULL` (FK `athletes.id ON DELETE RESTRICT`), `category VARCHAR(30) NULL`, `year INTEGER NULL`, `month INTEGER NULL`, `title VARCHAR(200) NOT NULL`, `status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','GENERATED','FAILED'))`, `error_message TEXT NULL`; índices parciales `(athlete_id)` y `(report_type)` `WHERE deleted_at IS NULL`
- [x] 1.3 Definir `report_schedules`: `id BIGSERIAL PK`, `report_type VARCHAR(20) NOT NULL`, `athlete_id BIGINT NULL` (FK `athletes.id ON DELETE RESTRICT`), `category VARCHAR(30) NULL`, `cron_expression VARCHAR(100) NOT NULL`, `timezone VARCHAR(63) NULL`, `active BOOLEAN NOT NULL DEFAULT TRUE`, `last_run_at TIMESTAMPTZ NULL`, `next_run_at TIMESTAMPTZ NULL`; índice parcial `(active)` `WHERE deleted_at IS NULL`
- [x] 1.4 Definir `report_exports`: `id BIGSERIAL PK`, `report_id BIGINT NOT NULL` (FK `reports.id ON DELETE RESTRICT`), `format VARCHAR(20) NOT NULL DEFAULT 'PDF'`, `file_name VARCHAR(255) NOT NULL`, `file_size_bytes BIGINT NULL`, `content_type VARCHAR(100) NOT NULL DEFAULT 'application/pdf'`, `content BYTEA NOT NULL`; índice parcial `(report_id)` `WHERE deleted_at IS NULL`
- [x] 1.5 Documentar script de rollback al final (`DROP TABLE report_exports; DROP TABLE report_schedules; DROP TABLE reports;`) y verificar `./mvnw compile` (BUILD SUCCESS)
- [x] 1.6 Agregar dependencia `com.github.librepdf:openpdf` en `pom.xml` (ver decisión D1)
- [x] 1.7 Habilitar `@EnableScheduling` (AppConfig u otra config) y exponer `report.scheduling.poll-interval-ms=${REPORT_SCHEDULING_POLL_INTERVAL_MS:60000}` en `application.properties` (+ `application-local.properties` gitignored)

## 2. Entidades, enums y repositorios (subagente: report-domain)

- [x] 2.1 Crear enums `ReportType` ({INDIVIDUAL, GENERAL}) y `ReportStatus` ({PENDING, GENERATED, FAILED}) en `report/` raíz
- [x] 2.2 Crear entidad `Report extends BaseEntity` con `@SQLDelete`/`@SQLRestriction`, `@SequenceGenerator` (`reports_id_seq`), `reportType` (`@Enumerated(STRING)`), `athleteId` (FK `athletes.id` opcional), `category`, `year`, `month`, `title`, `status` (`@Enumerated(STRING)`), `errorMessage`
- [x] 2.3 Crear entidad `ReportSchedule extends BaseEntity` con `@SQLDelete`/`@SQLRestriction`, `reportType`, `athleteId` (opcional), `category`, `cronExpression`, `timezone`, `active`, `lastRunAt` (`Instant`), `nextRunAt` (`Instant`)
- [x] 2.4 Crear entidad `ReportExport extends BaseEntity` con `@SQLDelete`/`@SQLRestriction`, `@ManyToOne(fetch=LAZY)` a `Report`, `format`, `fileName`, `fileSizeBytes`, `contentType`, `content` (`byte[]` SIN `@Lob`: en Hibernate 6/PostgreSQL `@Lob byte[]` mapea a `oid` y rompería `validate` contra `content BYTEA`)
- [x] 2.5 Crear repositorios `ReportRepository`, `ReportScheduleRepository`, `ReportExportRepository` con consultas `findActive...`/`existsActive...` (`@Query` JPQL explícito donde Spring Data no deriva)

## 3. DTOs (subagente: report-domain)

- [x] 3.1 Crear DTOs request/response de reporte en `report/dto/`: `GenerateReportRequest` (`reportType`, `athleteId?`, `category?`, `year?`, `month?`, `title`), `ReportResponse`, `ReportDetailResponse` (con lista de exports)
- [x] 3.2 Crear DTOs de contenido del reporte: `IndividualReportResponse` (sección evolución de tiempos, resumen asistencia, proyección), `TeamReportResponse` (entradas agregadas por style+distance+category), con factories `from...(dominio)`
- [x] 3.3 Crear DTOs de export: `ReportExportResponse` (metadata sin binario; el binario solo se sirve en el endpoint de descarga)
- [x] 3.4 Crear DTOs de schedule: `CreateReportScheduleRequest`, `UpdateReportScheduleRequest`, `ReportScheduleResponse`

## 4. Servicios (subagente: report-domain)

- [x] 4.1 Crear `AthleteReportingService`: por `athleteId`, ensambla evolución de tiempos (serie por style+distance ordenada por year/month vía `CheckupService`), resumen de asistencia (vía `AttendanceService` + `AlertService`) y proyección (vía `MedalProjectionService`); `@Transactional(readOnly = true)`; lanza `ResourceNotFoundException` (404) si el atleta no existe
- [x] 4.2 Crear `TeamReportingService`: agrega por (style, distance, category) los mejores tiempos y clasificaciones de los atletas (vía servicios checkup); `@Transactional(readOnly = true)`
- [x] 4.3 Crear `ExportService`: renderiza PDF con OpenPDF (tablas de evolución, asistencia, proyección) y persiste `ReportExport`; expone método de descarga (devuelve bytes + `contentType` + `fileName`)
- [x] 4.4 Crear `ReportGenerationService`: valida request, crea `Report` en PENDING, delega a `AthleteReportingService`/`TeamReportingService` según type, invoca `ExportService`, cierra en GENERATED o marca FAILED con `errorMessage`; `@Transactional`
- [x] 4.5 Crear `ReportScheduleService`: CRUD de schedules (validando cron vía `CronExpression` → `ValidationException` 400); método `runDueSchedules()` ejecutado por `@Scheduled` que dispara `ReportGenerationService` para schedules activos con `next_run_at <= now(clock)` y recalcula `next_run_at`
- [x] 4.6 Inyectar `Clock` donde exista dependencia de "ahora" (scheduling y auditoría de generación), patrón `AlertService`

## 5. Controllers y seguridad (subagente: report-domain)

- [x] 5.1 Crear `ReportController` en `/api/v1`: `POST /reports` (generar, `@PreAuthorize("hasAnyRole('ADMIN','COACH')")`), `GET /reports` (listar con filtros type/athleteId/status), `GET /reports/{id}` (detalle), `DELETE /reports/{id}` (soft delete), `GET /reports/{id}/export` (descarga PDF), `GET /reports/{id}/exports` (lista exports), `GET /athletes/{athleteId}/report` (individual), `GET /reports/team` (general)
- [x] 5.2 Crear `ReportScheduleController` en `/api/v1/report-schedules`: `POST`/`PUT`/`DELETE` con `@PreAuthorize("hasRole('ADMIN')")`, `GET` y `GET /{id}` autenticados
- [x] 5.3 `SecurityConfig` NO requiere `permitAll()`: `anyRequest().authenticated()` (línea 52) cubre los endpoints; verificar que `/api/v1/reports/team` (segmento literal) no colisiona con `/api/v1/reports/{id}`
- [x] 5.4 `GlobalExceptionHandler` NO modificada: handlers existentes cubren 404/409/400/403; `ValidationException` (cron inválido) → 400 ya manejado

## 6. Tests y auditoría final (subagentes: report-domain + quality-guardian)

- [x] 6.1 `AthleteReportingServiceTest` (JUnit5 + Mockito, `@DisplayName` español): reporte con datos, atleta sin checkups → secciones vacías, atleta inexistente → 404
- [x] 6.2 `TeamReportingServiceTest`: agregación por style+distance+category, sin datos → vacío, determinismo
- [x] 6.3 `ExportServiceTest`: genera PDF con contenido esperado, export de reporte FAILED → 409, persistencia de `ReportExport`
- [x] 6.4 `ReportGenerationServiceTest`: flujo PENDING→GENERATED, error → FAILED con mensaje, no muta módulos fuente (verify nunca `save` de repos ajenos)
- [x] 6.5 `ReportScheduleServiceTest`: crear schedule válido (calcula next_run_at), cron inválido → 400, due schedule genera reporte, inactivo se omite, fallo no desactiva, `Clock` mockeado
- [x] 6.6 `./mvnw compile` sin warnings nuevos; tests focused del módulo en verde; tests `@SpringBootTest` requieren PostgreSQL vivo (`docker-compose up -d postgres`)
- [x] 6.7 Auditoría `quality-guardian`: valida `BaseEntity`, `@SQLRestriction` (no `@Where`), DTOs sin entidades JPA, `@PreAuthorize` en escrituras, frontera de módulo (sin repos ajenos en `report/`), módulos cerrados no modificados, `SecurityConfig` sin `permitAll()`
- [x] 6.8 Actualizar `TASKS.md` raíz: módulo Reportes marcado ✅; `v0.5.0` ✅; fecha; confirmar que la deuda D5 (`medal_projections`) permanece sin tabla (decisión D3 del change); `AthleteReportingService` (TASKS.md:34) ahora implementado