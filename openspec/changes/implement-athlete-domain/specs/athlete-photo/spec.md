## ADDED Requirements

### Requirement: System allows uploading athlete profile photo
The system MUST allow administrators to upload a profile photo reference for athletes.

#### Scenario: Upload photo URL for athlete
- **WHEN** user provides a valid photo URL for an athlete
- **THEN** system stores the URL and associates it with the athlete

#### Scenario: Update existing photo URL
- **WHEN** user updates the photo URL for an athlete
- **THEN** system replaces the old URL with the new one

#### Scenario: Query athlete photo
- **WHEN** user requests an athlete's profile photo
- **THEN** system returns the photo URL

#### Scenario: Remove photo URL
- **WHEN** user wants to remove an athlete's photo
- **THEN** system clears the photo URL

#### Scenario: Upload photo for non-existent athlete
- **WHEN** user tries to upload a photo for a non-existent athlete
- **THEN** system rejects the operation with 404 error

### Requirement: System must validate photo URL format
The system MUST validate that photo URLs are in valid format.

#### Scenario: Valid HTTP URL
- **WHEN** user provides a valid HTTP/HTTPS URL
- **THEN** system accepts the URL

#### Scenario: Valid HTTPS URL
- **WHEN** user provides a valid HTTPS URL
- **THEN** system accepts the URL

#### Scenario: Invalid URL format
- **WHEN** user provides an invalid URL format
- **THEN** system rejects the URL with validation error

#### Scenario: Empty URL
- **WHEN** user provides an empty string as photo URL
- **THEN** system accepts it (treated as no photo)

### Requirement: System must NOT store binary photo data
The system MUST store only the photo URL reference, not the binary image data.

#### Scenario: Storage verification
- **WHEN** a photo is uploaded
- **THEN** system stores only the URL string in the database
- **AND** system does NOT store or process binary image data

#### Scenario: Photo access pattern
- **WHEN** an athlete profile is queried
- **THEN** system returns the photo URL in the response
- **AND** the application client is responsible for fetching the actual image

### Requirement: System must track photo audit information
The system MUST track when the photo was added or updated.

#### Scenario: Photo addition timestamp
- **WHEN** a photo URL is first set for an athlete
- **THEN** system records the addition timestamp in updatedAt

#### Scenario: Photo update timestamp
- **WHEN** a photo URL is updated
- **THEN** system updates the updatedAt timestamp

### Requirement: System must handle photo URL changes gracefully
The system MUST allow updating or removing photo URLs without affecting other athlete data.

#### Scenario: Update photo without changing other fields
- **WHEN** user only updates the photo URL
- **THEN** system updates only the photo field and preserves all other athlete data

#### Scenario: Remove photo without deleting athlete
- **WHEN** user removes the photo URL
- **THEN** system clears only the photo field and preserves all other athlete data
