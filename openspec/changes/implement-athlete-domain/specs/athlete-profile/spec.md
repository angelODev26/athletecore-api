## ADDED Requirements

### Requirement: System allows managing athlete anthropometric profile
The system MUST allow administrators to manage anthropometric profile data for athletes.

#### Scenario: Successful profile creation
- **WHEN** user submits anthropometric data for an athlete
- **THEN** system creates a new profile record linked to the athlete

#### Scenario: Successful profile update
- **WHEN** user updates existing anthropometric data for an athlete
- **THEN** system updates the profile record and returns the updated profile

#### Scenario: Query athlete profile
- **WHEN** user requests an athlete's profile by athlete ID
- **THEN** system returns the profile data (weight, height, arm span, BMI)

#### Scenario: Profile not found
- **WHEN** user requests a profile for an athlete that has no profile
- **THEN** system returns a 404 Not Found error

### Requirement: System must calculate BMI automatically
The system MUST automatically calculate BMI based on weight and height.

#### Scenario: BMI calculation with valid data
- **WHEN** user provides weight and height values
- **THEN** system calculates BMI using the formula: BMI = weight(kg) / (height(m))^2
- **AND** system stores the calculated BMI value

#### Scenario: BMI calculation precision
- **WHEN** BMI is calculated
- **THEN** system stores BMI with 2 decimal places precision

#### Scenario: BMI calculation requires valid inputs
- **WHEN** weight or height is missing
- **THEN** system does not calculate BMI and returns validation error

### Requirement: System must validate anthropometric data
The system MUST validate all anthropometric data before storing.

#### Scenario: Valid weight validation
- **WHEN** user provides a valid weight (1-500 kg)
- **THEN** system accepts the weight

#### Scenario: Invalid weight validation
- **WHEN** user provides weight outside valid range (negative or >500 kg)
- **THEN** system rejects the weight with validation error

#### Scenario: Valid height validation
- **WHEN** user provides a valid height (0.1-3.0 meters)
- **THEN** system accepts the height

#### Scenario: Invalid height validation
- **WHEN** user provides height outside valid range (less than 10cm or more than 3m)
- **THEN** system rejects the height with validation error

#### Scenario: Valid arm span validation
- **WHEN** user provides a valid arm span (0.1-3.0 meters)
- **THEN** system accepts the arm span

#### Scenario: Invalid arm span validation
- **WHEN** user provides arm span outside valid range
- **THEN** system rejects the arm span with validation error

### Requirement: System must track profile audit information
The system MUST track creation and update timestamps for all profile records.

#### Scenario: Profile creation timestamp
- **WHEN** a new profile is created
- **THEN** system automatically sets createdAt to current timestamp

#### Scenario: Profile update timestamp
- **WHEN** a profile is updated
- **THEN** system automatically updates updatedAt to current timestamp

#### Scenario: Profile soft deletion timestamp
- **WHEN** a profile is soft deleted
- **THEN** system automatically sets deletedAt to current timestamp

### Requirement: System must associate profile with athlete
The system MUST ensure each profile is linked to exactly one athlete.

#### Scenario: Create profile for existing athlete
- **WHEN** creating a profile for an existing athlete
- **THEN** system links the profile to the athlete

#### Scenario: Create profile for non-existent athlete
- **WHEN** trying to create a profile for a non-existent athlete
- **THEN** system rejects the operation with validation error

#### Scenario: One athlete one profile
- **WHEN** checking athlete relationship
- **THEN** system ensures each athlete has at most one active profile
