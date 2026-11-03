package com.keyloop.scheduler.domain;

import java.time.Instant;

public class Appointment {

    private final String id;
    private final String dealershipId;
    private final String customerId;
    private final String vehicleId;
    private final String serviceTypeId;
    private final String serviceBayId;
    private final String technicianId;
    private final Instant startAt;
    private final Instant endAt;
    private final AppointmentStatus status;

    public Appointment(Builder builder) {
        this.id = builder.id;
        this.dealershipId = builder.dealershipId;
        this.customerId = builder.customerId;
        this.vehicleId = builder.vehicleId;
        this.serviceTypeId = builder.serviceTypeId;
        this.serviceBayId = builder.serviceBayId;
        this.technicianId = builder.technicianId;
        this.startAt = builder.startAt;
        this.endAt = builder.endAt;
        this.status = builder.status;
    }

    public static class Builder {
        private String id;
        private String dealershipId;
        private String customerId;
        private String vehicleId;
        private String serviceTypeId;
        private String serviceBayId;
        private String technicianId;
        private Instant startAt;
        private Instant endAt;
        private AppointmentStatus status;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder dealershipId(String dealershipId) {
            this.dealershipId = dealershipId;
            return this;
        }

        public Builder customerId(String customerId) {
            this.customerId = customerId;
            return this;
        }

        public Builder vehicleId(String vehicleId) {
            this.vehicleId = vehicleId;
            return this;
        }

        public Builder serviceTypeId(String serviceTypeId) {
            this.serviceTypeId = serviceTypeId;
            return this;
        }

        public Builder serviceBayId(String serviceBayId) {
            this.serviceBayId = serviceBayId;
            return this;
        }

        public Builder technicianId(String technicianId) {
            this.technicianId = technicianId;
            return this;
        }

        public Builder startAt(Instant startAt) {
            this.startAt = startAt;
            return this;
        }

        public Builder endAt(Instant endAt) {
            this.endAt = endAt;
            return this;
        }

        public Builder status(AppointmentStatus status) {
            this.status = status;
            return this;
        }

        public Appointment build() {
            return new Appointment(this);
        }
    }

    public String getId() {
        return id;
    }

    public String getDealershipId() {
        return dealershipId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getVehicleId() {
        return vehicleId;
    }

    public String getServiceTypeId() {
        return serviceTypeId;
    }

    public String getServiceBayId() {
        return serviceBayId;
    }

    public String getTechnicianId() {
        return technicianId;
    }

    public Instant getStartAt() {
        return startAt;
    }

    public Instant getEndAt() {
        return endAt;
    }

    public AppointmentStatus getStatus() {
        return status;
    }
}
