## Why

El proyecto AthleteCore API tiene una base limpia con User/Role implementados (Fase 0 completada), pero falta implementar los módulos de negocio principales. El módulo de deportistas (athlete) es el primero que debe implementarse porque:

- Es el entity central del dominio (atletas, sus perfiles, deportes)
- Todos los demás módulos (entrenamientos, chequeos, reportes) dependen de datos de deportistas
- Permite validar la arquitectura completa antes de otros módulos

## What Changes

### Nuevas Funcionalidades:
- CRUD completo de deportistas (creación, lectura, actualización, eliminación)
- Gestión de perfil antropométrico (peso, talla, envergadura, IMC)
- Registro de deportes/disciplinas asociadas a deportistas
- Upload de foto de perfil (referencia URL)
- Soft delete para deportistas

### Modificaciones:
- Migración Flyway V2__athletes_schema.sql (nueva)
- Entidades: Athlete, AthleteProfile, Sport, Discipline
- Servicios: AthleteRegistrationService, AthleteProfileService, AthleteReportingService
- Controllers: AthleteController
- DTOs para todas las operaciones

### BREAKING:
- No breaking changes en API existente (solo se agrega nueva funcionalidad)

## Capabilities

### New Capabilities
- **athlete-crud**: Registro, consulta, actualización y eliminación de deportistas
- **athlete-profile**: Gestión de perfil antropométrico (peso, talla, envergadura, IMC automático)
- **athlete-sports**: Gestión de deportes/disciplinas asociadas a deportistas
- **athlete-photo**: Upload de foto de perfil (referencia URL, no binario)
- **athlete-soft-delete**: Eliminación lógica con soft delete (heredado de BaseEntity)

### Modified Capabilities
- Ninguno (módulo nuevo sin afectar capabilities existentes)

## Impact

### Archivos Nuevos:
- **Migraciones**: `src/main/resources/db/migration/V2__athletes_schema.sql`
- **Entidades**:
  - `src/main/java/com/athletecore/api/athlete/Athlete.java`
  - `src/main/java/com/athletecore/api/athlete/AthleteProfile.java`
  - `src/main/java/com/athletecore/api/athlete/Sport.java`
  - `src/main/java/com/athletecore/api/athlete/Discipline.java`
- **Servicios**:
  - `src/main/java/com/athletecore/api/athlete/AthleteRegistrationService.java`
  - `src/main/java/com/athletecore/api/athlete/AthleteProfileService.java`
  - `src/main/java/com/athletecore/api/athlete/AthleteReportingService.java`
- **Controllers**:
  - `src/main/java/com/athletecore/api/athlete/AthleteController.java`
- **DTOs**:
  - `src/main/java/com/athletecore/api/athlete/dto/CreateAthleteRequest.java`
  - `src/main/java/com/athletecore/api/athlete/dto/AthleteResponse.java`
  - `src/main/java/com/athletecore/api/athlete/dto/AthleteProfileRequest.java`
  - `src/main/java/com/athletecore/api/athlete/dto/AthleteProfileResponse.java`
  - `src/main/java/com/athletecore/api/athlete/dto/SportRequest.java`
  - `src/main/java/com/athletecore/api/athlete/dto/SportResponse.java`
- **Tests**: `src/test/java/com/athletecore/api/athlete/`

### API Endpoints Nuevos:
```
POST   /api/v1/athletes                    # Registrar deportista
GET    /api/v1/athletes                    # Listar deportistas
GET    /api/v1/athletes/{id}               # Obtener deportista
PUT    /api/v1/athletes/{id}               # Actualizar deportista
DELETE /api/v1/athletes/{id}               # Eliminar deportista (soft delete)
POST   /api/v1/athletes/{id}/profile       # Actualizar perfil antropométrico
GET    /api/v1/athletes/{id}/profile       # Obtener perfil
POST   /api/v1/athletes/{id}/sports        # Asignar deportes
GET    /api/v1/athletes/{id}/sports        # Listar deportes
```

### Dependencias:
- Aprovecha `BaseEntity` para auditoría y soft delete
- Valida con Jakarta Validation
- Usa `User` para relaciones (deportista puede tener coach/trainers asignados en futuro)
- Integra con `SecurityConfig` para protección de endpoints

### Consideraciones:
- **IMC calculado automáticamente**: Método puro sin dependencias externas
- **Fotos**: Solo URL referencial, no binarios en DB
- **Deportes genéricos**: Modelo extensible para futuros deportes (no solo natación)
- **Índices**: En nombre, documento, fecha de nacimiento para búsquedas frecuentes
