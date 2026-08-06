## Context

AthleteCore API tiene la Fase 0 (base limpia: User/Role, JWT, Flyway) y el módulo de deportistas (V2, CRUD completo) terminados. El siguiente módulo de negocio es Entrenamientos: planificación anual con jerarquía de ciclos (Plan → Mesociclo → Microciclo → Sesión), registro de sesiones y control de asistencia con alertas por ausencias consecutivas.

Restricciones vigentes:
- Spring Boot 3.5.6, Java 21, PostgreSQL 15, JPA/Hibernate con `ddl-auto=validate`, Flyway versionado.
- Todas las entidades extienden `BaseEntity` (auditoría + soft delete con `@SQLDelete`/`@SQLRestriction` por entidad concreta).
- Respuestas HTTP solo con DTOs (records), nunca entidades JPA.
- Servicios de propósito único (sin god services), validación Jakarta, excepciones centralizadas en `common/exception` + `GlobalExceptionHandler`.
- `athletes`, `sports`, `disciplines` ya existen (V2) y serán referenciados por el módulo.
- Documentación, comentarios y tests en español; conventional commits.

## Goals / Non-Goals

**Goals:**
- Esquema V3 con `training_plans`, `training_cycles`, `training_sessions`, `attendance` (más tablas de soporte de alertas si se requieren).
- Entidades `TrainingPlan`, `TrainingCycle`, `TrainingSession`, `Attendance` extendiendo `BaseEntity`.
- Jerarquía de ciclos con autorelación (mesociclo → microciclo) y validación de fechas dentro del rango padre.
- Registro de sesiones con estado (enum), volumen, intensidad, distancia, disciplina y observaciones.
- Asistencia por (sesión, deportista) con upsert y estados PRESENTE/AUSENTE/JUSTIFICADO.
- Alertas por N ausencias consecutivas con umbral configurable y `Clock` inyectado (determinista).
- CRUD REST bajo `/api/v1/training-*` protegido con JWT y roles ADMIN/COACH.
- Tests unitarios (JUnit 5 + Mockito, sin Spring context) con cobertura >80% en lógica de negocio.

**Non-Goals:**
- Integración con el módulo de chequeos/reportes (módulos futuros V4/V5).
- Notificaciones reales (email/SMS) — las alertas se exponen como consulta REST; el envío se deja a infraestructura futura.
- Cálculo de carga de entrenamiento avanzado (TSS, TRIMP, etc.).
- Autenticación/autorización por deportista (los endpoints son de staff: ADMIN/COACH).
- Paginación avanzada de listados (se puede agregar luego como mejora de infraestructura).

## Decisions

### D1. Jerarquía de ciclos con autorelación `parent_cycle_id`
`training_cycles` tendrá `plan_id` (obligatorio), `type` enum (MESOCICLO/MICROCICLO), `parent_cycle_id` nullable (solo microciclos apuntan a un mesociclo) y `order_index`.
- **Por qué**: Hibernate mapea la autorelación con `@ManyToOne` simple; permite profundidad arbitraria sin tablas intermedias; validar tipo padre/hijo (microciclo solo bajo mesociclo) en el servicio.
- **Alternativa considerada**: Tablas separadas `mesocycles`/`microcycles` — rechazada: duplica estructura y dificulta consultas transversales de la jerarquía.

### D2. Sesión con `cycle_id` opcional y referencia a disciplina existente
`training_sessions` referencia `cycle_id` (nullable: una sesión puede existir sin ciclo) y `discipline_id` (opcional, FK a `disciplines` de V2). Estado como enum `SessionStatus { PROGRAMADA, EJECUTADA, CANCELADA }` persistido como string.
- **Por qué**: Reutiliza el catálogo de disciplinas ya existente (extensibilidad); enum finito en código con validación Jakarta.
- **Alternativa**: Guardar estilo/disciplina como texto libre — rechazada: pierde integridad referencial y consistencia con el módulo de deportistas.

### D3. `attendance` con constraint único (session_id, athlete_id) y upsert
Upsert a nivel de repositorio: si el registro existe, se actualiza el estado; si no, se inserta.
- **Por qué**: El spec exige "una fila por (sesión, deportista); re-registro actualiza". El constraint único a nivel DB protege contra duplicados por concurrencia.
- **Alternativa**: Permitir múltiples filas — rechazada: rompe el invariante del dominio y complica el cálculo de ausencias.

### D4. Alertas de ausencias como consulta derivada (sin tabla física)
`AlertService` calcula sobre la marcha los deportistas con N+ ausencias consecutivas en sus sesiones más recientes, ordenadas por fecha de sesión. El umbral `training.attendance.absence-threshold` (default 3) vive en `application.properties` con `@ConfigurationProperties`.
- **Por qué**: El dato es derivado del historial; persistir alertas crearía estado inconsistente con la asistencia. Cálculo en memoria sobre consulta indexada es barato para el volumen esperado.
- **Alternativa**: Tabla `attendance_alerts` persistida — rechazada por ahora (riesgo de desincronización); se puede evaluar con volúmenes mayores o notificaciones reales.
- **Nota**: "limpiar alerta" se implementa como *acknowledgement* — ver Riesgos (R2) y Open Questions.

### D5. `Clock` inyectado en servicios de alertas y reglas de fecha
`AlertService` y validaciones de fechas usan `java.time.Clock` inyectado vía constructor (bean `Clock.systemDefaultZone()` en config). Ningún `now()` directo en lógica de negocio.
- **Por qué**: El agente training-domain exige lógica determinista y testeable.
- **Alternativa**: `LocalDateTime.now()` estático — rechazada: no testeable.

### D6. Consultas indexadas para listados frecuentes
Índices parciales (`WHERE deleted_at IS NULL`) en: `training_sessions(cycle_id, session_date)`, `attendance(session_id)`, `attendance(athlete_id, session_date join)`, `training_cycles(plan_id, parent_cycle_id)`.
- **Por qué**: Listado de sesiones por ciclo y asistencia por deportista son las consultas más frecuentes (patrón de V2).
- **Alternativa**: Sin índices — rechazada: degradación con volumen de datos.

### D7. Seguridad por roles ADMIN/COACH
- `ADMIN`: CRUD completo de planes/ciclos/sesiones, borrado, alertas.
- `COACH`: crear/actualizar sesiones, registrar asistencia, ver alertas.
- Protección por configuración en `SecurityConfig` (permitAll solo `POST /api/v1/users`) más `@PreAuthorize` por endpoint donde aplique.
- **Por qué**: Consistente con la política actual del proyecto (estática, sin expiración de sesión por token).

## Risks / Trade-offs

- **[Volumen de asistencia por upsert]** → Mitigación: constraint único + consultas por índice; el upsert se implementa con consulta previa y `save` (sin `ON CONFLICT` nativo para mantener compatibilidad JPA; evaluar `INSERT ... ON CONFLICT` nativo si hay presión de concurrencia).
- **[Alertas derivadas en memoria con muchos datos]** → Mitigación: consulta agregada por deportista y sesión, iteración única; si el volumen crece, migrar a tabla materializada (Open Question).
- **[Cálculo de ausencias "consecutivas" en historial incompleto]** → Mitigación: las ausencias consecutivas se cuentan sobre las sesiones recientes del deportista (fecha <= hoy con Clock); sesiones futuras no cuentan. Documentar el criterio en tests.
- **[Autorelación de ciclos puede crear ciclos inválidos]** → Mitigación: validación en servicio (microciclo solo hijo de mesociclo, fechas dentro del padre, no auto-referencia).
- **[Soft delete en ciclo con sesiones hijas]** → Mitigación: borrado lógico; las sesiones conservan `cycle_id` pero la jerarquía no las lista (las consultas de ciclo excluyen eliminados).

## Migration Plan

1. Crear rama `feature/training-domain` (ya creada).
2. Escribir `V3__training_schema.sql` con las 4 tablas + índices + comentarios documentales (patrón V2).
3. Implementar entidades, repositorios, servicios, controllers y DTOs.
4. Actualizar `SecurityConfig` para los nuevos endpoints.
5. Tests unitarios por servicio (cobertura >80%).
6. Verificar: `./mvnw compile` y `./mvnw test -Dtest=*Training*` (PostgreSQL local o docker-compose).
7. Merge a `develop` tras revisión; archivar el cambio OpenSpec.

Rollback: Flyway (`flyway.clean` no permitido en prod; en dev, `docker-compose --profile migrate up flyway` para revertir, o migración V3 reversible por ser DDL aditivo — no destruye datos existentes de V1/V2).

## Open Questions

- ¿`COACH` necesita crear planes/ciclos o solo sesiones y asistencia? (El agente puede decidir con el rol actual; por defecto: COACH edita sesiones/asistencia, ADMIN gestiona planes y alertas.)
- ¿Se requiere endpoint de "limpiar alerta" persistente o basta con que la asistencia PRESENTE rompa la racha? (Spec actual contempla ambas; se implementa la consulta + limpieza por ack si es simple.)
- ¿Umbral de ausencias por deporte o global? (Por defecto global vía configuración.)
