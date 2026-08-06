## ADDED Requirements

### Requirement: System detects consecutive absences
The system MUST detect athletes with N consecutive AUSENTE attendance records, where N is a configurable threshold.

#### Scenario: Athlete reaches consecutive absence threshold
- **WHEN** an athlete accumulates N consecutive absences in their most recent sessions
- **THEN** system flags the athlete as having a consecutive absence alert

#### Scenario: Athlete below the threshold
- **WHEN** an athlete has fewer than N consecutive absences
- **THEN** system does not flag the athlete

#### Scenario: Attendance breaks the absence streak
- **WHEN** an athlete with consecutive absences registers a PRESENTE attendance
- **THEN** system clears the consecutive absence streak for that athlete

### Requirement: System allows querying active alerts
The system MUST expose the list of athletes currently flagged by consecutive absence alerts, with the count of consecutive absences.

#### Scenario: Query alerts with flagged athletes
- **WHEN** user requests the alert list and there are flagged athletes
- **THEN** system returns each athlete with their consecutive absence count

#### Scenario: No flagged athletes
- **WHEN** user requests the alert list and no athlete is flagged
- **THEN** system returns an empty list

### Requirement: System allows clearing an alert
The system MUST allow an administrator to acknowledge and clear an athlete's alert.

#### Scenario: Clearing an existing alert
- **WHEN** administrator acknowledges an athlete's alert
- **THEN** system removes the alert for that athlete and returns confirmation

#### Scenario: Clearing an alert for athlete without alerts
- **WHEN** administrator acknowledges an athlete with no active alert
- **THEN** system returns a 404 Not Found error

### Requirement: Alert logic must be deterministic and configurable
The consecutive absence threshold MUST be configurable (not hardcoded), and the detection logic MUST be time-independent (injectable clock).

#### Scenario: Threshold configurable at runtime
- **WHEN** the threshold N is changed in configuration
- **THEN** detection uses the new threshold without code changes

#### Scenario: Detection does not depend on system clock
- **WHEN** tests exercise the detection logic with a fixed clock
- **THEN** results are identical regardless of the actual current time