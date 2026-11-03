CREATE INDEX ix_appointment_vehicle_window
    ON appointment (vehicle_id, status, start_at, end_at);
