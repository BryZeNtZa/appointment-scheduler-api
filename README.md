# Appointment Booking System API

A REST API for scheduling appointments between clients and managers, built with
**Java 21** and **Spring Boot 3.5**. Persistence is **PostgreSQL** with
**Flyway** migrations. The project is fully containerized — the only thing you
need on your host is **Docker**.

## Domain rules

| Rule | Where it is enforced |
|------|----------------------|
| Each appointment has exactly one manager | `appointments.manager_id NOT NULL` + service validation (must be a `MANAGER`) |
| A manager cannot have more than one appointment per time slot | Partial unique index `uq_manager_slot_active (manager_id, slot_start)` + pre-check |
| An appointment can include at most two individuals | `AppointmentService` (1 required client + 1 optional) |
| An appointment must be booked at least two days in advance | `AppointmentService.validateLeadTime` (uses an injectable `Clock`) |

**Static data**

- **Time slots**: one-hour blocks from 08:00 to 16:00 → valid start times are
  08:00, 09:00, … 15:00 (the last block ends at 16:00).
- **Departments** (the spec's "services"): `Archives`, `Finance Department (DAF)`,
  `Human Resources (HR)`, `Accounting`, `Social Affairs`. Seeded by Flyway
  migration `V2`.

## Concurrency strategy

The critical rule is "one appointment per manager per time slot" under
simultaneous bookings. It is enforced in two layers:

1. **Database-level partial unique index** on `(manager_id, slot_start)` for
   `BOOKED` rows. This is the race-proof guarantee: if two transactions try to
   book the same manager and slot at the same time, exactly one commits and the
   other fails with a unique-constraint violation, which is translated to
   **HTTP 409 Conflict**. The partial predicate (`WHERE status = 'BOOKED'`) lets
   a cancelled slot be re-booked.
2. **Application-level pre-check** for a friendly error in the common
   (non-racing) case.

Appointment updates/cancellations additionally use **optimistic locking**
(`@Version`) to detect concurrent modification.

The integration test
[`AppointmentBookingIntegrationTest`](src/test/java/com/afb/scheduler/appointment/AppointmentBookingIntegrationTest.java)
fires two concurrent bookings at the same manager + slot and asserts exactly one
succeeds.

## Tech stack

- Java 21, Spring Boot 3.5 (Web, Data JPA, Validation)
- PostgreSQL 16, Flyway
- Error handling via `@RestControllerAdvice` + RFC 7807 `ProblemDetail`
- Tests: JUnit 5, Mockito, AssertJ, Testcontainers
- Build: Maven (wrapper included); multi-stage Docker build

## Running the application

Everything runs in containers. From the project root:

```bash
docker compose up --build
```

This starts:

- `db` — PostgreSQL 16 on `localhost:5432` (db/user/pass: `scheduler`)
- `app` — the API on `http://localhost:8080`, after the database is healthy

Flyway applies the schema and seeds departments on startup. Stop with
`Ctrl+C`; remove volumes with `docker compose down -v`.

If those host ports are already in use, override them:

```bash
DB_PORT=5433 APP_PORT=8085 docker compose up --build   # API then on :8085
```

## Running the tests

No JDK/Maven needed on the host — tests run in a Maven container that mounts the
Docker socket so Testcontainers can launch a Postgres container:

```powershell
# Windows
./scripts/run-tests.ps1
```

```bash
# Linux / macOS
./scripts/run-tests.sh
```

> The scripts pin the Docker API version (`1.44`) because the docker-java client
> bundled with Testcontainers cannot auto-negotiate the API version of Docker
> Engine ≥ 29. If you have a JDK 21 on your host you can also just run
> `./mvnw test`.

## API reference

Base path: `/api`

### Users

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/users` | Create a client or manager |
| `GET`  | `/api/users/{ref}` | Get a user by reference |
| `GET`  | `/api/users?role=MANAGER` | List users (optional `role` filter) |

```bash
curl -X POST http://localhost:8080/api/users \
  -H 'Content-Type: application/json' \
  -d '{"ref":"MGR-1","email":"manager@afb.test","telephone":"0600000001","nom":"Doe","prenom":"John","role":"MANAGER"}'

curl -X POST http://localhost:8080/api/users \
  -H 'Content-Type: application/json' \
  -d '{"ref":"CLI-1","email":"client@afb.test","telephone":"0600000002","nom":"Roe","prenom":"Jane","role":"CLIENT"}'
```

### Departments

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/departments` | List the 5 departments |

### Appointments

| Method | Path | Description |
|--------|------|-------------|
| `POST`   | `/api/appointments` | Book an appointment |
| `GET`    | `/api/appointments/{refRDV}` | Get an appointment |
| `GET`    | `/api/appointments?managerRef=MGR-1` | List appointments (optional `managerRef`) |
| `DELETE` | `/api/appointments/{refRDV}` | Cancel an appointment |

```bash
curl -X POST http://localhost:8080/api/appointments \
  -H 'Content-Type: application/json' \
  -d '{
        "refClient": "CLI-1",
        "secondClientRef": null,
        "refService": "HR",
        "refResponsable": "MGR-1",
        "dateRDV": "2026-06-01T09:00:00",
        "motifRdv": "Annual review"
      }'
```

- `refRDV` is optional on input; if omitted, the server generates one.
- `refService` is a department **code** (`ARCHIVES`, `DAF`, `HR`, `ACCOUNTING`,
  `SOCIAL_AFFAIRS`).
- `secondClientRef` is optional and enables a two-person appointment.

### Error format

All errors are RFC 7807 `application/problem+json`, e.g. a double-booking:

```json
{
  "type": "about:blank",
  "title": "Conflict",
  "status": 409,
  "detail": "Manager MGR-1 already has an appointment at 2026-06-01T09:00"
}
```

| Situation | Status |
|-----------|--------|
| Validation error (missing/invalid field) | 400 |
| Unknown user / department / appointment | 404 |
| Business rule broken (lead time, slot, participants, role) | 422 |
| Slot already taken / concurrent booking | 409 |

## Project structure

```
src/main/java/com/afb/scheduler
├── common/
│   ├── config/   ClockConfig
│   └── error/    exceptions + GlobalExceptionHandler (ProblemDetail)
├── user/         User, Role, repository, service, controller, dto
├── department/   Department, repository, service, controller, dto
└── appointment/  Appointment, AppointmentStatus, TimeSlot,
                  repository, service (rules + concurrency), controller, dto
src/main/resources/db/migration   V1 schema, V2 department seed
```

## Notes and assumptions

- **`telephone` stored as text.** The spec's example types it as `int`, but
  storing phone numbers as integers loses leading zeros and formatting, so a
  `VARCHAR` column is used.
- **Lead time** is interpreted on calendar days: an appointment date must be
  at least two days after today.
- **Slot range**: 08:00–15:00 start times (last block ends 16:00).
- **`JPA ddl-auto=validate`** — Flyway owns the schema; Hibernate only validates
  the entity mapping against it.
- **`open-in-view=false`** — associations needed by responses are fetched
  eagerly with entity graphs to avoid lazy-loading outside the transaction.
