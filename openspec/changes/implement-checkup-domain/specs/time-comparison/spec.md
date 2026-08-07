## ADDED Requirements

### Requirement: System compares a trial time against the three national reference positions
The system MUST compare a registered trial time against the 1°, 2° and 3° reference times for the same (style, distance, category) and return the absolute and relative difference for each position.

#### Scenario: Trial time faster than gold position
- **WHEN** a trial time of 60.000s is compared against references (1°=62.000, 2°=63.500, 3°=65.000) for the same style/distance/category
- **THEN** system returns absolute differences (-2.000, -3.500, -5.000) where negative means faster than reference

#### Scenario: Trial time between two reference positions
- **WHEN** a trial time of 64.000s is compared against references (1°=62.000, 2°=63.500, 3°=65.000)
- **THEN** system returns differences (+2.000, +0.500, -1.000)

#### Scenario: Trial time slower than bronze position
- **WHEN** a trial time of 70.000s is compared against references (1°=62.000, 2°=63.500, 3°=65.000)
- **THEN** system returns differences (+8.000, +6.500, +5.000) and signals "Fuera de rango" candidate

### Requirement: Comparison requires an exact reference for the trial's style/distance/category
The system MUST reject a comparison when there is no active national reference time triple for the requested (style, distance, category).

#### Scenario: Missing reference triple
- **WHEN** a comparison is requested for a (style, distance, category) with no complete triple of active reference times
- **THEN** system rejects the request with a 409 Conflict error indicating missing reference data

#### Scenario: Partial triple (fewer than 3 positions)
- **WHEN** only positions 1 and 2 exist for a (style, distance, category) and comparison is requested
- **THEN** system rejects the request with a 409 Conflict error

### Requirement: Comparison completes within performance budget
The system MUST return comparison results for a single checkup in under 500 ms (requirement from checkup-domain agent). Comparisons are synchronous and index-backed.

#### Scenario: Single checkup comparison under 500 ms
- **WHEN** the system processes a comparison for one checkup with N trial times (N <= 10) against reference triples
- **THEN** the operation completes in under 500 ms on the reference hardware

### Requirement: Comparison results are deterministic
The system MUST compute comparisons from explicit numeric inputs only; any time-dependent behavior MUST be injected via `Clock` and the logic MUST yield identical results for identical inputs.

#### Scenario: Identical inputs yield identical outputs
- **WHEN** the same checkup and reference data are compared with a fixed clock
- **THEN** the computed differences and classifications are identical across runs
