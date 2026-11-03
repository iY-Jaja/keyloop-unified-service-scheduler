package com.keyloop.scheduler.entity;

import com.keyloop.scheduler.domain.AppointmentStatus;
import java.time.Instant;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity(name = "Appointment")
@Table(name = "appointment")
public class JpaAppointmentEntity {

    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dealership_id", nullable = false)
    private JpaDealershipEntity dealership;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private JpaCustomerEntity customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private JpaVehicleEntity vehicle;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_type_id", nullable = false)
    private JpaServiceTypeEntity serviceType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_bay_id", nullable = false)
    private JpaServiceBayEntity serviceBay;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "technician_id", nullable = false)
    private JpaTechnicianEntity technician;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "end_at", nullable = false)
    private Instant endAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppointmentStatus status;

    protected JpaAppointmentEntity() {
    }

    public JpaAppointmentEntity(String id, JpaDealershipEntity dealership, JpaCustomerEntity customer,
                                JpaVehicleEntity vehicle, JpaServiceTypeEntity serviceType,
                                JpaServiceBayEntity serviceBay, JpaTechnicianEntity technician,
                                Instant startAt, Instant endAt, AppointmentStatus status) {
        this.id = id;
        this.dealership = dealership;
        this.customer = customer;
        this.vehicle = vehicle;
        this.serviceType = serviceType;
        this.serviceBay = serviceBay;
        this.technician = technician;
        this.startAt = startAt;
        this.endAt = endAt;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public JpaDealershipEntity getDealership() {
        return dealership;
    }

    public JpaCustomerEntity getCustomer() {
        return customer;
    }

    public JpaVehicleEntity getVehicle() {
        return vehicle;
    }

    public JpaServiceTypeEntity getServiceType() {
        return serviceType;
    }

    public JpaServiceBayEntity getServiceBay() {
        return serviceBay;
    }

    public JpaTechnicianEntity getTechnician() {
        return technician;
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
