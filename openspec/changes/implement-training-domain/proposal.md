## Why

El módulo de deportistas (v0.2.0) ya está completo, pero AthleteCore API aún no puede gestionar la planificación del entrenamiento ni controlar la asistencia. El módulo de entrenamientos es el siguiente paso porque materializa la operación diaria del gimnasio/piscina: planificar el ciclo anual, registrar sesiones y detectar ausencias consecutivas que requieren alerta al equipo técnico.

## What Changes

### Nuevas Funcionalidades:
- Plan anual de entrenamiento con jerarquía de ciclos: Plan → Mesociclo → Microciclo → Sesión
- Registro de sesiones de entrenamiento con fecha, hora, estado (programada/ejecutada/cancelada), volumen, intensidad, distancia y observaciones
- Control de asistencia por deportista y sesión (presente, ausente, justificado)
- Alerta automática ante N ausencias consecutivas por deportista (N configurable)
- Soft delete para todas las entidades del módulo

### Modificaciones:
- Migración Flyway V3__training_schema.sql (nueva)
- Entidades: TrainingPlan, TrainingCycle, TrainingSession, Attendance (todas extienden BaseEntity)
- Servicios: TrainingPlanService, TrainingSessionService, AttendanceService, AlertService
- Controllers: TrainingPlanController, TrainingSessionController, AttendanceController, AlertController
- DTOs para todas las operaciones (request/response, sin entidades JPA)
- Seguridad: endpoints protegidos por rol ADMIN/COACH según sensibilidad

### BREAKING:
- No hay breaking changes en la API existente (solo se agrega funcionalidad nueva)

## Capabilities

### New Capabilities
- **training-plan**: Gestión de planes anuales y jerarquía de ciclos (mesociclo/microciclo)
- **training-session**: Registro y gestión de sesiones de entrenamiento (estado, volumen, intensidad)
- **attendance**: Control de asistencia por deportista y sesión (presente/ausente/justificado)
- **attendance-alerts**: Detección y notificación de ausencias consecutivas configurables

### Modified Capabilities
- Ninguno (módulo nuevo sin afectar capabilities existentes)

## Impact

### Archivos Nuevos:
- **Migraciones**: `src/main/resources/db/migration/V3__training_schema.sql`
- **Entidades**:
  - `src/main/java/com/athletecore/api/training/TrainingPlan.java`
  - `src/main/java/com/athletecore/api/training/TrainingCycle.java`
  - `src/main/java/com/athletecore/api/training/TrainingSession.java`
  - `src/main/java/com/athletecore/api/training/Attendance.java`
- **Servicios**:
  - `src/main/java/com/athletecore/api/training/TrainingPlanService.java`
  - `src/main/java/com/athletecore/api/training/TrainingSessionService.java`
  - `src/main/java/com/athletecore/api/training/AttendanceService.java`
  - `src/main/java/com/athletecore/api/training/AlertService.java`
- **Controllers**: `TrainingPlanController`, `TrainingSessionController`, `AttendanceController`, `AlertController`
- **DTOs**: `src/main/java/com/athletecore/api/training/dto/` (request/response por operación)
- **Tests**: `src/test/java/com/athletecore/api/training/`

### API Endpoints Nuevos (propuesta):
```
POST   /api/v1/training-plans                  # Crear plan anual
GET    /api/v1/training-plans                  # Listar planes
GET    /api/v1/training-plans/{id}             # Obtener plan con jerarquía
PUT    /api/v1/training-plans/{id}             # Actualizar plan
DELETE /api/v1/training-plans/{id}             # Eliminar plan (soft delete)
POST   /api/v1/training-plans/{planId}/cycles  # Crear mesociclo/microciclo
POST   /api/v1/training-cycles/{cycleId}/sessions  # Crear sesión
GET    /api/v1/training-cycles/{cycleId}/sessions  # Listar sesiones por ciclo
PUT    /api/v1/training-sessions/{id}/status   # Cambiar estado sesión
POST   /api/v1/training-sessions/{sessionId}/attendance   # Registrar asistencia
GET    /api/v1/training-sessions/{sessionId}/attendance   # Listar asistencia
GET    /api/v1/athletes/{athleteId}/attendance # Historial asistencia por deportista
GET    /api/v1/alerts/attendance               # Listar alertas activas
```

### Dependencias:
- Aprovecha `BaseEntity` para auditoría y soft delete
- Relaciona sesiones/asistencia con `Athlete` (módulo existente) y ciclos con `TrainingPlan`
- Valida con Jakarta Validation
- Lógica de alertas determinista mediante `Clock` inyectado (no `now()` directo)
- Índices para consultas frecuentes: sesiones por ciclo, asistencia por deportista/sesión

### Consideraciones:
- **Estado de sesión como enum**: PROGRAMADA, EJECUTADA, CANCELADA
- **Asistencia como enum**: PRESENTE, AUSENTE, JUSTIFICADO
- **Alertas configurables**: N de ausencias consecutivas por configuración (no hardcodeado)
- **Validación de rangos**: volumen e intensidad contra rangos razonables
- **Unicidad de asistencia**: una fila por (sesión, deportista), re-registro actualiza estado
