## ADDED Requirements

### Requirement: Individual report consolidates time evolution, attendance and medal projection per athlete
The system MUST produce, for a given athlete, a consolidated report containing: (a) the time evolution of trial times as a chronological series per (style, distance); (b) an attendance summary (total sessions and counts of PRESENTE/AUSENTE/JUSTIFICADO, plus the current consecutive-absence streak); and (c) the medal projection (classification and difference vs bronze per style+distance).

#### Scenario: Report for an athlete with checkups, attendance and projections
- **WHEN** a user requests the report for athlete 10 who has checkups across three months, attendance records and at least one active medal projection
- **THEN** the response contains the time-evolution series (ordered by year/month), the attendance summary and the medal projection entries

#### Scenario: Athlete without checkups has empty time series and projections
- **WHEN** a user requests the report for an athlete with no active checkups
- **THEN** the time series and projection sections are empty lists, and the report is not an error

#### Scenario: Unknown athlete produces 404
- **WHEN** a user requests the report for an athlete_id that does not exist or is soft-deleted
- **THEN** the system rejects the request with a 404 Not Found error

### Requirement: General report aggregates performance by style, distance and category across the team
The system MUST produce a general (team) report that groups the best trial times and medal classifications across athletes by (style, distance, category), enabling comparison across the team.

#### Scenario: General report groups by style, distance and category
- **WHEN** a user requests the general report for a category with several athletes and trial times
- **THEN** the response contains one aggregated entry per (style, distance, category) with the participating athletes and their projection classifications

#### Scenario: General report with no matching data is empty
- **WHEN** a user requests the general report for a category with no active checkups
- **THEN** the response is an empty list of aggregated entries, not an error

### Requirement: Reports must not mutate data of other modules
The system MUST assemble reports by reading through the public services of other modules and MUST NOT persist or mutate any rows belonging to athlete, training or checkup entities as a side effect of reporting.

#### Scenario: Report generation is read-only against source modules
- **WHEN** an individual or general report is generated
- **THEN** no `Checkup`, `CheckupTime`, `Attendance`, `Athlete` or `NationalReferenceTime` row is persisted or mutated during that request

### Requirement: Report completion is within performance budget
The system MUST assemble an individual or general report for up to 50 athletes within 500 ms.

#### Scenario: General report for 50 athletes under 500 ms
- **WHEN** a general report is requested for 50 athletes with up to 10 trial times each
- **THEN** the operation completes in under 500 ms on the reference hardware