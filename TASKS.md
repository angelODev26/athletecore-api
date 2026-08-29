# 📋 Tasks - AthleteCore API

## Resumen de Fase Actual

**Fase 0: Línea Base Limpia** ✅ COMPLETADA
- Auditoría completa de código encontrada
- 22 problemas corregidos
- Base limpia con User/Role básicos
- Configuración segura (JWT, BCrypt, seguridad)

---

## 📌 Fase 1: Módulos de Negocio (Próximo)

### Módulo 1: Deportistas (Athlete Domain)
**Agente:** [@athlete-domain](.claude/agents/athlete-domain.md)

#### Features Prioritarias:
- [x] Migración V2__athletes_schema.sql
  - [x] Tabla `athletes` (datos sociodemográficos)
  - [x] Tabla `athlete_profiles` (datos antropométricos)
  - [x] Tabla `sports` (deportes/disciplinas)
  - [x] Relación Many-to-Many athletes-sports
  - [x] Foto de perfil (URL referencia)
- [x] Entidades Java
  - [x] `Athlete` extends `BaseEntity`
  - [x] `AthleteProfile` extends `BaseEntity`
  - [x] `Sport` extends `BaseEntity`
  - [x] `Discipline` extends `BaseEntity`
- [x] Servicios
  - [x] `AthleteRegistrationService` (registro)
  - [x] `AthleteProfileService` (perfil antropométrico)
  - [x] `AthleteSportService` (asignación de deportes)
  - [x] `AthleteReportingService` (reportes — implementado en Módulo 4 Reportes)
- [x] Controllers REST
  - [x] `ATHLETES_API` (CRUD deportistas)
  - [x] Validaciones con Jakarta Validation
  - [x] Respuestas con DTOs (sin entidades JPA)
- [x] Tests unitarios (26 tests: registro, perfil, deportes — verificado en runtime contra PostgreSQL)

---

### Módulo 2: Entrenamientos (Training Domain)
**Agente:** [@training-domain](.claude/agents/training-domain.md)

#### Features Prioritarias:
- [x] Migración V3__training_schema.sql
  - [x] Tabla `training_plans` (plan anual)
  - [x] Tabla `training_cycles` (mesociclos/microciclos)
  - [x] Tabla `training_sessions` (sesiones individuales)
  - [x] Tabla `attendance` (control asistencia)
- [x] Entidades Java
  - [x] `TrainingPlan` extends `BaseEntity`
  - [x] `TrainingCycle` extends `BaseEntity`
  - [x] `TrainingSession` extends `BaseEntity`
  - [x] `Attendance` extends `BaseEntity`
- [x] Servicios
  - [x] `TrainingPlanService` (planificación)
  - [x] `TrainingSessionService` (sesiones)
  - [x] `AttendanceService` (control asistencia)
  - [x] `AlertService` (alertas ausencias)
- [x] Controllers REST
  - [x] `TRAINING_API` (CRUD entrenamientos)
  - [x] Alertas por ausencias consecutivas
  - [x] Respuestas con DTOs
- [x] Tests unitarios (54 tests: planes, ciclos, sesiones, asistencia, alertas — verificado con `./mvnw test -Dtest='*Training*Test,*Attendance*Test,*Alert*Test'`)

---

### Módulo 3: Chequeos Mensuales (Checkup Domain)
**Agente:** [@checkup-domain](.claude/agents/checkup-domain.md)

#### Features Prioritarias:
- [x] Migración V4__checkup_schema.sql
  - [x] Tabla `checkups` (chequeos mensuales)
  - [x] Tabla `checkup_times` (tiempos de prueba)
  - [x] Tabla `national_reference_times` (referencia nacional)
  - [~] Sin tabla — `MedalProjection` es record JVM no-entity (Design D5)
- [x] Entidades Java
  - [x] `Checkup` extends `BaseEntity`
  - [x] `CheckupTime` extends `BaseEntity`
  - [x] `NationalReferenceTime` extends `BaseEntity`
  - [~] `MedalProjection` es record (Design D5): no extiende `BaseEntity`, no se persiste
- [x] Servicios
  - [x] `CheckupService` (registro chequeos)
  - [x] `NationalReferenceTimeService` (CRUD admin tabla nacional — SRP)
  - [x] `TimeComparisonService` (comparación referencias)
  - [x] `MedalProjectionService` (proyección medallería)
  - [x] `ClassificationService` (clasificación por podio)
- [x] Controllers REST
  - [x] `CHECKUP_API` (CRUD chequeos: `CheckupController` + `NationalReferenceTimeController`)
  - [x] Tablas de referencia nacional (solo admin)
  - [x] Respuestas con DTOs y formato mm:ss.ms
- [x] Tests unitarios (72 tests: 59 unitarios + 13 e2e, verificados en runtime contra PostgreSQL)

---

### Módulo 4: Reportes (Report Domain)
**Agente:** [@report-domain](.claude/agents/report-domain.md)

#### Features Prioritarias:
- [x] Migración V5__reports_schema.sql
  - [x] Tabla `reports` (generación de reportes)
  - [x] Tabla `report_schedules` (programación)
  - [x] Tabla `report_exports` (exportaciones)
- [x] Entidades Java
  - [x] `Report` extends `BaseEntity`
  - [x] `ReportSchedule` extends `BaseEntity`
  - [x] `ReportExport` extends `BaseEntity`
- [x] Servicios
  - [x] `ReportGenerationService` (orquestación PENDING→GENERATED/FAILED)
  - [x] `AthleteReportingService` (reporte individual)
  - [x] `TeamReportingService` (reporte general)
  - [x] `ExportService` (exportación PDF)
  - [x] `ReportScheduleService` (programación)
- [x] Controllers REST
  - [x] `REPORTS_API` (CRUD reportes: `ReportController` + `ReportScheduleController`)
  - [x] Generación PDF de reportes (OpenPDF) y descarga
  - [x] Programación automática de reportes (`@Scheduled` + cron)
  - [x] Respuestas con DTOs
- [x] Tests unitarios (21 tests: 5 clases de servicio, JUnit5 + Mockito)

---

## 🔧 Mejoras de Infraestructura

### Seguridad Avanzada
- [ ] Implementar refresh tokens JWT
- [ ] Agregar rate limiting (Redis o memoria)
- [ ] Protección contra brute force (account lockout)
- [ ] MFA/2FA para roles administrativos
- [ ] Audit log de acciones sensibles

### Observabilidad
- [ ] Integrar Spring Boot Actuator
- [ ] Configurar métricas en Prometheus
- [ ] Logs estructurados en JSON (producción)
- [ ] Health checks personalizados (DB, Redis, etc.)
- [ ] Distributed tracing (OpenTelemetry)

### Performance
- [ ] Implementar caching con Redis
- [ ] Database connection pool optimization
- [ ] Query optimization e índice adicional
- [ ] paginación en endpoints de listado
- [ ] Async processing para reportes pesados

### DevOps
- [ ] Dockerfile multi-stage (optimizado)
- [ ] CI/CD pipeline (GitHub Actions)
- [ ] Health checks en Kubernetes (si aplica)
- [ ] Backup automático de base de datos
- [ ] Environment-specific configs (dev/staging/prod)

---

## 📝 Technical Debt (Pendientes)

### Documentación
- [ ] OpenAPI/Swagger documentation
- [ ] API README completo (endpoints, ejemplos)
- [ ] Diagramas de arquitectura
- [ ] Guías de desarrollo nuevo módulo

### Tests
- [ ] Integration tests para todos los módulos
- [ ] E2E tests críticos
- [ ] Test containers para PostgreSQL
- [ ] Coverage >80% en lógica de negocio

### Código
- [ ] Eliminar warnings de compilación
- [ ] Refactorizar servicios "Dios" en subservicios
- [ ] Agregar @JsonIgnore en campos sensibles
- [ ] Mejorar nombrado de métodos/repositorios

---

## 📅 Roadmap de Versiones

### v0.1.0 (Actual) - Base Limpia ✅
- User/Role básicos con seguridad
- Configuración Flyway
- Principios y arquitectura definidos

### v0.2.0 - Módulo Deportistas ✅
- CRUD deportistas completo
- Perfil antropométrico
- Gestión de deportes/disciplinas

### v0.3.0 - Módulo Entrenamientos
- Planificación anual
- Sesiones y ciclos
- Control de asistencia

### v0.4.0 - Módulo Chequeos ✅
- Registro de tiempos
- Comparación referencias
- Proyección medallería

### v0.5.0 - Módulo Reportes ✅
- Generación PDF
- Programación de reportes
- Exportaciones

### v1.0.0 - Producción
- Todos los módulos completos
- 80%+ cobertura de tests
- Documentación completa
- Security audit pasado

---

## 🔄 Proceso de Trabajo

### Para agregar nuevo módulo/feature:

1. **Crear rama específica:**
   ```bash
   git checkout -b feature/{módulo}-{feature}
   ```

2. **Planificar con OpenSpec:**
   - Usar skill `openspec-propose` para propuesta
   - Usar skill `openspec-explore` para investigar
   - Definir cambios necesarios

3. **Implementar:**
   - Migración Flyway (backend-architect)
   - Entidades (agentes de dominio)
   - Servicios y Controllers
   - DTOs (siguiendo principios)

4. **Tests:**
   - Unit tests (cobertura >80%)
   - Integration tests
   - E2E tests críticos

5. **Documentar:**
   - Actualizar TASKS.md
   - Agregar a CHANGELOG.md
   - Documentar en API README

6. **Merge:**
   - Pull Request → Revisión
   - Merge a `master` → Tags semánticos

---

## 📊 Estado Actual

| Módulo | Estado | Completado | Pendiente |
|--------|--------|------------|-----------|
| User/Security | ✅ Completo | 100% | 0% |
| Athlete | ✅ Completo | 100% | 0% (reportes → módulo Reportes) |
| Training | ✅ Completo | 100% | 0% (alertas → acknowledge sin persistencia, ver OpenSpec) |

> **Deuda técnica conocida — Módulo Training: alertas no persistentes**
>
> `AlertService.acknowledgeAlert` (líneas 94-112) está marcado con `@Transactional(readOnly = true)`
> porque las alertas son derivadas del historial de `Attendance` (no se persisten como entidad).
> El endpoint `POST /api/v1/alerts/attendance/{athleteId}/acknowledge` solo valida y devuelve la
> alerta actual, pero **no muta estado**: la próxima consulta `GET /api/v1/alerts/attendance`
> volverá a listar la misma alerta. La racha solo se limpia cuando se registra una asistencia
> PRESENTE o JUSTIFICADA que la rompa.
>
> **Decisión:** mantener como deuda conocida hasta observar el comportamiento en producción y
> decidir el modelo de persistencia adecuado. Si la UX operacional lo requiere, la corrección
> contemplada es una nueva entidad `AlertAcknowledgment extends BaseEntity` (con FK `athlete_id`,
> `acknowledged_by`, `acknowledged_at`, `streak_snapshot`, `last_absence_date`) + migración V4
> + filtrado en `AlertService.getActiveAlerts` que compare el último ACK con la fecha del último
> AUSENTE de la racha actual. Ver `openspec/changes/implement-training-domain/tasks.md` tarea 6.2.
>
> **Deuda técnica conocida — Módulo Checkup: `medal_projections` sin tabla persistente**
>
> Por la decisión D5 del design (`openspec/changes/implement-checkup-domain/design.md`),
> `MedalProjection` se modela como record JVM (no `@Entity`) construido en tiempo de consulta
> por `MedalProjectionService` (`@Transactional(readOnly = true)`) a partir de los `CheckupTime`
> activos y el triple activo de `NationalReferenceTime` para cada (style, distance, category).
> **No hay tabla `medal_projections`** en V4: la spec `medal-projection` exige
> "Projection does not mutate state". Si el Módulo Reportes (v0.5.0) requiere materializar la
> proyección (cache pre-calculada o exportación PDF), ese change introducirá la tabla y la
> migración asociada.
| Checkup | ✅ Completo | 100% | 0% (deuda D5 `medal_projections` sin tabla) |
| Reports | ✅ Completo | 100% | 0% (deuda D5 `medal_projections` sin tabla — confirmada decisión D3 del change) |
| Infraestructura | 🚧 Parcial | 30% | 70% |
| Tests | 🚧 Parcial | 45% | 55% |

---

**Última actualización:** 2026-08-29  
**Próxima revisión de roadmap:** Cada 2 sprints o cuando se completa un módulo
