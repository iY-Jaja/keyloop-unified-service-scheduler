package com.keyloop.scheduler;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
class AppointmentApiIntegrationTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void booksQualifiedTechnicianAndBayForFullServiceDuration() throws Exception {
        String request = bookingRequest(OffsetDateTime.now(ZoneOffset.UTC).plusDays(1));
        String key = UUID.randomUUID().toString();

        MvcResult result = mockMvc.perform(post("/api/v1/appointments")
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.status", is("CONFIRMED")))
                .andExpect(jsonPath("$.serviceBayId", notNullValue()))
                .andExpect(jsonPath("$.technicianId", equalTo("tech-1")))
                .andReturn();

        String appointmentId = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id").asText();
        mockMvc.perform(get("/api/v1/appointments/{id}", appointmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", equalTo(appointmentId)));
        mockMvc.perform(get("/actuator/metrics/scheduler.application.operation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", equalTo("scheduler.application.operation")));
    }

    @Test
    void replayReturnsSameAppointmentAndChangedPayloadConflicts() throws Exception {
        OffsetDateTime start = OffsetDateTime.now(ZoneOffset.UTC).plusDays(2);
        String key = UUID.randomUUID().toString();
        MvcResult first = mockMvc.perform(post("/api/v1/appointments")
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingRequest(start)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode firstBody = objectMapper.readTree(first.getResponse().getContentAsString());

        MvcResult replay = mockMvc.perform(post("/api/v1/appointments")
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingRequest(start)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode replayBody = objectMapper.readTree(replay.getResponse().getContentAsString());
        org.junit.jupiter.api.Assertions.assertEquals(firstBody.get("id"), replayBody.get("id"));

        mockMvc.perform(post("/api/v1/appointments")
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingRequest(start.plusHours(1))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", equalTo("IDEMPOTENCY_KEY_REUSED")));
    }

    @Test
    void overlappingBookingForSameVehicleIsRejected() throws Exception {
        OffsetDateTime start = OffsetDateTime.now(ZoneOffset.UTC).plusDays(3);
        mockMvc.perform(post("/api/v1/appointments")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingRequest(start)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/appointments")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingRequest(start.plusMinutes(30), "inspection")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", equalTo("VEHICLE_ALREADY_BOOKED")));
    }

    @Test
    void concurrentRequestsCannotDoubleBookTheSameVehicle() throws Exception {
        OffsetDateTime start = OffsetDateTime.now(ZoneOffset.UTC).plusDays(4);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch startTogether = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Callable<Integer> booking = () -> {
                ready.countDown();
                startTogether.await();
                return mockMvc.perform(post("/api/v1/appointments")
                                .header("Idempotency-Key", UUID.randomUUID().toString())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(bookingRequest(start)))
                        .andReturn().getResponse().getStatus();
            };
            Future<Integer> first = executor.submit(booking);
            Future<Integer> second = executor.submit(booking);
            ready.await();
            startTogether.countDown();

            int firstStatus = first.get();
            int secondStatus = second.get();
            org.junit.jupiter.api.Assertions.assertTrue(
                    (firstStatus == 201 && secondStatus == 409)
                            || (firstStatus == 409 && secondStatus == 201));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void exposesCachedReferenceData() throws Exception {
        mockMvc.perform(get("/api/v1/service-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id", not(equalTo(""))));
        mockMvc.perform(get("/api/v1/dealerships/dealer-1/resources"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceBays.length()", equalTo(2)));
    }

    @Test
    void listsAppointmentsWithPaginationAndReflectsNewBookings() throws Exception {
        mockMvc.perform(post("/api/v1/appointments")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingRequest(OffsetDateTime.now(ZoneOffset.UTC).plusDays(30))))
                .andExpect(status().isCreated());

        MvcResult before = mockMvc.perform(get("/api/v1/appointments").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page", equalTo(0)))
                .andExpect(jsonPath("$.size", equalTo(1)))
                .andExpect(jsonPath("$.content.length()", equalTo(1)))
                .andReturn();
        long totalBefore = objectMapper.readTree(before.getResponse().getContentAsString())
                .get("totalElements").asLong();

        mockMvc.perform(post("/api/v1/appointments")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingRequest(OffsetDateTime.now(ZoneOffset.UTC).plusDays(31))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/appointments").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size", equalTo(1)))
                .andExpect(jsonPath("$.totalElements", equalTo((int) totalBefore + 1)))
                .andExpect(jsonPath("$.totalPages", equalTo((int) totalBefore + 1)));

        mockMvc.perform(get("/api/v1/appointments").param("page", "0").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", equalTo("INVALID_REQUEST")));
        mockMvc.perform(get("/api/v1/appointments").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", equalTo("INVALID_REQUEST")));
    }

    @Test
    void exposesAdditionalDemoCatalogAndDealershipResources() throws Exception {
        mockMvc.perform(get("/api/v1/service-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", equalTo(13)))
                .andExpect(jsonPath("$[*].id", hasItems(
                        "tire-rotation", "brake-service", "battery-check",
                        "coolant-flush", "transmission-service", "wheel-alignment",
                        "air-conditioning-service", "engine-diagnostic", "windshield-repair",
                        "spark-plug-replacement", "safety-inspection")));

        mockMvc.perform(get("/api/v1/dealerships/dealer-2/resources"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceBays.length()", equalTo(2)))
                .andExpect(jsonPath("$.technicians.length()", equalTo(2)));
    }

    private String bookingRequest(OffsetDateTime startAt) throws Exception {
        return bookingRequest(startAt, "oil-change");
    }

    private String bookingRequest(OffsetDateTime startAt, String serviceTypeId) throws Exception {
        return objectMapper.writeValueAsString(new BookingPayload(startAt, serviceTypeId));
    }

    public static class BookingPayload {
        public String dealershipId = "dealer-1";
        public String customerId = "customer-1";
        public String vehicleId = "vehicle-1";
        public String serviceTypeId;
        public OffsetDateTime startAt;

        public BookingPayload(OffsetDateTime startAt) {
            this(startAt, "oil-change");
        }

        public BookingPayload(OffsetDateTime startAt, String serviceTypeId) {
            this.startAt = startAt;
            this.serviceTypeId = serviceTypeId;
        }
    }
}
