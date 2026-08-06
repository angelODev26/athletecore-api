## 1. Migración y esquema

- [x] 1.1 Crear `V3__training_schema.sql`: tablas `training_plans`, `training_cycles` (con autorelación `parent_cycle_id`), `training_sessions`, `attendance` con campos BaseEntity (`created_at`, `updated_at`, `deleted_at`) y constraints CHECK de soft delete
- [x] 1.2 Agregar índices parciales (`WHERE deleted_at IS NULL`) para consultas frecuentes: sesiones por ciclo, asistencia por sesión, asistencia por deportista, ciclos por plan
- [x] 1.3 Agregar FKs (plan → training_plans, ciclo → plan/ciclo padre, sesión → ciclo/disciplina, asistencia → sesión/deportista) y constraint UNIQUE (session_id, athlete_id) en attendance (índice único parcial para compatibilidad con soft delete)
- [x] 1.4 Agregar comentarios documentales (`COMMENT ON`) siguiendo el patrón de V2

## 2. Entidades y repositorios

- [x] 2.1 Crear enum `SessionStatus` (PROGRAMADA, EJECUTADA, CANCELADA) y enum `AttendanceStatus` (PRESENTE, AUSENTE, JUSTIFICADO) y enum `CycleType` (MESOCICLO, MICROCICLO)
- [x] 2.2 Crear entidad `TrainingPlan` extends BaseEntity con `@SQLDelete`/`@SQLRestriction`
- [x] 2.3 Crear entidad `TrainingCycle` extends BaseEntity con autorelación a padre y relación a plan
- [x] 2.4 Crear entidad `TrainingSession` extends BaseEntity con relación a ciclo y disciplina
- [x] 2.5 Crear entidad `Attendance` extends BaseEntity con relación a sesión y deportista + unique constraint
- [x] 2.6 Crear repositorios: `TrainingPlanRepository`, `TrainingCycleRepository`, `TrainingSessionRepository`, `AttendanceRepository` (consultas indexadas: por ciclo ordenadas por fecha, por deportista, conteo de ausencias consecutivas)

## 3. Servicios

- [x] 3.1 Crear `TrainingPlanService`: CRUD de planes + validación de fechas y agregado de ciclos (jerarquía, fechas dentro del plan, microciclo solo bajo mesociclo)
- [x] 3.2 Crear `TrainingSessionService`: registro de sesiones, listado por ciclo, cambio de estado, update/delete con validaciones de rango (volumen 0-100000, intensidad 0-100)
- [x] 3.3 Crear `AttendanceService`: registro/upsert de asistencia (unique por sesión+deportista, rechazo en sesión CANCELADA), historial por deportista, listado por sesión
- [x] 3.4 Crear `AlertService` con `Clock` inyectado y umbral configurable (`@ConfigurationProperties` con default 3): detección de N ausencias consecutivas, consulta de alertas activas, limpieza por acknowledge
- [x] 3.5 Crear bean `Clock` y propiedades de configuración `training.attendance.absence-threshold` en `application.properties`

## 4. DTOs y controllers

- [x] 4.1 Crear DTOs request/response en `dto/` (records) para planes, ciclos, sesiones, asistencia y alertas — sin exponer entidades JPA
- [x] 4.2 Crear `TrainingPlanController` (CRUD planes + ciclos) bajo `/api/v1/training-plans`
- [x] 4.3 Crear `TrainingSessionController` (sesiones por ciclo, cambio de estado, asistencia) bajo `/api/v1/training-sessions` y `/api/v1/training-cycles/{id}/sessions`
- [x] 4.4 Crear `AttendanceController` (asistencia por sesión, historial por deportista) y `AlertController` (alertas activas, acknowledge)
- [x] 4.5 Actualizar `SecurityConfig` para proteger los nuevos endpoints (ADMIN/COACH)

## 5. Tests

- [x] 5.1 Tests unitarios `TrainingPlanServiceTest` (CRUD, jerarquía, validación fechas)
- [x] 5.2 Tests unitarios `TrainingSessionServiceTest` (registro, rangos, cambio de estado, ciclos no existentes)
- [x] 5.3 Tests unitarios `AttendanceServiceTest` (upsert, rechazo en sesión cancelada, historial)
- [x] 5.4 Tests unitarios `AlertServiceTest` con `Clock` fijo (umbral, racha rota por PRESENTE, sin alertas)
- [x] 5.5 Verificar `./mvnw compile` y ejecutar tests del módulo (54 tests en verde). Nota: la validación end-to-end contra PostgreSQL local no se ejecutó por entorno sin Docker/PostgreSQL; pendiente de CI o entorno con DB.

## 6. Documentación y cierre

- [x] 6.1 Actualizar TASKS.md (módulo Training completo) y CHANGELOG.md — TASKS.md actualizado; CHANGELOG.md no existe en el repo (no se creó por política de no generar docs sin solicitud)
- [ ] 6.2 Revisión final con quality-guardian (cobertura >80%, convenciones, sin entidades en responses)
