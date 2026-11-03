CREATE TABLE dealership (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    timezone VARCHAR(255) NOT NULL
);

CREATE TABLE customer (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL
);

CREATE TABLE vehicle (
    id VARCHAR(255) PRIMARY KEY,
    customer_id VARCHAR(255) NOT NULL REFERENCES customer(id),
    vin VARCHAR(255) NOT NULL UNIQUE,
    make VARCHAR(255) NOT NULL,
    model VARCHAR(255) NOT NULL,
    model_year INTEGER NOT NULL
);

CREATE TABLE service_type (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    duration_minutes INTEGER NOT NULL CHECK (duration_minutes > 0)
);

CREATE TABLE service_bay (
    id VARCHAR(255) PRIMARY KEY,
    dealership_id VARCHAR(255) NOT NULL REFERENCES dealership(id),
    name VARCHAR(255) NOT NULL
);

CREATE TABLE technician (
    id VARCHAR(255) PRIMARY KEY,
    dealership_id VARCHAR(255) NOT NULL REFERENCES dealership(id),
    name VARCHAR(255) NOT NULL
);

CREATE TABLE technician_service (
    id VARCHAR(255) PRIMARY KEY,
    technician_id VARCHAR(255) NOT NULL REFERENCES technician(id),
    service_type_id VARCHAR(255) NOT NULL REFERENCES service_type(id),
    CONSTRAINT uq_technician_service UNIQUE (technician_id, service_type_id)
);

CREATE TABLE appointment (
    id VARCHAR(255) PRIMARY KEY,
    dealership_id VARCHAR(255) NOT NULL REFERENCES dealership(id),
    customer_id VARCHAR(255) NOT NULL REFERENCES customer(id),
    vehicle_id VARCHAR(255) NOT NULL REFERENCES vehicle(id),
    service_type_id VARCHAR(255) NOT NULL REFERENCES service_type(id),
    service_bay_id VARCHAR(255) NOT NULL REFERENCES service_bay(id),
    technician_id VARCHAR(255) NOT NULL REFERENCES technician(id),
    start_at TIMESTAMP NOT NULL,
    end_at TIMESTAMP NOT NULL,
    status VARCHAR(32) NOT NULL,
    CONSTRAINT ck_appointment_time CHECK (end_at > start_at)
);

CREATE INDEX ix_appointment_bay_window
    ON appointment (dealership_id, service_bay_id, status, start_at, end_at);
CREATE INDEX ix_appointment_technician_window
    ON appointment (dealership_id, technician_id, status, start_at, end_at);
CREATE TABLE idempotency_record (
    idempotency_key VARCHAR(128) PRIMARY KEY,
    request_hash VARCHAR(64) NOT NULL,
    appointment_id VARCHAR(255) NOT NULL
);
