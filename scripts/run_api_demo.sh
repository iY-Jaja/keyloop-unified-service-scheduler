#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
BASE_URL="${BASE_URL:-http://localhost:8080}"
RESPONSE_FILE="$(mktemp)"
HEADERS_FILE="$(mktemp)"

cleanup() {
    rm -f "$RESPONSE_FILE" "$HEADERS_FILE"
}
trap cleanup EXIT

fail() {
    printf 'ERROR: %s\n' "$*" >&2
    exit 1
}

for command_name in curl jq; do
    command -v "$command_name" >/dev/null 2>&1 ||
        fail "Required command '$command_name' was not found."
done

SKIP_COMPOSE=false
if [[ "${1:-}" == "--skip-compose" ]]; then
    SKIP_COMPOSE=true
elif [[ $# -gt 0 ]]; then
    fail "Usage: $0 [--skip-compose]"
fi

if [[ "$SKIP_COMPOSE" == false ]]; then
    command -v docker >/dev/null 2>&1 ||
        fail "Docker is required. Install Docker Desktop or use --skip-compose with an already running API."
    [[ -f "$ROOT_DIR/.env" ]] ||
        fail "Missing .env. From the project root, run: cp .env.example .env"
    printf 'Starting the local app and PostgreSQL with Docker Compose...\n'
    docker compose -f "$ROOT_DIR/docker-compose.yml" up -d --build
fi

printf 'Waiting for API readiness'
ready=false
for _ in $(seq 1 60); do
    if curl --silent --show-error --fail \
        "$BASE_URL/actuator/health/readiness" \
        --output "$RESPONSE_FILE" 2>/dev/null &&
        jq --exit-status '.status == "UP"' "$RESPONSE_FILE" >/dev/null; then
        ready=true
        break
    fi
    printf '.'
    sleep 2
done
printf '\n'
[[ "$ready" == true ]] || fail "API did not become ready at $BASE_URL."
printf 'API is ready.\n'

api_request() {
    local method="$1"
    local path="$2"
    local body="${3:-}"
    local idempotency_key="${4:-}"
    local curl_args=(
        --silent
        --show-error
        --output "$RESPONSE_FILE"
        --dump-header "$HEADERS_FILE"
        --write-out '%{http_code}'
        --request "$method"
        "$BASE_URL$path"
    )

    if [[ -n "$body" ]]; then
        curl_args+=(--header 'Content-Type: application/json' --data "$body")
    fi
    if [[ -n "$idempotency_key" ]]; then
        curl_args+=(--header "Idempotency-Key: $idempotency_key")
    fi

    HTTP_STATUS="$(curl "${curl_args[@]}")"
}

expect_status() {
    local label="$1"
    local expected="$2"
    if [[ "$HTTP_STATUS" != "$expected" ]]; then
        printf 'Response body: ' >&2
        jq . "$RESPONSE_FILE" >&2 2>/dev/null || cat "$RESPONSE_FILE" >&2
        fail "$label: expected HTTP $expected, received HTTP $HTTP_STATUS."
    fi
}

api_request GET '/api/v1/service-types'
expect_status 'List service types' 200
SERVICE_TYPE_COUNT="$(jq 'length' "$RESPONSE_FILE")"
printf 'Service types: %s\n' "$SERVICE_TYPE_COUNT"

api_request GET '/api/v1/dealerships/dealer-1/resources'
expect_status 'List dealership resources' 200
jq -r '"Dealer " + .dealershipId + ": " + (.serviceBays | length | tostring) + " bays, " + (.technicians | length | tostring) + " technicians"' "$RESPONSE_FILE"

RANDOM_DAYS=$((365 + RANDOM % 5000))
START_AT="$(date -u -v+"${RANDOM_DAYS}"d '+%Y-%m-%dT%H:%M:00Z')"
IDEMPOTENCY_KEY="$(uuidgen)"
BOOKING_BODY="$(jq -cn \
    --arg startAt "$START_AT" \
    '{dealershipId:"dealer-1",customerId:"customer-1",vehicleId:"vehicle-1",serviceTypeId:"oil-change",startAt:$startAt}')"

api_request POST '/api/v1/appointments' "$BOOKING_BODY" "$IDEMPOTENCY_KEY"
expect_status 'Create appointment' 201
APPOINTMENT_ID="$(jq -er '.id' "$RESPONSE_FILE")"
SERVICE_BAY_ID="$(jq -er '.serviceBayId' "$RESPONSE_FILE")"
TECHNICIAN_ID="$(jq -er '.technicianId' "$RESPONSE_FILE")"
grep -qi '^Location:' "$HEADERS_FILE" || fail 'Create appointment did not return a Location header.'
printf 'Created appointment %s: bay %s, technician %s, start %s\n' \
    "$APPOINTMENT_ID" "$SERVICE_BAY_ID" "$TECHNICIAN_ID" "$START_AT"

api_request POST '/api/v1/appointments' "$BOOKING_BODY" "$IDEMPOTENCY_KEY"
expect_status 'Retry booking with same key' 201
REPLAY_ID="$(jq -er '.id' "$RESPONSE_FILE")"
[[ "$REPLAY_ID" == "$APPOINTMENT_ID" ]] ||
    fail 'Idempotent retry returned a different appointment ID.'
printf 'Idempotent retry returned the same appointment.\n'

CONFLICT_KEY="$(uuidgen)"
api_request POST '/api/v1/appointments' "$BOOKING_BODY" "$CONFLICT_KEY"
expect_status 'Overlapping booking with new key' 409
CONFLICT_CODE="$(jq -er '.code' "$RESPONSE_FILE")"
[[ "$CONFLICT_CODE" == 'VEHICLE_ALREADY_BOOKED' ]] ||
    fail "Expected VEHICLE_ALREADY_BOOKED, received $CONFLICT_CODE."
printf 'Overlapping booking with a new key was rejected as expected.\n'

api_request GET "/api/v1/appointments/$APPOINTMENT_ID"
expect_status 'Read back appointment' 200
[[ "$(jq -er '.id' "$RESPONSE_FILE")" == "$APPOINTMENT_ID" ]] ||
    fail 'Appointment read-back returned a different ID.'
printf 'Appointment read-back succeeded.\n'

api_request GET '/api/v1/appointments?page=0&size=20'
expect_status 'List appointments' 200
jq --exit-status --arg id "$APPOINTMENT_ID" \
    'any(.content[]; .id == $id)' "$RESPONSE_FILE" >/dev/null ||
    fail 'Created appointment was not found on the first page.'
TOTAL_ELEMENTS="$(jq -er '.totalElements' "$RESPONSE_FILE")"
printf 'Appointment found in page 0 (%s total appointments).\n' "$TOTAL_ELEMENTS"

printf 'API demo completed successfully.\n'
