# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

A REST API for booking appointments between clients and managers (Spring Boot 3.5, Java 21, PostgreSQL, Flyway). No UI. The original requirements live in [docs/SPECS.md](docs/SPECS.md).

## Environment constraint: Docker-only

There is **no JDK 21 or Maven on the host** (only JDK 23/26 are installed). Everything builds, runs, and tests inside containers. Do not assume `mvn`/`java` work on the host. The Maven wrapper (`mvnw`) exists only as a fallback for someone who *does* have JDK 21.

The host **Docker Engine is v29+ (API 1.54)**, which the docker-java client bundled in Testcontainers cannot auto-negotiate. The test scripts work around this by pinning `DOCKER_API_VERSION=1.44` and `-Dapi.version=1.44`. Keep that pin when changing the test invocation.

## Commands

```bash
# Run the app (API on :8080, Postgres on :5432). Override ports if taken:
docker compose up --build
DB_PORT=5433 APP_PORT=8085 docker compose up --build

# Full test suite (spins up Postgres via Testcontainers, no host JDK needed)
./scripts/run-tests.ps1     # Windows
./scripts/run-tests.sh      # Linux/macOS

# Compile only (fast feedback), reusing a cached .m2 volume
docker run --rm -v "${PWD}:/app" -v afb_m2:/root/.m2 -w /app \
  maven:3.9-eclipse-temurin-21 mvn -B -DskipTests compile
```

To run **a single test class or method**, append `-Dtest=...` to the `mvn` line inside the test script (or the docker run above):

```bash
mvn -B test -DargLine="-Dapi.version=1.44" -Dtest=AppointmentServiceTest
mvn -B test -DargLine="-Dapi.version=1.44" -Dtest=AppointmentServiceTest#booksAppointmentForValidRequest
```

Pure unit tests (`AppointmentServiceTest`, `TimeSlotTest`) don't need Docker; the integration test and `contextLoads` do.

## Architecture

Layered + package-by-feature under `com.afb.scheduler`: each of `user`, `department`, `appointment` owns its entity, `JpaRepository`, `@Service`, `@RestController`, and `dto/` records. Cross-cutting code lives in `common/` (`error/`, `config/`).

Request flow: **Controller → Service (`@Transactional`, all business rules) → Repository**. DTOs are Java records; entities never leave the service boundary. The spec's JSON field names (`refRDV`, `dateRDV`, etc.) are mapped with `@JsonProperty` on the DTO records.

### Concurrency model (the core of this project)

Rule "a manager cannot have more than one appointment per time slot" must hold under simultaneous bookings. It is defended in layers, so changes here need care:

1. **`V1__init_schema.sql`** defines a *partial* unique index `uq_manager_slot_active (manager_id, slot_start) WHERE status = 'BOOKED'`. This is the race-proof guarantee; the partial predicate lets a cancelled slot be re-booked.
2. **`AppointmentService.book`** does a friendly pre-check, then `saveAndFlush` and catches `DataIntegrityViolationException`, rethrowing `ConflictException` (the lost-race path).
3. **`GlobalExceptionHandler`** maps `DataIntegrityViolationException` → 409 as a final backstop, and `ObjectOptimisticLockingFailureException` → 409.
4. Cancels/reschedules rely on `@Version` optimistic locking on `Appointment`.

The two-thread race is asserted in `AppointmentBookingIntegrationTest`.

### Conventions that bite if ignored

- **Flyway owns the schema; `spring.jpa.hibernate.ddl-auto=validate`.** Any entity change must come with a matching migration (`src/main/resources/db/migration/V*.sql`) or startup fails validation. Never edit an applied migration — add a new one.
- **`open-in-view=false`.** Lazy associations are not available after the transaction closes. Repository read methods use `@EntityGraph` to eager-fetch `manager`, `department`, and `participants` so controllers can map them. New read endpoints must do the same or map inside the service.
- **The 5 "services" from the spec are modeled as `Department`** (to avoid colliding with the Spring service layer), referenced by `code` (`ARCHIVES`, `DAF`, `HR`, `ACCOUNTING`, `SOCIAL_AFFAIRS`), seeded in `V2`.
- **Time-slot rules are centralized in `TimeSlot`** (hourly starts 08:00–15:00; `slotStartOf` truncates to the hour). Use it rather than re-deriving slot logic.
- **Time-based rules use an injected `Clock`** (`ClockConfig`) so tests can pin "now". The two-day lead time is computed in `AppointmentService.validateLeadTime`.
- **JPA entities have `protected` no-arg constructors.** Tests in other packages cannot `new` them — mock them (Mockito) or go through the real construction path.

### Error model

Service throws `ResourceNotFoundException` (404), `BusinessRuleException` (422), or `ConflictException` (409); `GlobalExceptionHandler` renders all errors as RFC 7807 `ProblemDetail`. Bean Validation failures on DTO records become 400 with a field-error map.
