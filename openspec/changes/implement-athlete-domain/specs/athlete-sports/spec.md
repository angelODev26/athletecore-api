## ADDED Requirements

### Requirement: System allows defining sports and disciplines
The system MUST allow administrators to define available sports and disciplines.

#### Scenario: Create new sport
- **WHEN** user creates a new sport entry
- **THEN** system creates the sport record with name and description

#### Scenario: Update sport
- **WHEN** user updates an existing sport
- **THEN** system updates the sport record

#### Scenario: Query sports list
- **WHEN** user requests the sports list
- **THEN** system returns all active sports with name and description

#### Scenario: Delete sport (soft)
- **WHEN** user soft deletes a sport
- **THEN** system marks the sport as deleted

### Requirement: System allows associating sports with athletes
The system MUST allow assigning multiple sports to athletes.

#### Scenario: Assign sport to athlete
- **WHEN** user assigns a sport to an athlete
- **THEN** system creates the relationship between athlete and sport

#### Scenario: Assign multiple sports to athlete
- **WHEN** user assigns multiple sports to an athlete
- **THEN** system creates multiple relationships (Many-to-Many)

#### Scenario: Query athlete sports
- **WHEN** user requests an athlete's sports
- **THEN** system returns all sports associated with the athlete

#### Scenario: Remove sport from athlete
- **WHEN** user removes a sport assignment from an athlete
- **THEN** system removes the relationship

#### Scenario: Assign non-existent sport
- **WHEN** user tries to assign a non-existent sport to an athlete
- **THEN** system rejects the operation with validation error

#### Scenario: Assign deleted sport
- **WHEN** user tries to assign a soft-deleted sport to an athlete
- **THEN** system rejects the operation with validation error

### Requirement: System allows defining disciplines
The system MUST allow administrators to define disciplines within sports.

#### Scenario: Create new discipline
- **WHEN** user creates a new discipline
- **THEN** system creates the discipline record with name, sport association, and description

#### Scenario: Update discipline
- **WHEN** user updates an existing discipline
- **THEN** system updates the discipline record

#### Scenario: Query disciplines list
- **WHEN** user requests the disciplines list filtered by sport
- **THEN** system returns all active disciplines for that sport

#### Scenario: Discipline must belong to a sport
- **WHEN** creating a discipline
- **THEN** system requires association with a parent sport

### Requirement: System must validate sport and discipline data
The system MUST validate all sport and discipline data before storing.

#### Scenario: Valid sport name validation
- **WHEN** user provides a valid sport name (1-100 characters)
- **THEN** system accepts the sport name

#### Scenario: Invalid sport name validation
- **WHEN** user provides an empty sport name
- **THEN** system rejects the sport name with validation error

#### Scenario: Valid discipline name validation
- **WHEN** user provides a valid discipline name (1-100 characters)
- **THEN** system accepts the discipline name

#### Scenario: Invalid discipline name validation
- **WHEN** user provides an empty discipline name
- **THEN** system rejects the discipline name with validation error

### Requirement: System must track sport and discipline audit information
The system MUST track creation and update timestamps for all sport and discipline records.

#### Scenario: Sport creation timestamp
- **WHEN** a new sport is created
- **THEN** system automatically sets createdAt to current timestamp

#### Scenario: Discipline update timestamp
- **WHEN** a discipline is updated
- **THEN** system automatically updates updatedAt to current timestamp

#### Scenario: Sport soft deletion timestamp
- **WHEN** a sport is soft deleted
- **THEN** system automatically sets deletedAt to current timestamp

### Requirement: System must support generic sport model
The system MUST support multiple sports, not just swimming.

#### Scenario: Add new sport type
- **WHEN** user adds a new sport (e.g., "Athletics", "Cycling")
- **THEN** system stores the sport and allows assigning it to athletes

#### Scenario: Associate discipline with sport
- **WHEN** user associates disciplines with a sport
- **THEN** system maintains the parent-child relationship
