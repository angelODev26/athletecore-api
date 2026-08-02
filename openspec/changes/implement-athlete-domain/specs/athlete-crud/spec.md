## ADDED Requirements

### Requirement: System allows registering a new athlete
The system MUST allow administrators to register new athletes with personal information.

#### Scenario: Successful registration
- **WHEN** user submits a registration form with valid data
- **THEN** system creates a new athlete record and returns the created athlete

#### Scenario: Registration with duplicate username
- **WHEN** user tries to register with an existing username
- **THEN** system rejects the registration and returns a validation error

#### Scenario: Registration with duplicate email
- **WHEN** user tries to register with an existing email
- **THEN** system rejects the registration and returns a validation error

#### Scenario: Registration with invalid email format
- **WHEN** user tries to register with an invalid email format
- **THEN** system rejects the registration and returns a validation error

#### Scenario: Registration with missing required fields
- **WHEN** user submits registration with missing required fields
- **THEN** system rejects the registration and returns validation errors

### Requirement: System allows querying all athletes
The system MUST allow administrators to retrieve a list of all athletes.

#### Scenario: Successful athlete list retrieval
- **WHEN** user requests the athlete list
- **THEN** system returns all active athletes with basic information (id, username, email, fullName)

#### Scenario: Empty athlete list
- **WHEN** there are no athletes in the system
- **THEN** system returns an empty list

### Requirement: System allows retrieving a specific athlete
The system MUST allow administrators to retrieve details of a specific athlete.

#### Scenario: Successful athlete retrieval
- **WHEN** user requests an existing athlete by ID
- **THEN** system returns the athlete details including full name, email, profile, sports, photo

#### Scenario: Athlete not found
- **WHEN** user requests a non-existent athlete ID
- **THEN** system returns a 404 Not Found error

#### Scenario: Retrieve deleted athlete
- **WHEN** user requests a soft-deleted athlete by ID
- **THEN** system returns a 404 Not Found error (soft deleted athletes should not be visible)

### Requirement: System allows updating an athlete
The system MUST allow administrators to update athlete information.

#### Scenario: Successful athlete update
- **WHEN** user updates an existing athlete with valid data
- **THEN** system updates the athlete record and returns the updated athlete

#### Scenario: Update non-existent athlete
- **WHEN** user tries to update a non-existent athlete ID
- **THEN** system returns a 404 Not Found error

#### Scenario: Update with duplicate username (different athlete)
- **WHEN** user updates an athlete with a username that belongs to another athlete
- **THEN** system rejects the update and returns a validation error

### Requirement: System allows soft deleting an athlete
The system MUST allow administrators to soft delete an athlete (logical deletion).

#### Scenario: Successful athlete deletion
- **WHEN** administrator deletes an existing athlete
- **THEN** system marks the athlete as deleted (deleted_at timestamp set) and returns deletion confirmation

#### Scenario: Delete non-existent athlete
- **WHEN** administrator tries to delete a non-existent athlete ID
- **THEN** system returns a 404 Not Found error

#### Scenario: Retrieve deleted athlete after deletion
- **WHEN** user requests a deleted athlete by ID
- **THEN** system returns a 404 Not Found error (soft deleted records should not be visible in normal queries)

### Requirement: System must validate athlete data
The system MUST validate all athlete data before creating or updating.

#### Scenario: Valid username validation
- **WHEN** user provides a valid username (3-255 characters)
- **THEN** system accepts the username

#### Scenario: Invalid username validation
- **WHEN** user provides an invalid username (less than 3 characters)
- **THEN** system rejects the username with validation error

#### Scenario: Valid email validation
- **WHEN** user provides a valid email format
- **THEN** system accepts the email

#### Scenario: Invalid email validation
- **WHEN** user provides an invalid email format
- **THEN** system rejects the email with validation error

#### Scenario: Valid first name validation
- **WHEN** user provides a valid first name (1-100 characters)
- **THEN** system accepts the first name

#### Scenario: Invalid first name validation
- **WHEN** user provides an empty first name
- **THEN** system rejects the first name with validation error

#### Scenario: Valid last name validation
- **WHEN** user provides a valid last name (1-100 characters)
- **THEN** system accepts the last name

#### Scenario: Invalid last name validation
- **WHEN** user provides an empty last name
- **THEN** system rejects the last name with validation error

### Requirement: System must track audit information
The system MUST track creation and update timestamps for all athlete records.

#### Scenario: Creation timestamp
- **WHEN** a new athlete is created
- **THEN** system automatically sets createdAt to current timestamp

#### Scenario: Update timestamp
- **WHEN** an athlete record is updated
- **THEN** system automatically updates updatedAt to current timestamp

#### Scenario: Deletion timestamp (soft delete)
- **WHEN** an athlete is soft deleted
- **THEN** system automatically sets deletedAt to current timestamp

### Requirement: System must exclude deleted athletes from queries
The system MUST automatically filter out soft-deleted athletes from all queries.

#### Scenario: Filter deleted athletes from list
- **WHEN** user queries all athletes
- **THEN** system returns only non-deleted athletes

#### Scenario: Filter deleted athlete from single query
- **WHEN** user queries a deleted athlete by ID
- **THEN** system returns 404 Not Found (athletes with deletedAt should appear as not found)
