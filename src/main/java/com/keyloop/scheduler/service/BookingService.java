package com.keyloop.scheduler.service;

import com.keyloop.scheduler.domain.Appointment;
import com.keyloop.scheduler.domain.IdempotencyRecord;
import com.keyloop.scheduler.dto.BookingRequest;
import com.keyloop.scheduler.exception.IdempotencyConflictException;
import com.keyloop.scheduler.observability.ObservedOperation;
import com.keyloop.scheduler.repository.JpaIdempotencyRecordRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class BookingService {

    private final TransactionalBookingWriter bookingWriter;
    private final JpaIdempotencyRecordRepository idempotencyRecordRepository;
    private final AppointmentQueryService appointmentQueryService;

    public BookingService(TransactionalBookingWriter bookingWriter,
                          JpaIdempotencyRecordRepository idempotencyRecordRepository,
                          AppointmentQueryService appointmentQueryService) {
        this.bookingWriter = bookingWriter;
        this.idempotencyRecordRepository = idempotencyRecordRepository;
        this.appointmentQueryService = appointmentQueryService;
    }

    @ObservedOperation("appointment-booking")
    public Appointment create(BookingRequest request, String idempotencyKey) {
        String requestHash = hashRequest(request);
        try {
            return bookingWriter.create(request, idempotencyKey, requestHash);
        } catch (DataIntegrityViolationException exception) {
            IdempotencyRecord existing = idempotencyRecordRepository.findById(idempotencyKey)
                    .map(record -> new IdempotencyRecord(record.getId(), record.getRequestHash(),
                            record.getAppointmentId()))
                    .orElse(null);
            if (existing == null) {
                throw exception;
            }
            if (!existing.getRequestHash().equals(requestHash)) {
                throw new IdempotencyConflictException();
            }
            return appointmentQueryService.get(existing.getAppointmentId());
        }
    }

    private String hashRequest(BookingRequest request) {
        String[] parts = {
                request.dealershipId,
                request.customerId,
                request.vehicleId,
                request.serviceTypeId,
                request.startAt.toInstant().toString()
        };
        StringBuilder canonical = new StringBuilder();
        for (String part : parts) {
            canonical.append(part.length()).append(':').append(part);
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(64);
            for (byte value : digest) {
                hex.append(String.format("%02x", value & 0xff));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available.", exception);
        }
    }
}
