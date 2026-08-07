## ADDED Requirements

### Requirement: System assigns a deterministic podium classification per (style, distance, category)
The system MUST assign exactly one classification to a trial time: **Por encima del podio** (faster than or within the configurable threshold of bronze), **Cercano a medallería** (within a configurable threshold beyond bronze), or **Fuera de rango** (beyond the proximity threshold). Classification is deterministic from numeric inputs and the configured threshold.

#### Scenario: Trial time faster than bronze → Por encima del podio
- **WHEN** a trial time of 64.000s is compared against bronze=65.000s with proximity threshold 1.500s
- **THEN** the classification is "Por encima del podio" (difference against bronze = -1.000)

#### Scenario: Trial time within proximity threshold of bronze → Cercano a medallería
- **WHEN** a trial time of 65.800s is compared against bronze=65.000s with proximity threshold 1.500s
- **THEN** the classification is "Cercano a medallería" (difference against bronze = +0.800)

#### Scenario: Trial time beyond proximity threshold of bronze → Fuera de rango
- **WHEN** a trial time of 67.000s is compared against bronze=65.000s with proximity threshold 1.500s
- **THEN** the classification is "Fuera de rango" (difference against bronze = +2.000)

### Requirement: Proximity threshold is configurable at runtime
The system MUST read the "Cercano a medallería" threshold from application configuration (e.g., `checkup.medal-proximity-threshold-seconds`) rather than hardcoding it. The threshold applies as an absolute delta in seconds against the bronze reference time.

#### Scenario: Threshold configurable via properties
- **WHEN** the property `checkup.medal-proximity-threshold-seconds` is set to 2.000
- **THEN** "Cercano a medallería" applies to trials whose difference against bronze is within (0, +2.000]

#### Scenario: Threshold change does not require code edits
- **WHEN** the threshold changes at deployment time via environment variable
- **THEN** the next classification request uses the new threshold without recompilation

### Requirement: Medal projection aggregates classifications per athlete
The system MUST produce, per athlete, the projected classification for each (style, distance) evaluated, with the difference against the bronze reference for each.

#### Scenario: Projection lists per (style, distance) classification
- **WHEN** an administrator requests the projection for athlete 10 with 3 registered trial times (one per (style, distance))
- **THEN** the response contains three entries, each with (style, distance, category, classification, difference vs bronze)

#### Scenario: Athlete without checkups has empty projection
- **WHEN** an administrator requests the projection for an athlete with no active checkups
- **THEN** the response is an empty list, not an error

### Requirement: Projection does not mutate state
The system MUST compute medal projections without persisting any new rows or mutating `MedalProjection` rows as a side effect. Projections are a read operation.

#### Scenario: Projection is read-only
- **WHEN** a medal projection is computed for an athlete
- **THEN** no new `MedalProjection` rows are persisted and no existing rows are mutated during that request

### Requirement: Medal projection completes within performance budget for a team
The system MUST compute projections for up to 50 athletes within 500 ms (requirement from checkup-domain agent).

#### Scenario: Team projection under 500 ms
- **WHEN** projections are requested for 50 athletes with up to 10 trial times each
- **THEN** the operation completes in under 500 ms on the reference hardware
