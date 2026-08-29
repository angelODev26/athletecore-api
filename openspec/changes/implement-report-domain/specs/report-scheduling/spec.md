## ADDED Requirements

### Requirement: System schedules recurring reports with a cron expression
The system MUST allow creating a `ReportSchedule` with a periodic schedule (cron expression, optional timezone, report type, optional athlete/category and filters) and an `active` flag. Invalid cron expressions are rejected.

#### Scenario: Create a valid schedule
- **WHEN** an administrator creates a schedule with a valid cron expression for a general report
- **THEN** the system persists the schedule as active and computes the next run timestamp

#### Scenario: Reject invalid cron expression
- **WHEN** an administrator submits a malformed cron expression
- **THEN** the system rejects the request with a 400 Bad Request error

### Requirement: Scheduler executes due schedules automatically
The system MUST automatically generate reports for active schedules whose `next_run_at` is at or before the current time, and update `last_run_at`/`next_run_at` after each run.

#### Scenario: Due schedule generates a report
- **WHEN** the scheduler runs and an active schedule has `next_run_at <= now`
- **THEN** the system generates the corresponding report, updates `last_run_at` to now and advances `next_run_at` per the cron expression

#### Scenario: Inactive schedule is skipped
- **WHEN** the scheduler runs and a schedule is deactivated
- **THEN** the system does not generate any report for it

#### Scenario: Schedule failure does not disable the schedule
- **WHEN** a scheduled report generation fails once
- **THEN** the schedule remains active and retains its previously computed `next_run_at`, so it can retry on the next cycle

### Requirement: Schedule management is restricted to ADMIN
The system MUST require the ADMIN role to create, update or deactivate/delete report schedules; listing is available to authenticated users.

#### Scenario: Non-admin cannot manage schedules
- **WHEN** a user without ADMIN role attempts to create, update or delete a schedule
- **THEN** the system rejects the request with a 403 Forbidden error

### Requirement: Scheduling logic is deterministic and testable
The system MUST compute next-run timestamps using an injected `Clock` so that schedule execution is deterministic and testable with a fixed clock.

#### Scenario: Next run computed deterministically
- **WHEN** the clock is fixed and a cron expression is evaluated
- **THEN** the computed `next_run_at` is stable and reproducible for the same inputs and clock