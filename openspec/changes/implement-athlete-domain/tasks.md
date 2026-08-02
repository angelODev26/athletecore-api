## 1. Database Schema

- [x] 1.1 Create V2__athletes_schema.sql migration file
- [x] 1.2 Create athletes table with audit fields
- [x] 1.3 Create athlete_profiles table with OneToOne relationship
- [x] 1.4 Create sports table with description
- [x] 1.5 Create disciplines table with sport FK
- [x] 1.6 Create athlete_sports junction table (Many-to-Many)
- [x] 1.7 Add indexes on username, email, full name
- [x] 1.8 Add foreign key constraints
- [ ] 1.9 Run migration and verify tables created

## 2. Domain Entities

- [x] 2.1 Create Athlete entity extending BaseEntity
- [x] 2.2 Create AthleteProfile entity extending BaseEntity
- [x] 2.3 Create Sport entity extending BaseEntity
- [x] 2.4 Create Discipline entity extending BaseEntity
- [x] 2.5 Add Jakarta Validation annotations to entities
- [x] 2.6 Implement BMI calculation method in AthleteProfile
- [x] 2.7 Configure OneToOne relationship Athlete ↔ AthleteProfile
- [x] 2.8 Configure ManyToMany relationship Athlete ↔ Sport
- [x] 2.9 Configure OneToMany Sport → Discipline
- [x] 2.10 Add @JsonIgnore to avoid serialization issues

## 3. Repository Layer

- [x] 3.1 Create AthleteRepository extending JpaRepository
- [x] 3.2 Create AthleteProfileRepository with athlete lookup
- [x] 3.3 Create SportRepository with CRUD methods
- [x] 3.4 Create DisciplineRepository with sport filtering
- [x] 3.5 Add custom queries for common searches
- [x] 3.6 Add findByUsername, findByEmail methods

## 4. DTOs - Request

- [x] 4.1 Create CreateAthleteRequest DTO
- [ ] 4.2 Create UpdateAthleteRequest DTO
- [ ] 4.3 Create AthleteProfileRequest DTO
- [ ] 4.4 Create AssignSportsRequest DTO
- [x] 4.5 Add Jakarta Validation to request DTOs
- [ ] 4.6 Create CreateSportRequest DTO
- [ ] 4.7 Create CreateDisciplineRequest DTO
- [ ] 4.8 Create SportsAssignment DTO

## 5. DTOs - Response

- [x] 5.1 Create AthleteResponse DTO
- [ ] 5.2 Create AthleteProfileResponse DTO
- [ ] 5.3 Create SportResponse DTO
- [ ] 5.4 Create DisciplineResponse DTO
- [ ] 5.5 Create AthleteWithProfileResponse DTO
- [ ] 5.6 Create AthleteWithSportsResponse DTO
- [ ] 5.7 Ensure all DTOs exclude password and internal fields

## 5. DTOs - Response

- [ ] 5.1 Create AthleteResponse DTO
- [ ] 5.2 Create AthleteProfileResponse DTO
- [ ] 5.3 Create SportResponse DTO
- [ ] 5.4 Create DisciplineResponse DTO
- [ ] 5.5 Create AthleteWithProfileResponse DTO
- [ ] 5.6 Create AthleteWithSportsResponse DTO
- [ ] 5.7 Ensure all DTOs exclude password and internal fields

## 6. Service Layer

- [ ] 6.1 Create AthleteRegistrationService
- [ ] 6.2 Create AthleteProfileService
- [ ] 6.3 Create AthleteSportService
- [ ] 6.4 Implement registration with validation
- [ ] 6.5 Implement BMI calculation logic
- [ ] 6.6 Implement duplicate username/email checks
- [ ] 6.7 Implement soft delete operations
- [ ] 6.8 Add @Transactional annotations
- [ ] 6.9 Add @Transactional(readOnly = true) for queries
- [ ] 6.10 Handle exceptions appropriately

## 7. Controller Layer

- [ ] 7.1 Create AthleteController with REST endpoints
- [ ] 7.2 Implement POST /api/v1/athletes (register)
- [ ] 7.3 Implement GET /api/v1/athletes (list with pagination)
- [ ] 7.4 Implement GET /api/v1/athletes/{id} (retrieve)
- [ ] 7.5 Implement PUT /api/v1/athletes/{id} (update)
- [ ] 7.6 Implement DELETE /api/v1/athletes/{id} (soft delete)
- [ ] 7.7 Implement POST /api/v1/athletes/{id}/profile
- [ ] 7.8 Implement GET /api/v1/athletes/{id}/profile
- [ ] 7.9 Implement POST /api/v1/athletes/{id}/sports
- [ ] 7.10 Implement GET /api/v1/athletes/{id}/sports
- [ ] 7.11 Return DTOs not entities in responses
- [ ] 7.12 Use @Valid for request validation

## 8. Security Configuration

- [ ] 8.1 Add /api/v1/athletes endpoints to SecurityConfig
- [ ] 8.2 Require authentication for all athlete endpoints
- [ ] 8.3 Add @PreAuthorize for sensitive operations
- [ ] 8.4 Verify JWT authentication works
- [ ] 8.5 Test with different user roles

## 9. Tests - Unit Tests

- [ ] 9.1 Create AthleteRegistrationServiceTest
- [ ] 9.2 Create AthleteProfileServiceTest
- [ ] 9.3 Create AthleteSportServiceTest
- [ ] 9.4 Create AthleteControllerTest with MockMvc
- [ ] 9.5 Create AthleteRepositoryTest
- [ ] 9.6 Create AthleteEntityTest
- [ ] 9.7 Create DTO validation tests
- [ ] 9.8 Test BMI calculation with various inputs
- [ ] 9.9 Test duplicate username/email rejection
- [ ] 9.10 Test soft delete behavior

## 10. Tests - Integration Tests

- [ ] 10.1 Create AthleteIntegrationTest with TestContainers
- [ ] 10.2 Verify database migration in test
- [ ] 10.3 Test full CRUD flow
- [ ] 10.4 Test profile creation and update
- [ ] 10.5 Test sport assignments
- [ ] 10.6 Test authentication in integration tests

## 11. Documentation

- [ ] 11.1 Update TASKS.md with implementation progress
- [ ] 11.2 Add Athlete module section to README
- [ ] 11.3 Generate OpenAPI/Swagger documentation (optional)
- [ ] 11.4 Document API endpoints with examples
- [ ] 11.5 Update CHANGELOG.md

## 12. Code Quality

- [ ] 12.1 Run spotless:check on new code
- [ ] 12.2 Fix all compilation warnings
- [ ] 12.3 Ensure code follows core/principles.md
- [ ] 12.4 Verify all entities extend BaseEntity
- [ ] 12.5 Verify all responses use DTOs
- [ ] 12.6 Run findbugs/spotbugs scan

## 13. Final Verification

- [ ] 13.1 Run mvn clean test compile
- [ ] 13.2 Run flyway:migrate on test database
- [ ] 13.3 Start application and test endpoints manually
- [ ] 13.4 Verify soft delete works
- [ ] 13.5 Verify BMI calculation is correct
- [ ] 13.6 Check code coverage > 80%
