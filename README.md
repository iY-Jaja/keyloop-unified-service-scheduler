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

## Assessment implementation scope

I chose the **backend service layer**. A customer-facing browser client is out of scope; clients are represented by documented REST endpoints and cURL examples. Swagger UI is included as developer documentation for browsing and trying the API, while tests exercise API behavior through MockMvc and real HTTP requests.

| Part 2 requirement | Implementation |
|---|---|
| Expose a REST API | Versioned endpoints under `/api/v1` for booking, appointment retrieval/listing, service types, and dealership resources |
| Persist data | PostgreSQL with JPA and Flyway-managed schema; Compose uses a named volume, and integration tests use PostgreSQL Testcontainers |
| Stub the client | cURL examples below demonstrate reference-data requests, booking, and appointment retrieval; API/component and HTTP E2E tests provide an executable test harness |
| Consider scalability and performance | Bounded pagination, database indexes, connection/thread pool limits, and caching for stable reference data; live availability and appointment lists are not cached. Pool sizing is a starting point, not a load-tested capacity claim |
| Consider reliability | Transactional booking with PostgreSQL pessimistic locks and idempotency, health-gated Compose startup, and readiness probes |
| Consider maintainability | Layered MVC responsibilities, Flyway migrations, stable API errors, and unit, component, integration, and API E2E tests |
| Consider observability | Correlation IDs in request logs, AOP booking metrics, and Actuator health/metrics endpoints |

These are design and implementation measures for the assessment scope, not a claim of production readiness. Authentication, authorization, load testing, distributed tracing, and deployment-grade secrets management remain production follow-ups; see [SYSTEM_DESIGN.md](./SYSTEM_DESIGN.md).

## Requirements and configuration

- Java 8 or newer (the project compiles to Java 8 bytecode)
- Maven 3.6+
- PostgreSQL 16 is the only supported database. Docker Compose provides a local PostgreSQL instance; tests use PostgreSQL Testcontainers and require Docker.

Spring settings are YAML-based: `application.yaml` contains PostgreSQL defaults and demo fixture locations; the `prod` profile switches Flyway to schema-only migrations. `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` override the database connection settings through the process environment. Spring Boot does not load `.env` files directly. Docker Compose reads the project-root `.env` file for interpolation; copy `.env.example` to `.env` to provide `POSTGRES_PASSWORD`, which Compose passes to PostgreSQL and the app. Keep `.env` local and use a secret manager outside local development.

Tomcat and HikariCP use bounded defaults intended for local development, not as production capacity guarantees. The configured async MVC request timeout does not impose an end-to-end deadline on the synchronous booking handler; tune database and transaction timeouts separately.

Code follows the MVC layered structure: REST request/response DTOs are in `dto/`, controllers and the correlation filter are in `controller/`, business operations are in `service/`, Spring Data interfaces are in `repository/`, and JPA mappings are in `entity/`. Plain business objects remain in `domain/`; `JpaEntityMapper` maps between those objects and persistence entities. Services depend directly on repositories, avoiding extra port and adapter layers while keeping HTTP handling out of business operations.

## Run locally with Docker Compose

```bash
cp .env.example .env
docker compose up --build
```

The API listens on `http://localhost:8080`. PostgreSQL is private to the Compose network; only the API port is published. Compose waits for the database health check before starting the API, and uses the API readiness endpoint for its health check.

Compose activates the `prod` profile but overrides Flyway locations to load demo fixtures for this local assessment environment. The fixtures include three dealerships, four customers, four vehicles, thirteen service types, five bays, five technicians, and their qualifications. No appointments are preloaded.

Example demo IDs:

| Entity | IDs |
|---|---|
| Dealerships | `dealer-1`, `dealer-2`, `dealer-3` |
| Customers | `customer-1` through `customer-4` |
| Vehicles | `vehicle-1` through `vehicle-4` |
| Service types | `oil-change`, `inspection`, `tire-rotation`, `brake-service`, `battery-check`, `coolant-flush`, `transmission-service`, `wheel-alignment`, `air-conditioning-service`, `engine-diagnostic`, `windshield-repair`, `spark-plug-replacement`, `safety-inspection` |

Use the dealership resource endpoint to see which bays and technicians belong to each dealership. A technician must be qualified for the selected service type; the booking API makes that assignment automatically.

Stop the containers while preserving database data:

```bash
docker compose down
```

PostgreSQL data is stored in the named `postgres_data` volume. The sample `.env` password is for local use only; change it before sharing the environment and use a secrets manager outside local development. The Compose stack does not include Kafka or another broker because event publishing/outbox processing is not implemented; add one when an asynchronous consumer is introduced.

The `prod` profile uses the PostgreSQL datasource and schema-only Flyway migrations by default. To run the app directly with Maven, start PostgreSQL and set `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`. For local demo data, also override Flyway locations as shown below.

## Testing strategy

The service has no customer-facing browser application; Swagger UI is only for developer API documentation. End-to-end coverage therefore exercises the API over HTTP rather than customer workflows in Playwright. The test suite is split by boundary:

| Test level | Test class | Scope |
|---|---|---|
| Unit | `BookingServiceTest` | Booking orchestration and idempotency behavior with mocked collaborators; no Spring context or database |
| Component | `AppointmentControllerTest` | Spring MVC controller, validation, routing, and error/response mapping with mocked services |
| Integration | `AppointmentApiIntegrationTest`, `PostgreSqlBookingTest` | Full Spring context and MockMvc against PostgreSQL Testcontainers; booking, persistence, conflicts, pagination, and database concurrency |
| API E2E | `AppointmentApiE2ETest` | Starts the application on a random HTTP port and exercises booking, read-back, paginated listing, and idempotent replay against PostgreSQL Testcontainers |

PostgreSQL integration tests also cover back-to-back appointment boundaries, missing qualified technicians, customer/vehicle ownership mismatch, past appointment times, and unknown dealership or service-type IDs.

There is no authentication in this assessment implementation, so tests verify malformed or missing request data rather than authentication failures. In the verified Docker environment, Testcontainers required `-Dapi.version=1.40`; use the override if the daemon rejects its default API version. Check Surefire counts: a successful Maven exit with skipped PostgreSQL tests is not evidence that integration or E2E behavior passed.

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

The Docker image build is not a substitute for this host-side test run: container-backed tests may be skipped when the build environment cannot access the Docker daemon.

## Postman collection

Import [Unified-Service-Scheduler.postman_collection.json](./postman/Unified-Service-Scheduler.postman_collection.json) into Postman. Start the Compose stack, then run the collection's requests in order. It reads service types and dealership resources, creates a booking at a generated future time, retries it with the same idempotency key, verifies an overlapping booking is rejected, and reads the created appointment. Set the collection variable `baseUrl` if the API is not at `http://localhost:8080`.

The API contract is documented in [openapi.yaml](./openapi.yaml) using OpenAPI 2.0 (Swagger 2.0). With the application running, open `http://localhost:8080/swagger-ui/index.html` to browse the endpoints and try requests. Swagger UI loads this same contract from `/openapi.yaml`; the contract is maintained as a static file rather than generated from controller code. “Try it out” sends requests to the local API, so use a local/demo database—booking requests create persistent appointments. You can also import the file into Postman or another Swagger 2-compatible API tool.

The collection is a manual API smoke-test/demo, not a replacement for the automated Maven unit, component, integration, and API E2E suites. It creates a real appointment in the database; use it only with a local/demo database, not production.

To start the stack, wait for readiness, and run the same demo flow automatically from the terminal:

```bash
cp .env.example .env
./scripts/run_api_demo.sh
```

The script uses Bash, `curl`, `jq`, `uuidgen`, and Docker Compose. If the API is already running, use `./scripts/run_api_demo.sh --skip-compose`; set `BASE_URL` to target a different local API URL. Each run creates a persistent appointment using a generated future date and a unique idempotency key; use only with the local demo database.

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

The successful response is `201 Created`, includes the assigned bay and technician, and has a `Location` header. Repeating the same request with the same key replays the original appointment. Reusing the key with a different payload returns `409 IDEMPOTENCY_KEY_REUSED`.

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

See [SYSTEM_DESIGN.md](./SYSTEM_DESIGN.md) for the architecture diagram, MVC layered structure, AOP observability, design patterns, REST/event-driven suitability, request flow, assumptions, and trade-offs. The [API & service contracts](./.github/modernize/assessment/engines/facts/api-service-contracts.md) document inventories the endpoints and includes a booking sequence diagram. Domain objects remain framework independent, while services directly use repositories and JPA entities for persistence. REST is used for synchronous booking; an outbox/event broker is a documented production extension, not currently implemented.

The end-to-end business booking sequence is documented in [business-workflows.md](./.github/modernize/assessment/engines/facts/business-workflows.md).

### AI Collaboration Narrative

I used Perplexity, Claude, and GitHub Copilot to help unpack Scenario A, compare architecture options, and think through edge cases. I started by turning the requirements into design questions: how to check availability for the entire service duration, how to avoid double-booking when requests race, how to make retries safe, and what the API should return when the slot is unavailable. I also used AI as a sounding board for the trade-offs between layered MVC and Hexagonal Architecture, synchronous REST and event-driven booking, and caching versus fresh availability reads.

I made the final choices based on the assessment scope and Java 8/Spring Boot constraints. I chose layered MVC, synchronous booking, PostgreSQL transactions with pessimistic locks, persistent idempotency records, and caching only for relatively stable reference data. I did not add an event broker or outbox because there is no asynchronous consumer in this assessment. I treated AI output as suggestions to inspect, not as proof that a design or implementation was correct.

To verify the result, I reviewed the code against the acceptance criteria and ran unit, component, PostgreSQL integration, and API E2E tests. These exercise qualified bay and technician assignment over the full duration, conflicts and concurrent requests, idempotent retries and key reuse, persistence, and pagination. The full suite passed with no skipped tests; see [runtime-validation-report.md](./runtime-validation-report.md) for the exact command and results. I also refined the documentation to distinguish implemented behavior from future production recommendations.

### Scenario A acceptance coverage

| Requirement | Implementation | Verification |
|---|---|---|
| Request a service appointment for a vehicle, service type, dealership, and desired time | `POST /api/v1/appointments`; request validation and booking service | `booksQualifiedTechnicianAndBayForFullServiceDuration` |
| Check that a bay and qualified technician are available for the full service duration | Transactional overlap checks with dealership/vehicle locks; service duration determines the interval | Booking and overlap integration tests; PostgreSQL concurrency tests require Docker |
| Persist an appointment associating customer, vehicle, technician, and bay | JPA appointment record saved with references in the booking transaction; appointment can be retrieved by ID | Booking test followed by `GET /api/v1/appointments/{appointmentId}` |

## Suggested video walkthrough (about 8 minutes)

Use the [complete video walkthrough script](./VIDEO_WALKTHROUGH_SCRIPT.md) for a timed narration, screen cues, Postman demo steps, test results, AI collaboration story, and closing. The target runtime is about eight minutes, within the required 5–10 minute range.

## Scope and production considerations

This assessment implementation covers the backend service layer. It intentionally does not implement a UI, authentication/authorization, or external master-data integrations. Before production, add authenticated dealership-level access control, secrets management, database backup/recovery, migration/release controls, and a distributed cache or explicit invalidation if reference data changes frequently. See the system design document for concurrency and scaling trade-offs.
