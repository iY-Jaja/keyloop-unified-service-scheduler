package com.keyloop.scheduler.service;

import com.keyloop.scheduler.dto.BookingRequest;
import com.keyloop.scheduler.entity.JpaIdempotencyRecordEntity;
import com.keyloop.scheduler.persistence.JpaEntityMapper;
import com.keyloop.scheduler.repository.JpaAppointmentRepository;
import com.keyloop.scheduler.repository.JpaCustomerRepository;
import com.keyloop.scheduler.repository.JpaDealershipRepository;
import com.keyloop.scheduler.repository.JpaIdempotencyRecordRepository;
import com.keyloop.scheduler.repository.JpaServiceBayRepository;
import com.keyloop.scheduler.repository.JpaServiceTypeRepository;
import com.keyloop.scheduler.repository.JpaTechnicianRepository;
import com.keyloop.scheduler.repository.JpaTechnicianServiceRepository;
import com.keyloop.scheduler.repository.JpaVehicleRepository;
import com.keyloop.scheduler.domain.Appointment;
import com.keyloop.scheduler.domain.AppointmentStatus;
import com.keyloop.scheduler.domain.Customer;
import com.keyloop.scheduler.domain.Dealership;
import com.keyloop.scheduler.domain.IdempotencyRecord;
import com.keyloop.scheduler.domain.ServiceBay;
import com.keyloop.scheduler.domain.ServiceType;
import com.keyloop.scheduler.domain.Technician;
import com.keyloop.scheduler.domain.Vehicle;
import com.keyloop.scheduler.exception.IdempotencyConflictException;
import com.keyloop.scheduler.exception.NotFoundException;
import com.keyloop.scheduler.exception.SlotUnavailableException;
import com.keyloop.scheduler.exception.VehicleAlreadyBookedException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionalBookingWriter {

    private final JpaDealershipRepository dealershipRepository;
    private final JpaCustomerRepository customerRepository;
    private final JpaVehicleRepository vehicleRepository;
    private final JpaServiceTypeRepository serviceTypeRepository;
    private final JpaServiceBayRepository serviceBayRepository;
    private final JpaTechnicianRepository technicianRepository;
    private final JpaTechnicianServiceRepository technicianServiceRepository;
    private final JpaAppointmentRepository appointmentRepository;
    private final JpaIdempotencyRecordRepository idempotencyRecordRepository;
    private final JpaEntityMapper mapper;

    public TransactionalBookingWriter(JpaDealershipRepository dealershipRepository,
                                      JpaCustomerRepository customerRepository,
                                      JpaVehicleRepository vehicleRepository,
                                      JpaServiceTypeRepository serviceTypeRepository,
                                      JpaServiceBayRepository serviceBayRepository,
                                      JpaTechnicianRepository technicianRepository,
                                      JpaTechnicianServiceRepository technicianServiceRepository,
                                      JpaAppointmentRepository appointmentRepository,
                                      JpaIdempotencyRecordRepository idempotencyRecordRepository,
                                      JpaEntityMapper mapper) {
        this.dealershipRepository = dealershipRepository;
        this.customerRepository = customerRepository;
        this.vehicleRepository = vehicleRepository;
        this.serviceTypeRepository = serviceTypeRepository;
        this.serviceBayRepository = serviceBayRepository;
        this.technicianRepository = technicianRepository;
        this.technicianServiceRepository = technicianServiceRepository;
        this.appointmentRepository = appointmentRepository;
        this.idempotencyRecordRepository = idempotencyRecordRepository;
        this.mapper = mapper;
    }

    @Transactional
    public Appointment create(BookingRequest request, String idempotencyKey, String requestHash) {
        Dealership dealership = dealershipRepository.findByIdForUpdate(request.dealershipId)
                .map(mapper::toDomain)
                .orElseThrow(() -> new NotFoundException("Dealership not found."));
        IdempotencyRecord existing = idempotencyRecordRepository.findById(idempotencyKey)
                .map(record -> new IdempotencyRecord(record.getId(), record.getRequestHash(),
                        record.getAppointmentId()))
                .orElse(null);
        if (existing != null) {
            if (!existing.getRequestHash().equals(requestHash)) {
                throw new IdempotencyConflictException();
            }
            return appointmentRepository.findById(existing.getAppointmentId())
                    .map(mapper::toDomain)
                    .orElseThrow(() -> new IllegalStateException(
                            "Idempotency record references a missing appointment."));
        }

        Customer customer = customerRepository.findById(request.customerId)
                .map(mapper::toDomain)
                .orElseThrow(() -> new NotFoundException("Customer not found."));
        Vehicle vehicle = vehicleRepository.findByIdForUpdate(request.vehicleId)
                .map(mapper::toDomain)
                .orElseThrow(() -> new NotFoundException("Vehicle not found."));
        ServiceType serviceType = serviceTypeRepository.findById(request.serviceTypeId)
                .map(mapper::toDomain)
                .orElseThrow(() -> new NotFoundException("Service type not found."));

        if (!customer.getId().equals(vehicle.getCustomerId())) {
            throw new IllegalArgumentException("The selected vehicle does not belong to the customer.");
        }

        Instant startAt = request.startAt.toInstant();
        if (!startAt.isAfter(Instant.now())) {
            throw new IllegalArgumentException("Appointment start time must be in the future.");
        }
        Instant endAt = startAt.plusSeconds(serviceType.getDurationMinutes() * 60L);
        if (appointmentRepository.existsVehicleConflict(
                AppointmentStatus.CONFIRMED, vehicle.getId(), startAt, endAt)) {
            throw new VehicleAlreadyBookedException();
        }
        String appointmentId = UUID.randomUUID().toString();

        idempotencyRecordRepository.saveAndFlush(
                new JpaIdempotencyRecordEntity(idempotencyKey, requestHash, appointmentId));

        ServiceBay availableBay = serviceBayRepository.findByDealership_IdOrderByIdAsc(dealership.getId())
                .stream()
                .map(mapper::toDomain)
                .filter(bay -> !appointmentRepository.existsBayConflict(
                        AppointmentStatus.CONFIRMED, dealership.getId(), bay.getId(), startAt, endAt))
                .findFirst()
                .orElseThrow(() -> new SlotUnavailableException("No service bay is available for this time."));

        List<Technician> qualifiedTechnicians = technicianServiceRepository
                .findQualifiedTechnicians(dealership.getId(), serviceType.getId()).stream()
                .map(mapper::toDomain)
                .collect(java.util.stream.Collectors.toList());
        Technician availableTechnician = qualifiedTechnicians.stream()
                .filter(technician -> !appointmentRepository.existsTechnicianConflict(
                        AppointmentStatus.CONFIRMED, dealership.getId(), technician.getId(), startAt, endAt))
                .findFirst()
                .orElseThrow(() -> new SlotUnavailableException(
                        "No qualified technician is available for this time."));

        Appointment appointment = new Appointment.Builder()
                .id(appointmentId)
                .dealershipId(dealership.getId())
                .customerId(customer.getId())
                .vehicleId(vehicle.getId())
                .serviceTypeId(serviceType.getId())
                .serviceBayId(availableBay.getId())
                .technicianId(availableTechnician.getId())
                .startAt(startAt)
                .endAt(endAt)
                .status(AppointmentStatus.CONFIRMED)
                .build();
        return mapper.toDomain(appointmentRepository.save(mapper.toEntity(appointment,
                dealershipRepository.getReferenceById(appointment.getDealershipId()),
                customerRepository.getReferenceById(appointment.getCustomerId()),
                vehicleRepository.getReferenceById(appointment.getVehicleId()),
                serviceTypeRepository.getReferenceById(appointment.getServiceTypeId()),
                serviceBayRepository.getReferenceById(appointment.getServiceBayId()),
                technicianRepository.getReferenceById(appointment.getTechnicianId()))));
    }
}
