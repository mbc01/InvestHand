# InvestHand

A Spring Boot REST API for managing investment group members and their monthly
contributions.

## Stack

| | |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.5.10 |
| Persistence | Spring Data JPA + Hibernate |
| Database | PostgreSQL 16 |
| Security | Spring Security + BCrypt |
| Build | Maven 3.8+ (wrapper included) |
| Tests | JUnit 5, Mockito, MockMvc, H2 (in-memory) |

Java 21 is enforced by `<java.version>` in `pom.xml` and pinned again in the
CI workflow. Building on an older JDK will fail.

## Project structure

```
src/main/java/com/investment/Group/management/
├── Config/         SecurityConfig, DatabaseSeeder, DatabaseUserDetailsService
├── Controller/     ContributionController
├── Service/        ContributionService
├── Repository/     ContributionRepository, MemberRepository, UserRepository, RoleRepository
├── model/          User, Member, Contribution, Role, Admin, Business
├── dto/            ContributionRequest, ContributionResponse
└── Exception/      GlobalExceptionHandler, MemberNotFoundException,
                    DuplicateContributionException
```

## Prerequisites

- JDK 21
- PostgreSQL 16 with a database named `Family_investment_scheme_db`

## Configuration

**No credentials are committed to the repository.** Configuration is read from
environment variables:

| Variable | Default | Purpose |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/Family_investment_scheme_db` | JDBC URL |
| `DB_USERNAME` | `postgres` | Database user |
| `DB_PASSWORD` | *(empty)* | Database password — **must be set locally** |
| `ADMIN_USERNAME` | `admin` | Username for the seeded admin |
| `ADMIN_EMAIL` | `admin@example.com` | Email for the seeded admin |
| `ADMIN_PASSWORD` | *(empty)* | Password for the seeded admin account |

`DB_PASSWORD` and `ADMIN_PASSWORD` have no defaults. If `DB_PASSWORD` is unset
the app starts and then fails with `The server requested SCRAM-based
authentication, but no password was provided` — the empty value is sent rather
than the connection being refused earlier, so the error looks like a Postgres
problem rather than a missing variable.

For local development you can instead create `src/main/resources/application-local.properties`
(git-ignored):

```properties
spring.datasource.password=your-local-password
```

then run with:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

On first startup the seeder creates the `ADMIN` and `MEMBER` roles and, if
`ADMIN_PASSWORD` is set, an initial administrator with a BCrypt-hashed
password. No admin is created when the variable is absent — there is no
default password to guess. **Change it before using the app anywhere real.**

## Build and run

```bash
./mvnw clean package      # build
./mvnw spring-boot:run    # run on http://localhost:8080
```

The schema is managed by Hibernate with `ddl-auto=update`, so tables are
created on first start. Note that `update` only ever *adds* — it never alters
an existing column's type or drops a column, so a column that was created as
`double precision` stays that way. See [Schema notes](#schema-notes).

Interactive API docs (requires the app running):

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

## Authentication

The API is **stateless** and authenticates with **HTTP Basic**: credentials are
sent in an `Authorization` header on every request and nothing is stored
server-side.

```bash
curl -u "admin:your-password" http://localhost:8080/api/contributions
```

Rules:

- Reads require any authenticated user.
- Writes require the `ADMIN` role, enforced by `@PreAuthorize` on the
  controller.
- `/actuator/health`, `/swagger-ui/**` and `/v3/api-docs/**` are public.
- There is no self-service signup; accounts are created by an administrator.

**CSRF is disabled, deliberately.** CSRF protects against the browser silently
attaching ambient authority — a session cookie — to a cross-site request. That
does not apply here: the session policy is `STATELESS`, so no `JSESSIONID` is
ever issued, and credentials travel in an explicit header that a cross-site
HTML form post cannot set.

This is not merely redundant. CSRF was originally left enabled, and because the
default token repository needs a session to store the token in — and there is
neither a session nor an endpoint to issue one — **every `POST` failed with a
`403` no client could satisfy.** If authentication ever moves to cookies or
sessions, CSRF must be re-enabled and a token endpoint added.

## API

| Method | Path | Role | Description |
|---|---|---|---|
| `POST` | `/api/contributions/{memberId}` | ADMIN | Record a contribution. Returns `201`. |
| `GET` | `/api/contributions` | any | List all contributions. |
| `GET` | `/api/contributions/member/{memberId}` | any | List one member's contributions. |
| `GET` | `/actuator/health` | public | Health probe for monitoring. |

Example:

```bash
curl -X POST http://localhost:8080/api/contributions/1 \
  -u "admin:your-password" \
  -H 'Content-Type: application/json' \
  -d '{"amount": 500.00, "month": "JANUARY"}'
```

### Error responses

Domain failures return meaningful status codes rather than a blanket `500`:

| Status | Cause |
|---|---|
| `400` | Amount missing, zero, negative, or malformed JSON |
| `401` | Missing or invalid credentials |
| `403` | Authenticated but not an ADMIN (write attempted) |
| `404` | No member with that id |
| `409` | Contribution already recorded for that month |

```json
{
  "timestamp": "2026-10-08T05:00:39.070Z",
  "status": 409,
  "error": "Conflict",
  "message": "Member 2 has already made a contribution for JANUARY",
  "path": "/api/contributions"
}
```

## Business rules

- A member may contribute at most once per month. `month` is normalised to
  upper case so `january` cannot bypass the duplicate check; when omitted, the
  current month is used.
- Payments on or before the 5th are recorded as `PAID`, later ones as `LATE`.
  The server always sets `status`, `paymentDate` and `member` — clients cannot
  supply them, which is enforced by binding to a request DTO rather than the
  entity.
- Amounts are validated with `BigDecimal.compareTo`, not `equals`, because
  `equals` is scale-sensitive: `100.00` and `100` are equal in value but not
  under `equals`.

## Tests

```bash
./mvnw test
```

24 tests run against in-memory H2, so no local PostgreSQL instance is required
and real data is never touched.

| Suite | Covers |
|---|---|
| `ContributionServiceTest` | Duplicate detection, amount validation, month normalisation, money precision |
| `ContributionApiSecurityTest` | Auth enforcement, 401/403/404/409 status codes, no password in responses |
| `UserSerializationTest` | Regression test: credentials never serialised to JSON |
| `GroupManagementApplicationTests` | Context loads, roles seeded, admin password is a BCrypt hash |

## Schema notes

Two things are worth knowing before changing the entity classes.

**`month` is a column name that differs between databases.** It is unreserved in
PostgreSQL, so `month varchar(255)` is valid there, but H2 *reserves* `MONTH`
and rejects it. When it was rejected, Hibernate logged a DDL failure, continued
silently, and left the `contributions` table missing entirely — every write then
failed with `Table "CONTRIBUTIONS" not found`. The fix is in the test
datasource, not the entity:

```properties
# application-test.properties
spring.datasource.url=jdbc:h2:mem:investhand;...;NON_KEYWORDS=MONTH
```

Production keeps `month` so it matches the existing data.

**The database has drifted from the entities.** Two items are mapped by neither:

- `users.active` (`boolean`) — the `User` entity has no such field.
- `user_roles` — a proper join table with foreign keys to `users` and `roles`,
  unused by the code. The model stores `role` as a plain string on `User`
  instead.

Reconciling these is a design decision, not a mechanical fix: either map the
entities onto them, or drop them if they are vestigial.

## Design notes

- **Money is `BigDecimal`, never `double`.** Binary floating point cannot
  represent `0.10` exactly, so summing amounts with `double` drifts.
- **Passwords are never serialised.** `@JsonIgnore` sits on both the `User`
  password field and its getter, so leaking credentials is impossible even if a
  future controller returns an entity directly. Controllers also return DTOs.
- **Writes are transactional.** The duplicate check and the insert must be
  atomic, or two simultaneous requests can both pass the check.
- **Repositories are top-level interfaces.** They were previously nested inside
  plain wrapper classes that Spring never registered as beans.
- **Failed DDL is silent.** Hibernate logs a warning for a rejected `CREATE
  TABLE` and carries on, so a schema error shows up much later as a confusing
  runtime failure. When adding a column, check the startup log for
  `GenerationTarget encountered exception accepting command`.

## CI

`.github/workflows/ci.yml` runs on every push and pull request to `main`:
JDK 21, `mvn -B clean verify`, and the packaged jar uploaded as an artifact. No
database credentials are needed because the tests run on H2.

## License

Private. All rights reserved.