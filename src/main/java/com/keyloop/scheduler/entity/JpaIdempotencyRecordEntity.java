package com.keyloop.scheduler.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import org.springframework.data.domain.Persistable;

@Entity
@Table(name = "idempotency_record")
public class JpaIdempotencyRecordEntity implements Persistable<String> {

    @Id
    @Column(name = "idempotency_key", length = 128)
    private String idempotencyKey;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @Column(name = "appointment_id", nullable = false)
    private String appointmentId;

    protected JpaIdempotencyRecordEntity() {
    }

    public JpaIdempotencyRecordEntity(String idempotencyKey, String requestHash, String appointmentId) {
        this.idempotencyKey = idempotencyKey;
        this.requestHash = requestHash;
        this.appointmentId = appointmentId;
    }

    public String getRequestHash() {
        return requestHash;
    }

    public String getAppointmentId() {
        return appointmentId;
    }

    @Override
    public String getId() {
        return idempotencyKey;
    }

    @Override
    public boolean isNew() {
        return true;
    }
}
