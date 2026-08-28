## ADDED Requirements

### Requirement: National reference times are keyed by style, distance, category and position
The system MUST store one national reference time per tuple (style, distance, category, position), where position is 1 (gold), 2 (silver) or 3 (bronze). Uniqueness applies only to active reference times.

#### Scenario: Create reference times for a (style, distance, category) triple
- **WHEN** an administrator creates reference times for (style="LIBRE", distance=50, category="infantil") with positions 1, 2 and 3
- **THEN** system persists three rows differentiated only by position with their respective seconds values

#### Scenario: Reject duplicate (style, distance, category, position)
- **WHEN** a user creates a reference time for a tuple already present as active
- **THEN** system rejects the request with a 409 Conflict error

#### Scenario: Position must be 1, 2 or 3
- **WHEN** a user creates a reference time with position 4
- **THEN** system rejects the request with a 400 Bad Request validation error

### Requirement: National reference times CRUD is restricted to administrators
The system MUST restrict create/update/soft-delete of national reference times to the ADMIN role. READ access is available to other operational roles.

#### Scenario: Administrator can create reference times
- **WHEN** a user authenticated with role ADMIN posts a new reference time
- **THEN** system accepts the request and persist the row

#### Scenario: Non-admin user cannot create reference times
- **WHEN** a user authenticated with a non-ADMIN role posts a new reference time
- **THEN** system returns 403 Forbidden

#### Scenario: Non-admin user can read reference times
- **WHEN** a user authenticated with a non-ADMIN role queries reference times
- **THEN** system returns the list of active reference times

### Requirement: National reference times are stored in seconds and presented as `mm:ss.ms`
The system MUST store national reference times in seconds (decimal, millisecond precision) and present them in `mm:ss.ms` format in response DTOs, using the same formatter as checkup times.

#### Scenario: Reference time stored in seconds, displayed in mm:ss.ms
- **WHEN** a reference time of 70.000 seconds is rendered in a response DTO
- **THEN** DTO presents it as `01:10.000`

### Requirement: Deletion of a national reference time is logical
The system MUST soft delete national reference times (set `deleted_at`) and exclude soft-deleted rows from queries via `@SQLRestriction`.

#### Scenario: Delete a national reference time excludes it from queries
- **WHEN** an administrator soft-deletes a national reference time
- **THEN** subsequent queries do not list it, and uniqueness no longer considers it
