# Runtime Validation Report

**Generated:** 2026-11-03T22:51:01+07:00  
**Target:** `/Users/giap92446/Downloads/keyloop`

## Summary

| Test level | Status | Exit code | Results |
|---|---|---:|---|
| Startup/readiness | PASS | 0 | Existing Compose API readiness endpoint returned `{"status":"UP"}` |
| Unit | PASS | 0 | `BookingServiceTest`: 4 passed |
| Component | PASS | 0 | `AppointmentControllerTest`: 3 passed |
| Integration | PASS | 0 | `AppointmentApiIntegrationTest` and `PostgreSqlBookingTest`: 10 passed against PostgreSQL Testcontainers |
| API E2E | PASS | 0 | Random-port HTTP E2E suite: 2 passed against PostgreSQL Testcontainers |
| Full Maven suite | PASS | 0 | 19 tests passed, 0 failures, 0 errors, 0 skipped |

## Environment

- Docker CLI and daemon: available; Docker Engine 29.9.1 reports API version 1.56 and minimum API version 1.40.
- Docker daemon through Testcontainers: available with Maven system property `-Dapi.version=1.40`. Without the override, Testcontainers' Docker client requested API v1.32 and received HTTP 400 because that version is below the daemon's minimum.
- Node.js: available (`v24.21.0`).
- Browser E2E: not applicable; this project is a REST API and has no browser UI.
- API readiness: `http://localhost:8080/actuator/health/readiness` returned HTTP 200 and `{"status":"UP"}` for the already-running Compose service.

## Test levels added

| Test class | Level | Coverage |
|---|---|---|
| `BookingServiceTest` | Unit | Request fingerprinting, idempotency replay, changed-payload conflict, and propagation of unrelated persistence errors |
| `AppointmentControllerTest` | Component | MVC routing, request validation, response/Location mapping, page metadata, and stable error response |
| `AppointmentApiIntegrationTest` | Integration | Full Spring context and MockMvc against PostgreSQL Testcontainers |
| `PostgreSqlBookingTest` | Integration | Concurrent bookings across dealerships and idempotency races against PostgreSQL Testcontainers |
| `AppointmentApiE2ETest` | API E2E | Real random-port HTTP booking, persistence read-back, paginated listing, idempotent replay, and invalid-request handling |

The assessment service does not implement authentication, so tests verify malformed requests and missing idempotency headers instead of authentication failures.

## Commands

```bash
mvn -Dapi.version=1.40 -Dtest=BookingServiceTest,AppointmentControllerTest test
mvn -Dapi.version=1.40 -Dtest=AppointmentApiIntegrationTest,PostgreSqlBookingTest test
mvn -Dapi.version=1.40 -Dtest=AppointmentApiE2ETest test
mvn -Dapi.version=1.40 clean test
```

Surefire reports are generated under `target/surefire-reports/`.
