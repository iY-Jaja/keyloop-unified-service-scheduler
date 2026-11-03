package com.keyloop.scheduler.persistence;

import com.keyloop.scheduler.entity.JpaAppointmentEntity;
import com.keyloop.scheduler.entity.JpaCustomerEntity;
import com.keyloop.scheduler.entity.JpaDealershipEntity;
import com.keyloop.scheduler.entity.JpaIdempotencyRecordEntity;
import com.keyloop.scheduler.entity.JpaServiceBayEntity;
import com.keyloop.scheduler.entity.JpaServiceTypeEntity;
import com.keyloop.scheduler.entity.JpaTechnicianEntity;
import com.keyloop.scheduler.entity.JpaVehicleEntity;
import com.keyloop.scheduler.domain.Appointment;
import com.keyloop.scheduler.domain.Customer;
import com.keyloop.scheduler.domain.Dealership;
import com.keyloop.scheduler.domain.IdempotencyRecord;
import com.keyloop.scheduler.domain.ServiceBay;
import com.keyloop.scheduler.domain.ServiceType;
import com.keyloop.scheduler.domain.Technician;
import com.keyloop.scheduler.domain.Vehicle;
import org.springframework.stereotype.Component;

@Component
public class JpaEntityMapper {

    public Dealership toDomain(JpaDealershipEntity entity) {
        return new Dealership(entity.getId(), entity.getName(), entity.getTimezone());
    }

    public Customer toDomain(JpaCustomerEntity entity) {
        return new Customer(entity.getId(), entity.getName());
    }

    public Vehicle toDomain(JpaVehicleEntity entity) {
        return new Vehicle(entity.getId(), entity.getCustomer().getId(), entity.getVin(),
                entity.getMake(), entity.getModel());
    }

    public ServiceType toDomain(JpaServiceTypeEntity entity) {
        return new ServiceType(entity.getId(), entity.getName(), entity.getDurationMinutes());
    }

    public ServiceBay toDomain(JpaServiceBayEntity entity) {
        return new ServiceBay(entity.getId(), entity.getDealership().getId(), entity.getName());
    }

    public Technician toDomain(JpaTechnicianEntity entity) {
        return new Technician(entity.getId(), entity.getDealership().getId(), entity.getName());
    }

    public Appointment toDomain(JpaAppointmentEntity entity) {
        return new Appointment.Builder()
                .id(entity.getId())
                .dealershipId(entity.getDealership().getId())
                .customerId(entity.getCustomer().getId())
                .vehicleId(entity.getVehicle().getId())
                .serviceTypeId(entity.getServiceType().getId())
                .serviceBayId(entity.getServiceBay().getId())
                .technicianId(entity.getTechnician().getId())
                .startAt(entity.getStartAt())
                .endAt(entity.getEndAt())
                .status(entity.getStatus())
                .build();
    }

    public IdempotencyRecord toDomain(JpaIdempotencyRecordEntity entity) {
        return new IdempotencyRecord(entity.getId(), entity.getRequestHash(), entity.getAppointmentId());
    }

    public JpaIdempotencyRecordEntity toEntity(IdempotencyRecord record) {
        return new JpaIdempotencyRecordEntity(record.getIdempotencyKey(),
                record.getRequestHash(), record.getAppointmentId());
    }

    public JpaAppointmentEntity toEntity(Appointment appointment,
                                         JpaDealershipEntity dealership,
                                         JpaCustomerEntity customer,
                                         JpaVehicleEntity vehicle,
                                         JpaServiceTypeEntity serviceType,
                                         JpaServiceBayEntity serviceBay,
                                         JpaTechnicianEntity technician) {
        return new JpaAppointmentEntity(appointment.getId(), dealership, customer, vehicle,
                serviceType, serviceBay, technician, appointment.getStartAt(),
                appointment.getEndAt(), appointment.getStatus());
    }
}
