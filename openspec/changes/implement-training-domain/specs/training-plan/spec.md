## ADDED Requirements

### Requirement: System allows creating an annual training plan
The system MUST allow administrators to create an annual training plan with name, start date, end date and optional description.

#### Scenario: Successful plan creation
- **WHEN** user submits a valid plan (name, startDate, endDate)
- **THEN** system creates the training plan and returns it

#### Scenario: End date before start date
- **WHEN** user submits a plan whose endDate is before startDate
- **THEN** system rejects the creation with a validation error

#### Scenario: Missing required fields
- **WHEN** user submits a plan without name or dates
- **THEN** system rejects the creation with validation errors

### Requirement: System allows managing training cycles
The system MUST allow organizing a plan into a hierarchy of cycles: mesocycles and microcycles, each with start/end date and order.

#### Scenario: Adding a cycle to a plan
- **WHEN** user adds a cycle (type, name, dates) to an existing plan
- **THEN** system creates the cycle associated with the plan and returns it

#### Scenario: Adding a cycle to non-existent plan
- **WHEN** user adds a cycle to a non-existent plan ID
- **THEN** system returns a 404 Not Found error

#### Scenario: Nested cycle in another cycle
- **WHEN** user adds a microcycle as a child of a mesocycle
- **THEN** system creates the nested cycle and links it to the parent cycle

#### Scenario: Cycle dates outside parent plan dates
- **WHEN** user adds a cycle whose dates fall outside the plan date range
- **THEN** system rejects the creation with a validation error

### Requirement: System allows retrieving plan detail
The system MUST return a plan with its full cycle hierarchy (mesocycles containing microcycles).

#### Scenario: Retrieving existing plan
- **WHEN** user requests an existing plan by ID
- **THEN** system returns the plan with its nested cycles

#### Scenario: Plan not found
- **WHEN** user requests a non-existent plan ID
- **THEN** system returns a 404 Not Found error

#### Scenario: Retrieving soft-deleted plan
- **WHEN** user requests a soft-deleted plan by ID
- **THEN** system returns a 404 Not Found error

### Requirement: System allows updating a training plan
The system MUST allow updating plan name, dates and description.

#### Scenario: Successful plan update
- **WHEN** user updates an existing plan with valid data
- **THEN** system updates the plan and returns it

#### Scenario: Update non-existent plan
- **WHEN** user tries to update a non-existent plan ID
- **THEN** system returns a 404 Not Found error

### Requirement: System allows deleting a training plan
The system MUST allow soft deleting a training plan.

#### Scenario: Successful plan deletion
- **WHEN** administrator deletes an existing plan
- **THEN** system marks the plan as deleted and returns deletion confirmation

#### Scenario: Delete non-existent plan
- **WHEN** administrator tries to delete a non-existent plan ID
- **THEN** system returns a 404 Not Found error

### Requirement: System must exclude soft-deleted training data from queries
All entities in the training module MUST automatically filter out soft-deleted records.

#### Scenario: Deleted plan excluded from list
- **WHEN** user queries all plans
- **THEN** system returns only non-deleted plans

#### Scenario: Deleted cycle excluded from hierarchy
- **WHEN** user requests a plan containing a soft-deleted cycle
- **THEN** system returns the plan without the deleted cycle