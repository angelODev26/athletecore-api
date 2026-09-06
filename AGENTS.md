# AGENTS.md

Spring Boot 3.5.6 REST API (Java 21, Maven, PostgreSQL 15, Flyway, JWT, Lombok) for managing athlete performance. Docs, comments, tests, and commit messages are written in **Spanish** (conventional commits, e.g. `feat:`, `refactor:`).

## Commands

- Run locally: `./mvnw spring-boot:run` — requires PostgreSQL and `JWT_SECRET` (main `application.properties` has no fallback for it). The gitignored `application-local.properties` provides dev fallbacks; activate it with `SPRING_PROFILES_ACTIVE=local` and pass env vars from `.env` (copy from `.env.example`, never commit).
- Tests: `./mvnw test`. **Gotcha:** the `@SpringBootTest` context test (`AthletecoreApiApplicationTests`) needs a live PostgreSQL — start `docker-compose up -d postgres` first, or run focused unit tests only: `./mvnw test -Dtest=UserServiceTest`.
- DB only: `docker-compose up -d postgres`. Migrations outside app startup: `docker-compose --profile migrate up flyway`.
- Docker dev (`app-dev` service) hot-reload is fake: it mounts `./target/classes` and `./src`, so you must `./mvnw package -DskipTests` then `docker-compose restart app-dev` to pick up changes.
- No linter configured; `./mvnw compile` is the quick verification step.

## Architecture

- Feature packages under `com.athletecore.api`: `domain` (User, Role, BaseEntity), `user`, `athlete`, `config`, `common/exception`.
- All entities extend `BaseEntity` — soft delete via `@SQLDelete`/`@SQLRestriction`, audit timestamps; repositories filter `deleted_at IS NULL`. Never delete rows physically.
- Responses use DTOs (records in `<module>/dto/`); never return JPA entities. Services are single-purpose (e.g. `AthleteRegistrationService`, `AthleteSportService`) — no god services.
- Exceptions: throw from `common/exception`; `GlobalExceptionHandler` centralizes error responses.
- Security: stateless JWT; only `POST /api/v1/users` is public (`SecurityConfig.java:25`); admin endpoints use `@PreAuthorize("hasRole('ADMIN')")`.
- Read `core/principles.md` before adding a module — it codifies these conventions.

## Database / Flyway

- Migrations: `src/main/resources/db/migration/V{n}__name.sql` — versioned, reversible, with `created_at`/`updated_at`/`deleted_at` and indexes (see `docs/flyway.md`).
- `spring.jpa.hibernate.ddl-auto=validate` always; schema changes require a new migration, never `create`/`update`. Flyway runs automatically at app startup.

## Workflow

- Roadmap and per-module status: `TASKS.md`. It assigns modules to agent role files in `.claude/agents/`.
- New features go through OpenSpec: use the `openspec-propose` / `openspec-apply-change` skills. Delta specs live in `openspec/changes/`, main specs in `openspec/specs/`.
- Current branch `feature/implement-athlete-domain` contains **uncommitted WIP** (athlete module, TASKS.md, OpenSpec change) — do not commit without asking.

## Domain Agents
Specialized subagents exist per module (see `.opencode/agents/` and `.claude/agents/`):
athlete-domain, training-domain, checkup-domain, report-domain, user-security,
backend-architect, quality-guardian. Delegate module-specific work to them.

## Quality review
Run `/review [rutas]` (Claude Code y OpenCode) tras terminar una tarea: invoca a
`quality-guardian` con el alcance (los archivos dados, o los cambios sin commitear /
último commit si no das rutas) y genera un reporte con evidencia `archivo:línea`
priorizado por severidad. Solo lectura — nunca modifica código.

## Testing

- Service tests: JUnit 5 + MockitoExtension (no Spring context), Spanish `@DisplayName`. Coverage target >80% per TASKS.md.
- No integration tests or TestContainers exist yet; the only `@SpringBootTest` is the context-load test.

## CodeGraph

- The repo is CodeGraph-indexed (`.codegraph/`): run `codegraph explore "<symbol or question>"` to locate code before grepping.
