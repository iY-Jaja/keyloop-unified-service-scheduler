# Runtime Validation Report

**Validated:** 2026-11-04
**Scope:** Scenario A backend in this repository

## Result

| Test level | Test class(es) | Passed | Failed | Errors | Skipped |
|---|---|---:|---:|---:|---:|
| Unit | `BookingServiceTest` | 4 | 0 | 0 | 0 |
| Component | `AppointmentControllerTest` | 3 | 0 | 0 | 0 |
| Integration | `AppointmentApiIntegrationTest`, `PostgreSqlBookingTest` | 16 | 0 | 0 | 0 |
| API E2E | `AppointmentApiE2ETest` | 3 | 0 | 0 | 0 |
| **Total** | **Full Maven suite** | **26** | **0** | **0** | **0** |

The full Maven build completed successfully. The integration and API E2E tests ran against PostgreSQL Testcontainers; none were skipped.

## Validation command

```bash
mvn -Dapi.version=1.40 clean test
```

The `api.version` override was needed for Testcontainers to communicate with the Docker daemon in this environment. Without a compatible Docker API version, container-backed tests cannot be treated as verified.

## Coverage

- **Unit:** request fingerprinting, idempotency replay and key-reuse conflicts, and persistence-error propagation.
- **Component:** controller routing, validation, response and `Location` mapping, pagination metadata, and stable error responses.
- **Integration:** booking with qualified resources, full-duration overlap prevention, back-to-back bookings at interval boundaries, unavailable technician handling, customer/vehicle ownership checks, past appointment rejection, unknown dealership/service type errors, pagination, and concurrent booking/idempotency behavior.
- **API E2E:** booking over HTTP, persistence read-back, paginated listing, idempotent replay, invalid-request handling, and HTTP availability of Swagger UI, its Swagger 2.0 contract, and the UI bundle.

The application does not implement authentication, so tests cover malformed or missing request data rather than authentication failures. It has no customer-facing browser application; Swagger UI is for developer API exploration only, so E2E coverage focuses on REST behavior rather than customer browser workflows.

## Readiness check

The existing Compose API returned HTTP 200 with `{"status":"UP"}` from `http://localhost:8080/actuator/health/readiness` during this validation.

Surefire reports are available under `target/surefire-reports/`.
