# Scenario A Video Walkthrough Script

**Target length:** about 8 minutes

**Format:** screen recording with spoken narration; appearing on camera is optional.

Use this as speaking notes, not a word-for-word performance. Replace bracketed text with your own details, and only describe AI tools and decisions that match your actual experience.

## Before recording

1. Start the local stack and check readiness:

   ```bash
   docker compose up -d --build
   curl --fail http://localhost:8080/actuator/health/readiness
   ```

   Expect `{"status":"UP"}`.

2. Import [the Postman collection](./postman/Unified-Service-Scheduler.postman_collection.json) and select the `http://localhost:8080` base URL.
3. Keep [SYSTEM_DESIGN.md](./SYSTEM_DESIGN.md), [the API sequence diagram](./.github/modernize/assessment/engines/facts/api-service-contracts.md), and [the validation report](./runtime-validation-report.md) ready to show.
4. Close unrelated windows, hide notifications, and make a short screen-and-microphone test. A camera is not required.
5. Choose the demo approach: in Postman, run the collection requests in order, or run `./scripts/run_api_demo.sh` to execute the same flow from the terminal. Both approaches create persistent demo data; use the local demo stack only.

## 1. Introduction and scenario — 0:00–0:40

**On screen:** Show the project title or README overview.

**Say:**

> “Hello, I’m [name]. I chose Scenario A, the Unified Service Scheduler. The problem is to book a vehicle service appointment only when both a service bay and a technician qualified for that service are available for the whole service duration.
>
> I chose to implement the backend service layer. It exposes a REST API and uses PostgreSQL for persistent data. The client is represented by API examples and a Postman collection rather than a separate web application.”

## 2. System design and important choices — 0:40–2:00

**On screen:** Show the architecture diagram in [SYSTEM_DESIGN.md](./SYSTEM_DESIGN.md), then the booking flow in [api-service-contracts.md](./.github/modernize/assessment/engines/facts/api-service-contracts.md).

**Say:**

> “The application uses a layered MVC structure. The controller handles HTTP requests and validation. The service layer coordinates the booking use case. Repositories access PostgreSQL, where appointments, reference data, and idempotency records are stored.
>
> For a booking, the service calculates the end time from the service type’s duration, then checks the vehicle, bay, and qualified technician across the complete interval. The booking transaction uses pessimistic locks on the dealership and vehicle rows, so concurrent booking attempts are coordinated in the database. The appointment and idempotency record are persisted together.
>
> Booking is synchronous because the caller needs an immediate confirmation or conflict. Retrying with the same idempotency key returns the original appointment; using that key for a different request is rejected. I cache relatively stable reference data, but not live availability or appointment listings.
>
> For observability, the current implementation has correlation IDs in request logs, a booking-operation metric, and Actuator health and metrics endpoints. Prometheus/Grafana dashboards and OpenTelemetry tracing are possible future additions; they are not configured in this assessment.”

## 3. Application demonstration — 2:00–4:20

**On screen:** Show Postman and the imported collection, or run `./scripts/run_api_demo.sh` in a terminal. Keep the responses and status visible.

**Say:**

> “I’ll demonstrate the API using Postman. First, I’ll retrieve the service catalog and the resources for a dealership. This shows which service types can be requested and which bays and technicians belong to the dealership.”

**Action:** Run **List service types**, then **List dealership resources**.

**Say:**

> “Next, I’ll create an oil-change appointment. The collection chooses a future start time and a unique idempotency key, so I don’t have to hard-code a date. The service uses the oil-change duration to check the full appointment interval.”

**Action:** Run **Create appointment**. Point to `201 Created`, the assigned bay and technician, and the response’s appointment ID.

**Say:**

> “The API returned `201 Created` with the confirmed appointment and its assigned resources. The response includes a location for retrieving the appointment.”

**Action:** Run **Retry same booking**.

**Say:**

> “This request uses the same payload and the same idempotency key. It returns the same appointment rather than creating a duplicate.”

**Action:** Run **Try overlapping booking with a new key**.

**Say:**

> “Now I’m using a new key for the same vehicle and time. This is a new booking attempt, not a retry, so the API rejects it with a vehicle-conflict response.”

**Action:** Run **Get created appointment**, then **List appointments**.

**Say:**

> “Finally, I can retrieve the saved appointment by ID and see it in the paginated appointment list. These requests demonstrate the main API flow from reference data to booking, retry, conflict, and read-back.”

## 4. Tests and verification — 4:20–5:20

**On screen:** Show the results table in [runtime-validation-report.md](./runtime-validation-report.md), then optionally the command in the terminal.

**Say:**

> “I tested the service at four levels. Unit tests cover booking orchestration and idempotency logic. Component tests cover the HTTP controller and response behavior. PostgreSQL integration tests exercise resource assignment, overlap and concurrency behavior, ownership checks, validation, and pagination. API end-to-end tests send real HTTP requests to a running application and verify persistence and read-back.
>
> I ran `mvn -Dapi.version=1.40 clean test`. The full suite passed: 25 tests, zero failures, and zero skipped tests. The integration and end-to-end tests used PostgreSQL Testcontainers. The API readiness check also returned `UP`. The report records the command and results.”

## 5. GenAI collaboration — 5:20–6:50

**On screen:** Show the “GenAI use during system design” section in [SYSTEM_DESIGN.md](./SYSTEM_DESIGN.md).

**Say:**

> “I used [name only the AI tools you actually used] to help unpack the requirements, compare design options, and think through edge cases. I considered questions such as how to prevent two simultaneous requests from booking the same vehicle, how to make retries safe, and whether an event broker was needed.
>
> I treated AI output as suggestions to assess, not as proof. I compared layered MVC with Hexagonal Architecture and chose MVC because it gave this assessment a clear structure without adding unnecessary abstraction. I kept booking synchronous, used PostgreSQL transactions and locks for consistency, and did not add a broker because there is no asynchronous consumer in scope.
>
> I remained responsible for the final decisions. I checked them against the code, the acceptance criteria, and the PostgreSQL-backed tests. That process helped me consider alternatives and edge cases while verifying that the final behavior matched the design.”

## 6. Learning, challenges, and close — 6:50–7:40

**On screen:** Show the known-gaps or production-considerations section in the design document.

**Say:**

> “One of the main challenges was handling concurrent bookings. A simple availability check is not enough if two requests can pass the check at the same time. The transaction and row locks, together with the PostgreSQL concurrency tests, address that risk for bookings made through this application.
>
> I also learned to distinguish a retry from a new booking attempt: idempotency handles the retry, while availability rules reject a genuinely conflicting appointment.
>
> This is an assessment implementation, not a production-ready service. Production follow-ups include authentication and authorization, load testing, deployment-grade secrets management, and distributed tracing and dashboards. Thank you for watching.”

## Recording notes

- Speak naturally and pause while switching windows; short pauses are easy to edit.
- If Postman reports a conflict on the first booking, use a fresh local demo database or adjust the generated start time, then rerun the collection.
- Do not show credentials, private customer data, or unrelated desktop notifications.
- The video can be screen-only with narration. No camera or live screen-sharing session is required.
