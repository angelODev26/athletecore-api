## ADDED Requirements

### Requirement: System stores checkups grouped by athlete and month
The system MUST allow registering a monthly checkup that groups trial times for an athlete, with uniqueness constraint of one active checkup per (athlete_id, year, month, category) tuple. Soft-deleted checkups do not collide with new ones.

#### Scenario: Create a new monthly checkup for an athlete
- **WHEN** a user creates a checkup for (athlete=10, year=2026, month=8, category="mayor")
- **THEN** system persists the checkup and returns its identifier and audit timestamps

#### Scenario: Reject duplicate active checkup for same athlete/year/month/category
- **WHEN** a user creates a checkup for (athlete=10, year=2026, month=8, category="mayor") and an active checkup already exists for that tuple
- **THEN** system rejects the request with a 409 Conflict error

#### Scenario: Soft-deleted checkup does not block a new one
- **WHEN** a previous checkup for (athlete=10, year=2026, month=8, category="mayor") was soft-deleted and a user creates a new checkup for the same tuple
- **THEN** system persists the new checkup (deleted_at of the previous one excludes it from uniqueness)

#### Scenario: Unknown athlete cannot have a checkup
- **WHEN** a user creates a checkup for an athlete_id that does not exist or is soft-deleted
- **THEN** system rejects the request with a 404 Not Found error

### Requirement: Checkup groups trial times by style and distance
The system MUST store one `CheckupTime` per checkup, style and distance, with uniqueness per (checkup_id, style, distance). Each time is stored in seconds with millisecond precision.

#### Scenario: Add a trial time to a checkup
- **WHEN** a user adds a time of 65.250 seconds for (style="LIBRE", distance=100) within a checkup
- **THEN** system persists the row with the numeric value `65.250`

#### Scenario: Reject duplicate (style, distance) within a checkup
- **WHEN** a user adds a time for a (style, distance) pair already present in the same checkup
- **THEN** system rejects the request with a 409 Conflict error

#### Scenario: Time stored with millisecond precision
- **WHEN** a trial time carries milliseconds (e.g., 1:05.250)
- **THEN** system stores it as a decimal value with at least 3 fractional digits and preserves them on read

### Requirement: Trial times are presented as `mm:ss.ms` and stored as seconds
The system MUST store trial times in seconds (decimal) and present them exclusively in `mm:ss.ms` format at the DTO boundary. The entity layer MUST NOT perform formatting.

#### Scenario: Entity stores seconds, DTO presents mm:ss.ms
- **WHEN** a checkup time of 65.250 seconds is rendered in any response DTO
- **THEN** DTO presents it as `01:05.250` and the entity persists it as `65.250`

#### Scenario: Formatter handles minutes rollover
- **WHEN** a seconds value is 125.755
- **THEN** the formatter renders `02:05.755`

#### Scenario: Formatter rejects invalid input
- **WHEN** a negative or null seconds value is passed to the formatter
- **THEN** the formatter returns an error or null representation (no negative mm:ss.ms strings)

### Requirement: Delete of a checkup is logical
The system MUST soft delete checkups (set `deleted_at`) rather than physically deleting rows, and soft-deleted checkups MUST be excluded from all queries via `@SQLRestriction`.

#### Scenario: Delete a checkup sets deleted_at
- **WHEN** an administrator deletes a checkup
- **THEN** system sets `deleted_at` to current timestamp and keeps the row, excluding it from default queries

#### Scenario: Deleted checkup is invisible in listings
- **WHEN** a user lists checkups after one was soft-deleted
- **THEN** the soft-deleted checkup is not present in the result
