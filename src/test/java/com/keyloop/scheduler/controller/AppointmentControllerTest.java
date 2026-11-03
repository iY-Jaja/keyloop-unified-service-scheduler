package com.keyloop.scheduler.controller;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.keyloop.scheduler.domain.Appointment;
import com.keyloop.scheduler.domain.AppointmentStatus;
import com.keyloop.scheduler.service.AppointmentQueryService;
import com.keyloop.scheduler.service.BookingService;
import com.keyloop.scheduler.service.ServiceCatalog;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AppointmentController.class)
class AppointmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BookingService bookingService;

    @MockBean
    private AppointmentQueryService appointmentQueryService;

    @MockBean
    private ServiceCatalog serviceCatalog;

    @Test
    void bookingEndpointMapsRequestToCreatedResponseAndLocation() throws Exception {
        Appointment appointment = appointment();
        when(bookingService.create(org.mockito.ArgumentMatchers.any(), eq("unit-key")))
                .thenReturn(appointment);

        mockMvc.perform(post("/api/v1/appointments")
                        .header("Idempotency-Key", "unit-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location",
                        "/api/v1/appointments/component-appointment"))
                .andExpect(jsonPath("$.id", equalTo("component-appointment")))
                .andExpect(jsonPath("$.status", is("CONFIRMED")));

        verify(bookingService).create(org.mockito.ArgumentMatchers.any(), eq("unit-key"));
    }

    @Test
    void rejectsMissingIdempotencyHeaderWithStableErrorBody() throws Exception {
        mockMvc.perform(post("/api/v1/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", equalTo("INVALID_REQUEST")));
    }

    @Test
    void appointmentListMapsPageMetadataAndValidatesBounds() throws Exception {
        Page<Appointment> appointments = new PageImpl<>(
                Collections.singletonList(appointment()), PageRequest.of(0, 1), 1);
        when(appointmentQueryService.list(0, 1)).thenReturn(appointments);

        mockMvc.perform(get("/api/v1/appointments").param("page", "0").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id", equalTo("component-appointment")))
                .andExpect(jsonPath("$.totalElements", equalTo(1)))
                .andExpect(jsonPath("$.totalPages", equalTo(1)));

        mockMvc.perform(get("/api/v1/appointments").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", equalTo("INVALID_REQUEST")));
    }

    private String bookingJson() {
        return "{\"dealershipId\":\"dealer-1\",\"customerId\":\"customer-1\","
                + "\"vehicleId\":\"vehicle-1\",\"serviceTypeId\":\"oil-change\","
                + "\"startAt\":\"2030-01-15T10:00:00Z\"}";
    }

    private Appointment appointment() {
        Instant startAt = OffsetDateTime.parse("2030-01-15T10:00:00Z").toInstant();
        return new Appointment.Builder()
                .id("component-appointment")
                .dealershipId("dealer-1")
                .customerId("customer-1")
                .vehicleId("vehicle-1")
                .serviceTypeId("oil-change")
                .serviceBayId("bay-1")
                .technicianId("tech-1")
                .startAt(startAt)
                .endAt(startAt.plusSeconds(3600))
                .status(AppointmentStatus.CONFIRMED)
                .build();
    }
}
