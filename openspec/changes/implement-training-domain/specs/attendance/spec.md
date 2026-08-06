## ADDED Requirements

### Requirement: System allows registering attendance
The system MUST allow recording attendance per athlete per session with status PRESENTE, AUSENTE or JUSTIFICADO, ensuring one record per (session, athlete) pair.

#### Scenario: Successful attendance registration
- **WHEN** user registers an athlete as PRESENTE in an existing session
- **THEN** system creates the attendance record and returns it

#### Scenario: Registering attendance on non-existent session
- **WHEN** user registers attendance on a non-existent session ID
- **THEN** system returns a 404 Not Found error

#### Scenario: Registering attendance for non-existent athlete
- **WHEN** user registers attendance for a non-existent athlete ID
- **THEN** system returns a 404 Not Found error

#### Scenario: Duplicate registration updates the record
- **WHEN** user registers attendance for the same (session, athlete) pair again with a different status
- **THEN** system updates the existing record instead of creating a duplicate

#### Scenario: Invalid attendance status
- **WHEN** user submits an attendance status not in {PRESENTE, AUSENTE, JUSTIFICADO}
- **THEN** system rejects the registration with a validation error

### Requirement: System allows querying attendance by session
The system MUST return the attendance list of a session with the status of each athlete.

#### Scenario: List attendance of existing session
- **WHEN** user requests attendance for an existing session ID
- **THEN** system returns the attendance records for that session

#### Scenario: Session without attendance
- **WHEN** user requests attendance for a session with no records
- **THEN** system returns an empty list

### Requirement: System allows querying attendance history by athlete
The system MUST return the attendance history of an athlete across sessions.

#### Scenario: List attendance of existing athlete
- **WHEN** user requests the attendance history for an existing athlete ID
- **THEN** system returns the attendance records ordered by session date

#### Scenario: Athlete without attendance
- **WHEN** user requests history for an athlete with no attendance records
- **THEN** system returns an empty list

### Requirement: System must validate that attendance is only registered for planned sessions
Attendance MUST only be registered against sessions whose status is PROGRAMADA or EJECUTADA.

#### Scenario: Attendance on cancelled session
- **WHEN** user registers attendance on a CANCELADA session
- **THEN** system rejects the registration with a validation error

### Requirement: System must exclude soft-deleted attendance from queries
Soft-deleted attendance records MUST NOT appear in attendance queries.

#### Scenario: Deleted attendance excluded from list
- **WHEN** user queries attendance of a session
- **THEN** system returns only non-deleted attendance records