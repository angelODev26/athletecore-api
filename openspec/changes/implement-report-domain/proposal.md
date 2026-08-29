## Why

Los módulos Athlete (v0.2.0), Training (v0.3.0) y Checkup (v0.4.0) ya cubren ficha deportiva, operación diaria y evaluación mensual de rendimiento, pero no hay forma de consolidar esos datos en un artefacto distribuible: un reporte de evolución de tiempos, asistencia y proyección de medallería, por deportista o por equipo, exportable a PDF y programable. El módulo de Reportes (v0.5.0) es el cierre del ciclo de gestión del desempeño deportivo: transforma los datos operativos en informes accionables para entrenadores y dirección técnica.

## What Changes

### Nuevas Funcionalidades:
- **Reporte individual** por deportista: evolución de tiempos de prueba (serie temporal por style+distancia), resumen de asistencia (total, presentes/ausentes/justificados, racha actual) y proyección de medallería.
- **Reporte general** de equipo/categoría: comparativo de rendimiento por (style, distance, category).
- **Exportación a PDF** de reportes con descarga vía API.
- **Programación automática** de reportes recurrentes (cron), persistida y ejecutada por un scheduler del propio Spring.
- Soft delete para todas las entidades del módulo (extienden `BaseEntity`).

### Modificaciones:
- Migración Flyway `V5__reports_schema.sql` (nueva): tablas `reports`, `report_schedules`, `report_exports`.
- Entidades: `Report`, `ReportSchedule`, `ReportExport` (extienden `BaseEntity`).
- Servicios (SRP): `AthleteReportingService`, `TeamReportingService`, `ReportGenerationService`, `ExportService`, `ReportScheduleService`.
- Controllers: `ReportController` (REPORTS_API) y `ReportScheduleController` (programación admin).
- DTOs records (`report/dto/`) sin exponer entidades JPA.
- Dependencia de generación PDF (ver decisión D1 en `design.md`).
- Habilitación de scheduling en configuración (`@EnableScheduling`).

### BREAKING:
- No hay breaking changes: se agrega funcionalidad nueva y se reutilizan los servicios existentes de los módulos cerrados sin modificarlos.

## Capabilities

### New Capabilities
- **report-generation**: Reportes individuales (por deportista) y generales (por equipo/categoría) que consolidan evolución de tiempos, asistencia y proyección de medallería.
- **report-export**: Exportación de reportes a PDF y descarga del artefacto generado.
- **report-scheduling**: Programación automática de reportes recurrentes mediante expresión cron, con ejecución desatendida.

### Modified Capabilities
- Ninguno (módulo nuevo sin afectar requirements existentes).

## Impact

### Archivos Nuevos (estimación):
- **Migración**: `src/main/resources/db/migration/V5__reports_schema.sql`
- **Entidades**: `Report.java`, `ReportSchedule.java`, `ReportExport.java` (+ enums `ReportType`, `ReportStatus`)
- **Repositorios**: `ReportRepository`, `ReportScheduleRepository`, `ReportExportRepository`
- **Servicios**: `AthleteReportingService`, `TeamReportingService`, `ReportGenerationService`, `ExportService`, `ReportScheduleService`
- **Controllers**: `ReportController.java`, `ReportScheduleController.java`
- **DTOs**: `src/main/java/com/athletecore/api/report/dto/` (request/response records por operación)
- **Tests**: `src/test/java/com/athletecore/api/report/`

### API Endpoints Nuevos (propuesta):
```
# Generación de reportes
POST   /api/v1/reports                              # Generar reporte (type, athleteId?, category?, year?, month?)
GET    /api/v1/reports                              # Listar reportes (filtros type/athleteId/status)
GET    /api/v1/reports/{id}                         # Detalle del reporte (+ exports asociados)
DELETE /api/v1/reports/{id}                         # Soft delete

# Exportación PDF
GET    /api/v1/reports/{id}/export                  # Descarga del PDF (lo genera si no existe)
GET    /api/v1/reports/{id}/exports                 # Lista de exports del reporte

# Reporte individual (datos estructurados para frontend)
GET    /api/v1/athletes/{athleteId}/report           # Evolución, asistencia y proyección

# Reporte general
GET    /api/v1/reports/team                          # Comparativo por equipo/categoría

# Programación (solo ADMIN)
POST   /api/v1/report-schedules                     # Crear programación
GET    /api/v1/report-schedules                     # Listar programaciones
PUT    /api/v1/report-schedules/{id}                # Actualizar programación
DELETE /api/v1/report-schedules/{id}                # Soft delete / desactivar
```

### Dependencias:
- Aprovecha `BaseEntity` (en `domain/`) para auditoría y soft delete; `@SQLDelete`/`@SQLRestriction` se declaran en cada entidad concreta.
- **Reutiliza servicios de módulos existentes** (frontera de módulo, sin acceso directo a repos ajenos): `MedalProjectionService`, `TimeComparisonService`, `ClassificationService`, `CheckupService` (checkup), `AlertService`, `AttendanceService` (training), `AthleteProfileService`, `AthleteSportService` (athlete).
- `Clock` inyectable para lógica determinista de scheduling (patrón `AlertService`).
- OpenPDF (librería PDF) — ver decisión D1 en `design.md`.

### Consideraciones:
- **Frontera de módulo**: los reportes leen datos vía servicios públicos de otros módulos, nunca vía sus repositorios internos.
- **MedalProjection (deuda D5 del checkup)**: la proyección se consume como derivación read-only de `MedalProjectionService`; el reporte NO materializa una tabla `medal_projections` (ver D3 en `design.md`).
- **PDF**: generación síncrona (escala < 50 deportistas); el artefacto se persiste como `BYTEA` para descarga repetida sin regenerar.
- **Scheduling**: `@Scheduled` + `@EnableScheduling` (sin Quartz).
- **Performance**: reporte individual/general ensamblado en < 500 ms para 50 deportistas (índices respaldados).