package com.keyloop.scheduler.controller;

import com.keyloop.scheduler.domain.Appointment;
import com.keyloop.scheduler.dto.AppointmentResponse;
import com.keyloop.scheduler.dto.BookingRequest;
import com.keyloop.scheduler.dto.PageResponse;
import com.keyloop.scheduler.dto.ResourceSummary;
import com.keyloop.scheduler.dto.ServiceTypeResponse;
import com.keyloop.scheduler.service.AppointmentQueryService;
import com.keyloop.scheduler.service.BookingService;
import com.keyloop.scheduler.service.ServiceCatalog;
import java.net.URI;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1")
public class AppointmentController {

    private final BookingService bookingService;
    private final AppointmentQueryService appointmentQueryService;
    private final ServiceCatalog serviceCatalog;

    public AppointmentController(BookingService bookingService,
                                 AppointmentQueryService appointmentQueryService,
                                 ServiceCatalog serviceCatalog) {
        this.bookingService = bookingService;
        this.appointmentQueryService = appointmentQueryService;
        this.serviceCatalog = serviceCatalog;
    }

    @PostMapping("/appointments")
    public ResponseEntity<AppointmentResponse> book(
            @Valid @RequestBody BookingRequest request,
            @RequestHeader("Idempotency-Key") @NotBlank @Size(max = 128) String idempotencyKey) {
        Appointment appointment = bookingService.create(request, idempotencyKey);
        AppointmentResponse response = new AppointmentResponse(appointment);
        return ResponseEntity.created(URI.create("/api/v1/appointments/" + appointment.getId()))
                .body(response);
    }

    @GetMapping("/appointments")
    public PageResponse<AppointmentResponse> getAppointments(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return new PageResponse<>(appointmentQueryService.list(page, size)
                .map(AppointmentResponse::new));
    }

    @GetMapping("/appointments/{appointmentId}")
    public AppointmentResponse getAppointment(@PathVariable String appointmentId) {
        return new AppointmentResponse(appointmentQueryService.get(appointmentId));
    }

    @GetMapping("/service-types")
    public List<ServiceTypeResponse> getServiceTypes() {
        return serviceCatalog.getServiceTypes();
    }

    @GetMapping("/dealerships/{dealershipId}/resources")
    public ResourceSummary getResources(@PathVariable String dealershipId) {
        return serviceCatalog.getResources(dealershipId);
    }
}
