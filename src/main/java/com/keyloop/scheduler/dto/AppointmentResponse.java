package com.keyloop.scheduler.dto;

import com.keyloop.scheduler.domain.Appointment;
import java.time.Instant;

public class AppointmentResponse {

    public final String id;
    public final String dealershipId;
    public final String customerId;
    public final String vehicleId;
    public final String serviceTypeId;
    public final String serviceBayId;
    public final String technicianId;
    public final Instant startAt;
    public final Instant endAt;
    public final String status;

    public AppointmentResponse(Appointment appointment) {
        this.id = appointment.getId();
        this.dealershipId = appointment.getDealershipId();
        this.customerId = appointment.getCustomerId();
        this.vehicleId = appointment.getVehicleId();
        this.serviceTypeId = appointment.getServiceTypeId();
        this.serviceBayId = appointment.getServiceBayId();
        this.technicianId = appointment.getTechnicianId();
        this.startAt = appointment.getStartAt();
        this.endAt = appointment.getEndAt();
        this.status = appointment.getStatus().name();
    }

}
