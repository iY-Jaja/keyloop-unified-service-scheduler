package com.keyloop.scheduler.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.keyloop.scheduler.domain.Appointment;
import com.keyloop.scheduler.dto.BookingRequest;
import com.keyloop.scheduler.entity.JpaIdempotencyRecordEntity;
import com.keyloop.scheduler.exception.IdempotencyConflictException;
import com.keyloop.scheduler.repository.JpaIdempotencyRecordRepository;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    private static final String IDEMPOTENCY_KEY = "booking-unit-test";

    @Mock
    private TransactionalBookingWriter bookingWriter;

    @Mock
    private JpaIdempotencyRecordRepository idempotencyRecordRepository;

    @Mock
    private AppointmentQueryService appointmentQueryService;

    @InjectMocks
    private BookingService bookingService;

    @Test
    void createsAppointmentWithStableRequestFingerprint() {
        BookingRequest request = request();
        Appointment expected = appointment("appointment-unit-test");
        when(bookingWriter.create(eq(request), eq(IDEMPOTENCY_KEY), anyString()))
                .thenReturn(expected);

        Appointment result = bookingService.create(request, IDEMPOTENCY_KEY);

        ArgumentCaptor<String> hash = ArgumentCaptor.forClass(String.class);
        verify(bookingWriter).create(eq(request), eq(IDEMPOTENCY_KEY), hash.capture());
        assertSame(expected, result);
        assertEquals(64, hash.getValue().length());
        org.junit.jupiter.api.Assertions.assertTrue(hash.getValue().matches("[0-9a-f]{64}"));
        verify(idempotencyRecordRepository, never()).findById(IDEMPOTENCY_KEY);
    }

    @Test
    void rethrowsDatabaseFailureWhenNoIdempotencyRecordExists() {
        BookingRequest request = request();
        DataIntegrityViolationException databaseFailure =
                new DataIntegrityViolationException("unrelated integrity failure");
        doThrow(databaseFailure).when(bookingWriter)
                .create(eq(request), eq(IDEMPOTENCY_KEY), anyString());
        when(idempotencyRecordRepository.findById(IDEMPOTENCY_KEY)).thenReturn(Optional.empty());

        assertSame(databaseFailure,
                assertThrows(DataIntegrityViolationException.class,
                        () -> bookingService.create(request, IDEMPOTENCY_KEY)));
        verify(appointmentQueryService, never()).get(anyString());
    }

    @Test
    void rejectsDuplicateKeyWhenRequestFingerprintDoesNotMatch() {
        BookingRequest request = request();
        doThrow(new DataIntegrityViolationException("duplicate key")).when(bookingWriter)
                .create(eq(request), eq(IDEMPOTENCY_KEY), anyString());
        when(idempotencyRecordRepository.findById(IDEMPOTENCY_KEY))
                .thenReturn(Optional.of(new JpaIdempotencyRecordEntity(
                        IDEMPOTENCY_KEY, "different-request-hash", "appointment-original")));

        assertThrows(IdempotencyConflictException.class,
                () -> bookingService.create(request, IDEMPOTENCY_KEY));
        verify(appointmentQueryService, never()).get(anyString());
    }

    @Test
    void replaysAppointmentWhenDuplicateKeyHasSameRequestFingerprint() {
        BookingRequest request = request();
        Appointment expected = appointment("appointment-original");
        doAnswer(invocation -> {
            String requestHash = invocation.getArgument(2);
            when(idempotencyRecordRepository.findById(IDEMPOTENCY_KEY))
                    .thenReturn(Optional.of(new JpaIdempotencyRecordEntity(
                            IDEMPOTENCY_KEY, requestHash, expected.getId())));
            throw new DataIntegrityViolationException("duplicate key");
        }).when(bookingWriter).create(eq(request), eq(IDEMPOTENCY_KEY), anyString());
        when(appointmentQueryService.get(expected.getId())).thenReturn(expected);

        assertSame(expected, bookingService.create(request, IDEMPOTENCY_KEY));
        verify(appointmentQueryService).get(expected.getId());
    }

    private BookingRequest request() {
        BookingRequest request = new BookingRequest();
        request.dealershipId = "dealer-1";
        request.customerId = "customer-1";
        request.vehicleId = "vehicle-1";
        request.serviceTypeId = "oil-change";
        request.startAt = OffsetDateTime.parse("2030-01-15T10:00:00+00:00");
        return request;
    }

    private Appointment appointment(String id) {
        return new Appointment.Builder()
                .id(id)
                .dealershipId("dealer-1")
                .customerId("customer-1")
                .vehicleId("vehicle-1")
                .serviceTypeId("oil-change")
                .serviceBayId("bay-1")
                .technicianId("tech-1")
                .startAt(request().startAt.toInstant())
                .endAt(request().startAt.toInstant().plusSeconds(3600))
                .status(com.keyloop.scheduler.domain.AppointmentStatus.CONFIRMED)
                .build();
    }
}
