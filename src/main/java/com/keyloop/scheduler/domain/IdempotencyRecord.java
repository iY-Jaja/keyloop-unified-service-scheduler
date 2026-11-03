package com.keyloop.scheduler.domain;

public class IdempotencyRecord {

    private final String idempotencyKey;
    private final String requestHash;
    private final String appointmentId;

    public IdempotencyRecord(String idempotencyKey, String requestHash, String appointmentId) {
        this.idempotencyKey = idempotencyKey;
        this.requestHash = requestHash;
        this.appointmentId = appointmentId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getRequestHash() {
        return requestHash;
    }

    public String getAppointmentId() {
        return appointmentId;
    }
}
