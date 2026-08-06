## ADDED Requirements

### Requirement: System allows registering a training session
The system MUST allow associating a training session to a cycle with date, time, status (PROGRAMADA, EJECUTADA, CANCELADA), volume, intensity, distance, sport/discipline and observations.

#### Scenario: Successful session registration
- **WHEN** user submits a session with valid data (date, time, status, volume, intensity)
- **THEN** system creates the session and returns it

#### Scenario: Session on non-existent cycle
- **WHEN** user registers a session on a non-existent cycle ID
- **THEN** system returns a 404 Not Found error

#### Scenario: Invalid status value
- **WHEN** user submits a session with a status not in {PROGRAMADA, EJECUTADA, CANCELADA}
- **THEN** system rejects the registration with a validation error

#### Scenario: Volume outside reasonable range
- **WHEN** user submits a session with volume outside the validated range (0-100000)
- **THEN** system rejects the registration with a validation error

#### Scenario: Intensity outside reasonable range
- **WHEN** user submits a session with intensity outside 0-100 percent
- **THEN** system rejects the registration with a validation error

#### Scenario: Missing required fields
- **WHEN** user submits a session without date or cycle
- **THEN** system rejects the registration with validation errors

### Requirement: System allows querying sessions by cycle
The system MUST allow retrieving the list of sessions belonging to a cycle.

#### Scenario: List sessions of existing cycle
- **WHEN** user requests sessions for an existing cycle ID
- **THEN** system returns the list of sessions ordered by date

#### Scenario: Cycle not found
- **WHEN** user requests sessions for a non-existent cycle ID
- **THEN** system returns a 404 Not Found error

#### Scenario: Cycle with no sessions
- **WHEN** user requests sessions for a cycle without sessions
- **THEN** system returns an empty list

### Requirement: System allows retrieving a specific session
The system MUST return the full detail of a session including volume, intensity, distance, status and athlete attendance.

#### Scenario: Successful session retrieval
- **WHEN** user requests an existing session by ID
- **THEN** system returns the session detail

#### Scenario: Session not found
- **WHEN** user requests a non-existent session ID
- **THEN** system returns a 404 Not Found error

### Requirement: System allows changing session status
The system MUST allow transitioning a session status between PROGRAMADA, EJECUTADA and CANCELADA.

#### Scenario: Successfully marking session as executed
- **WHEN** user marks a PROGRAMADA session as EJECUTADA
- **THEN** system updates the status and returns the updated session

#### Scenario: Change status of non-existent session
- **WHEN** user changes the status of a non-existent session ID
- **THEN** system returns a 404 Not Found error

### Requirement: System allows updating a training session
The system MUST allow updating session data (date, volume, intensity, observations, etc.).

#### Scenario: Successful session update
- **WHEN** user updates an existing session with valid data
- **THEN** system updates the session and returns it

#### Scenario: Update non-existent session
- **WHEN** user tries to update a non-existent session ID
- **THEN** system returns a 404 Not Found error

### Requirement: System allows deleting a training session
The system MUST allow soft deleting a training session.

#### Scenario: Successful session deletion
- **WHEN** administrator deletes an existing session
- **THEN** system marks the session as deleted and returns deletion confirmation

#### Scenario: Delete non-existent session
- **WHEN** administrator tries to delete a non-existent session ID
- **THEN** system returns a 404 Not Found error