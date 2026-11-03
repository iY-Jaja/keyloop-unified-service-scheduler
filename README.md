# Unified Service Scheduler (Scenario A)

Backend implementation of the Keyloop Technical Assessment's **Resource Constrained Booking** scenario.

## What it does

- Books a vehicle service appointment against a dealership's service bays and qualified technicians.
- Uses the service type's configured duration and checks the **whole** appointment interval.
- Persists the confirmed appointment, assigned resources, and idempotency key.
- Rejects bookings that conflict with an existing bay, qualified technician, or the same vehicle, using specific `409` error codes.
- Exposes a versioned REST API under `/api/v1`.
- Lists appointments through a bounded, paginated endpoint with stable ordering.
- Caches service types and dealership resource summaries in Ehcache for five minutes. Live availability is never cached.
- Uses a conventional layered MVC structure: controllers call services, services use Spring Data repositories, and repositories persist JPA entities. Spring AOP measures booking duration/outcome.
- Uses PostgreSQL as the only supported database. Shared connection settings are in `application.yaml`; the `prod` profile switches Flyway to schema-only migrations for non-demo deployments.
- Configures Tomcat request handling and HikariCP connection pooling with bounded local-development defaults.

## Requirements and run

- Java 8 or newer (the project compiles to Java 8 bytecode)
- Maven 3.6+
- PostgreSQL 16 is the only supported database. Docker Compose provides a local PostgreSQL instance; tests use PostgreSQL Testcontainers and require Docker.

Configuration is YAML-only:

- `src/main/resources/application.yaml` contains PostgreSQL connection defaults and local demo fixture locations.
- `src/main/resources/application-prod.yaml` switches Flyway to schema-only migrations when the `prod` profile is active; `application-dev.yaml` retains demo fixtures for local development.
- Environment variables (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`) override connection details; use a secret manager for real deployments.
- Tomcat is configured for 50 worker threads, 200 open connections, and a 50-request accept queue. HikariCP is capped at 10 DB connections with a five-second pool wait (Hikari timeout values are configured in milliseconds). `spring.mvc.async.request-timeout` applies to asynchronous MVC requests; it does not impose a total duration limit on the current synchronous booking handler. Database/transaction timeouts should be tuned separately for the production database and workload.

Code follows the MVC layered structure: REST request/response DTOs are in `dto/`, controllers and the correlation filter are in `controller/`, business operations are in `service/`, Spring Data interfaces are in `repository/`, and JPA mappings are in `entity/`. Plain business objects remain in `domain/`; `JpaEntityMapper` maps between those objects and persistence entities. Services depend directly on repositories, avoiding extra port and adapter layers while keeping HTTP handling out of business operations.

Start PostgreSQL and the application together with Docker Compose:

```bash
cp .env.example .env
docker compose up --build
```

The app listens on `http://localhost:8080`. Flyway creates the PostgreSQL schema and loads demo data: three dealerships, four customers, four vehicles, thirteen service types, five bays, five technicians, and their qualifications. No appointments are preloaded, so the booking examples can use fresh slots. To run the app directly with Maven instead, make sure PostgreSQL is running and configure `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`.

Example demo IDs:

| Entity | IDs |
|---|---|
| Dealerships | `dealer-1`, `dealer-2`, `dealer-3` |
| Customers | `customer-1` through `customer-4` |
| Vehicles | `vehicle-1` through `vehicle-4` |
| Service types | `oil-change`, `inspection`, `tire-rotation`, `brake-service`, `battery-check`, `coolant-flush`, `transmission-service`, `wheel-alignment`, `air-conditioning-service`, `engine-diagnostic`, `windshield-repair`, `spark-plug-replacement`, `safety-inspection` |

Use the dealership resource endpoint to see which bays and technicians belong to each dealership. A technician must be qualified for the selected service type; the booking API makes that assignment automatically.

The optional `postgres` profile uses the same PostgreSQL datasource and limits Flyway to schema migrations without demo fixtures. Configure credentials through `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`; do not commit credentials.

## Run the full stack with Docker Compose

Docker Compose starts the Spring Boot API and PostgreSQL. Ehcache and AOP run in-process with the application. Compose explicitly loads demo data into PostgreSQL, so this stack is intended for local development and assessment demos.

```bash
cp .env.example .env
docker compose up --build
```

The API is available at `http://localhost:8080`. PostgreSQL is private to the Compose network and is not exposed on the host. Compose waits for PostgreSQL's health check before starting the API, and the API's readiness endpoint is used for its health check. Flyway initializes the database schema and local demo fixtures.

Stop the containers while preserving database data:

```bash
docker compose down
```

PostgreSQL data is stored in the named `postgres_data` volume. The sample `.env` password is for local use only; change it before sharing the environment and use a secrets manager outside local development. The Compose stack does not include Kafka or another broker because event publishing/outbox processing is not implemented; add one when an asynchronous consumer is introduced.

The API container uses the Tomcat and HikariCP pool settings in `application.yaml`. The database is on the internal Compose network; only the API port is published to the host. The Compose health check waits for the application readiness probe, while PostgreSQL has its own `pg_isready` check.

## Testing strategy

The service is a REST API with no browser UI, so end-to-end coverage exercises real HTTP requests rather than Playwright. The test suite is split by boundary:

| Test level | Test class | Scope |
|---|---|---|
| Unit | `BookingServiceTest` | Booking orchestration and idempotency behavior with mocked collaborators; no Spring context or database |
| Component | `AppointmentControllerTest` | Spring MVC controller, validation, routing, and error/response mapping with mocked services |
| Integration | `AppointmentApiIntegrationTest`, `PostgreSqlBookingTest` | Full Spring context and MockMvc against PostgreSQL Testcontainers; booking, persistence, conflicts, pagination, and database concurrency |
| API E2E | `AppointmentApiE2ETest` | Starts the application on a random HTTP port and exercises booking, read-back, paginated listing, and idempotent replay against PostgreSQL Testcontainers |

There is no authentication in this assessment implementation, so tests verify malformed/missing request data rather than authentication failures. Docker Engine 29 rejects the older API version (`1.32`) selected by this Testcontainers setup; pass `-Dapi.version=1.40` to Maven so Testcontainers uses an API version supported by the daemon. Always check Surefire counts: a green Maven exit code with skipped PostgreSQL tests is not evidence that integration or E2E behavior passed.

Run unit and component tests without Docker:

```bash
mvn -Dapi.version=1.40 -Dtest=BookingServiceTest,AppointmentControllerTest test
```

Run the integration suites:

```bash
mvn -Dapi.version=1.40 -Dtest=AppointmentApiIntegrationTest,PostgreSqlBookingTest test
```

Run the API E2E suite:

```bash
mvn -Dapi.version=1.40 -Dtest=AppointmentApiE2ETest test
```

Run all levels together (Docker must be available for PostgreSQL integration and E2E suites):

```bash
mvn -Dapi.version=1.40 test
```

Inspect the Maven/Surefire result and confirm PostgreSQL suites have zero skipped tests; a successful Maven exit code with skipped containers is not PostgreSQL verification.

To run the application on your host against PostgreSQL in Docker, publish the database port by adding this under the `db` service in `docker-compose.yml`:

```yaml
    ports:
      - "5433:5432"
```

Then recreate the database container and run the app on your host. Port `5433` avoids accidentally connecting to another PostgreSQL service on the default port. The base configuration already uses PostgreSQL, so no profile is required:

```bash
docker compose stop app
docker compose up -d db
DB_URL=jdbc:postgresql://localhost:5433/service_scheduler \
DB_USERNAME=scheduler \
DB_PASSWORD=scheduler_local_only \
mvn spring-boot:run
```

The credentials above must match the database's initialized role. PostgreSQL applies `POSTGRES_PASSWORD` only when its data directory is first initialized; changing `.env` later does not change the password stored in the persistent `postgres_data` volume. Preserve data by using the existing database password or changing the role password inside PostgreSQL. Only remove the named volume if its data is disposable and you intentionally want to reset it.

Flyway applies schema migrations only in the `prod` profile. In a real deployment, customer/dealership records are provisioned by master-data integrations. To load demo fixtures into a disposable database, override the Flyway locations:

```bash
SPRING_FLYWAY_LOCATIONS=classpath:db/migration,classpath:db/demo \
DB_URL=jdbc:postgresql://localhost:5432/service_scheduler \
DB_USERNAME=scheduler \
DB_PASSWORD=scheduler_local_only \
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

## API

### Reference data

```bash
curl http://localhost:8080/api/v1/service-types
curl http://localhost:8080/api/v1/dealerships/dealer-1/resources
```

### Book an appointment

The `Idempotency-Key` header is required. Send an ISO-8601 timestamp with an offset; the service normalizes it to UTC. The demo `oil-change` takes 60 minutes.
An overlapping confirmed appointment for the same vehicle is rejected, including when requests target different dealerships. Reusing an idempotency key with the same request replays the original appointment; changing the request with that key returns `409`.

```bash
curl -i -X POST http://localhost:8080/api/v1/appointments \
  -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: booking-demo-001' \
  -d '{
    "dealershipId": "dealer-1",
    "customerId": "customer-1",
    "vehicleId": "vehicle-1",
    "serviceTypeId": "oil-change",
    "startAt": "2027-01-16T10:00:00+07:00"
  }'
```

The successful response is `201 Created`, includes the assigned bay and technician, and has a `Location` header. Repeating the same request with the same key returns the original appointment. Reusing the key with a different payload returns `409 IDEMPOTENCY_KEY_REUSED`.

The database persists appointments across app/container restarts. A slot that worked before may therefore return a conflict on a later run. A new idempotency key does **not** bypass a vehicle or resource conflict; choose a different, unbooked interval. To inspect confirmed bookings overlapping a proposed `2027-01-15T10:00:00+07:00` oil-change slot in the Compose database (stored in UTC), run:

```bash
docker compose exec -T db psql -U scheduler -d service_scheduler -c \
  "SELECT start_at, end_at, status FROM appointment
   WHERE vehicle_id = 'vehicle-1' AND status = 'CONFIRMED'
     AND start_at < TIMESTAMP '2027-01-15 04:00:00'
     AND end_at > TIMESTAMP '2027-01-15 03:00:00'
   ORDER BY start_at;"
```

If the query returns a row, the slot is already occupied and `409 VEHICLE_ALREADY_BOOKED` is expected. Choose a different unoccupied time. A successful booking is persisted, so repeating it with the same idempotency key and payload replays the original `201` response.

```bash
curl http://localhost:8080/api/v1/appointments/{appointmentId}
```

List appointments with zero-based page numbers (default `page=0`, `size=20`, maximum page size `100`):

```bash
curl 'http://localhost:8080/api/v1/appointments?page=0&size=20'
```

The list response contains `content`, `page`, `size`, `totalElements`, `totalPages`, `first`, and `last`. Results are sorted by appointment start time and then ID. Appointment lists are deliberately not cached because bookings change them; the service re-reads current data for each page. Only the relatively stable service catalog and dealership resources use the five-minute cache.

Other useful responses:

| HTTP | Code | Meaning |
|---|---|---|
| 400 | `INVALID_REQUEST` | Invalid/missing fields, key, or a start time that is not in the future |
| 404 | `NOT_FOUND` | Unknown dealership, customer, vehicle, service type, or appointment |
| 409 | `SLOT_UNAVAILABLE` | No bay or qualified technician is free for the full interval |
| 409 | `VEHICLE_ALREADY_BOOKED` | The vehicle has an overlapping confirmed appointment, even at another dealership |
| 409 | `IDEMPOTENCY_KEY_REUSED` | Existing key was sent with a different request |

Error body shape: `{"code":"<ERROR_CODE>","message":"..."}`.

## Design and AI collaboration

See [SYSTEM_DESIGN.md](./SYSTEM_DESIGN.md) for the architecture diagram, MVC layered structure, AOP observability, design patterns, REST/event-driven suitability, request flow, assumptions, and trade-offs. Domain objects remain framework independent, while services directly use repositories and JPA entities for persistence. REST is used for synchronous booking; an outbox/event broker is a documented production extension, not currently implemented.

The end-to-end business booking sequence is documented in [business-workflows.md](./.github/modernize/assessment/engines/facts/business-workflows.md).

### AI Collaboration Narrative

I used Perplexity, Claude, and GitHub Copilot as AI tools because they helped me analyze the requirements and evaluate my own thinking. I used AI as a sounding board for architecture choices and edge cases—not as a replacement for my decisions. For this project, I considered the Java 8/Spring Boot 2.x constraints, appointment availability, duplicate requests, transaction outcomes, caching, and the trade-offs between Hexagonal Architecture and a simpler MVC layered structure. I chose the MVC direction, then used AI assistance while refining the implementation, tests, and documentation. I also considered pagination and whether caching changing appointment results was appropriate.

I treated AI suggestions as proposals and checked the implementation against the acceptance criteria and observed API behavior. The test suites cover qualified technician and bay assignment, overlapping appointments, idempotent retries and key reuse, simultaneous booking, pagination bounds, and read-back of a new appointment. I verified the full suite against PostgreSQL Testcontainers: all 19 tests passed with none skipped. Docker Engine 29 required `-Dapi.version=1.40` because its minimum supported API version is newer than the Testcontainers default used here. Before submission, I will review the final repository state and make sure my walkthrough accurately describes the choices and validation.

### Scenario A acceptance coverage

| Requirement | Implementation | Verification |
|---|---|---|
| Request a service appointment for a vehicle, service type, dealership, and desired time | `POST /api/v1/appointments`; request validation and booking service | `booksQualifiedTechnicianAndBayForFullServiceDuration` |
| Check that a bay and qualified technician are available for the full service duration | Transactional overlap checks with dealership/vehicle locks; service duration determines the interval | Booking and overlap integration tests; PostgreSQL concurrency tests require Docker |
| Persist an appointment associating customer, vehicle, technician, and bay | JPA appointment record saved with references in the booking transaction; appointment can be retrieved by ID | Booking test followed by `GET /api/v1/appointments/{appointmentId}` |

## Suggested video walkthrough (5–7 minutes)

1. **Introduction and scenario (30 sec):** Explain why simultaneous bay and technician availability matters.
2. **Architecture (90 sec):** Walk through the diagram in `SYSTEM_DESIGN.md`, emphasizing the controller-service-repository-entity layers, the AOP metrics aspect, transaction/dealership and vehicle locks, database, and why availability is not cached.
3. **Live demo (2 min):** Start with `docker compose up --build` to run the application against PostgreSQL, show the service catalog, create a booking with the cURL command above, repeat it with the same idempotency key, then try another key in the overlapping time slot to show the conflict response.
4. **Verification (60 sec):** Run `mvn -Dapi.version=1.40 test` with Docker available and point out the PostgreSQL overlap, retry, and simultaneous-booking integration tests.
5. **AI collaboration and ownership (60 sec):** Explain how Perplexity, Claude, and GitHub Copilot helped you analyze and evaluate your thinking; identify your own design decisions (especially MVC layers, locking, idempotency, and cache policy), how you verified the implementation, and any PostgreSQL test skip that remains unresolved.
6. **Learning and next steps (30 sec):** Mention PostgreSQL validation, authentication, and cancellation/rescheduling as production follow-ups.

## Scope and production considerations

This assessment implementation covers the backend service layer. It intentionally does not implement a UI, authentication/authorization, or external master-data integrations. Before production, add authenticated dealership-level access control, secrets management, database backup/recovery, migration/release controls, and a distributed cache or explicit invalidation if reference data changes frequently. See the system design document for concurrency and scaling trade-offs.
# keyloop-unified-service-scheduler
