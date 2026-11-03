package com.keyloop.scheduler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
class PostgreSqlBookingTest {

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

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void addSecondDealership() {
        jdbcTemplate.update("INSERT INTO dealership (id, name, timezone) " +
                "VALUES ('dealer-2', 'Second Demo Motors', 'UTC') ON CONFLICT (id) DO NOTHING");
        jdbcTemplate.update("INSERT INTO service_bay (id, dealership_id, name) " +
                "VALUES ('bay-3', 'dealer-2', 'Bay 3') ON CONFLICT (id) DO NOTHING");
        jdbcTemplate.update("INSERT INTO technician (id, dealership_id, name) " +
                "VALUES ('tech-3', 'dealer-2', 'Morgan Reed') ON CONFLICT (id) DO NOTHING");
        jdbcTemplate.update("INSERT INTO technician_service (id, technician_id, service_type_id) " +
                "VALUES ('qual-4', 'tech-3', 'oil-change') ON CONFLICT (id) DO NOTHING");
    }

    @Test
    void simultaneousBookingsAtDifferentDealershipsCannotOverlapForOneVehicle() throws Exception {
        OffsetDateTime start = OffsetDateTime.now(ZoneOffset.UTC).plusDays(30);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch startTogether = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<BookingResult> first = executor.submit(
                    booking("dealer-1", UUID.randomUUID().toString(), start, ready, startTogether));
            Future<BookingResult> second = executor.submit(
                    booking("dealer-2", UUID.randomUUID().toString(), start, ready, startTogether));
            ready.await();
            startTogether.countDown();

            BookingResult firstResult = first.get();
            BookingResult secondResult = second.get();
            assertTrue((firstResult.status == 201 && secondResult.status == 409)
                    || (firstResult.status == 409 && secondResult.status == 201));
            BookingResult rejected = firstResult.status == 409 ? firstResult : secondResult;
            assertEquals("VEHICLE_ALREADY_BOOKED", rejected.code);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void simultaneousRetriesWithSameIdempotencyKeyReturnTheSameAppointment() throws Exception {
        OffsetDateTime start = OffsetDateTime.now(ZoneOffset.UTC).plusDays(31);
        String key = UUID.randomUUID().toString();
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch startTogether = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<BookingResult> first = executor.submit(
                    booking("dealer-1", key, start, ready, startTogether));
            Future<BookingResult> second = executor.submit(
                    booking("dealer-1", key, start, ready, startTogether));
            ready.await();
            startTogether.countDown();

            BookingResult firstResult = first.get();
            BookingResult secondResult = second.get();
            assertEquals(201, firstResult.status);
            assertEquals(201, secondResult.status);
            assertEquals(firstResult.appointmentId, secondResult.appointmentId);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void reusingAnIdempotencyKeyWithDifferentPayloadConflicts() throws Exception {
        OffsetDateTime start = OffsetDateTime.now(ZoneOffset.UTC).plusDays(32);
        String key = UUID.randomUUID().toString();
        mockMvc.perform(post("/api/v1/appointments")
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BookingPayload("dealer-1", start))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/appointments")
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new BookingPayload("dealer-1", start.plusHours(1)))))
                .andExpect(status().isConflict())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .jsonPath("$.code").value("IDEMPOTENCY_KEY_REUSED"));
    }

    private Callable<BookingResult> booking(String dealershipId, String idempotencyKey,
                                            OffsetDateTime start, CountDownLatch ready,
                                            CountDownLatch startTogether) {
        return () -> {
            ready.countDown();
            startTogether.await();
            String payload = objectMapper.writeValueAsString(
                    new BookingPayload(dealershipId, start));
            MvcResult result = mockMvc.perform(post("/api/v1/appointments")
                            .header("Idempotency-Key", idempotencyKey)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andReturn();
            JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
            return new BookingResult(result.getResponse().getStatus(),
                    body.has("code") ? body.get("code").asText() : null,
                    body.has("id") ? body.get("id").asText() : null);
        };
    }

    public static class BookingPayload {
        public String dealershipId;
        public String customerId = "customer-1";
        public String vehicleId = "vehicle-1";
        public String serviceTypeId = "oil-change";
        public OffsetDateTime startAt;

        BookingPayload(String dealershipId, OffsetDateTime startAt) {
            this.dealershipId = dealershipId;
            this.startAt = startAt;
        }
    }

    private static class BookingResult {
        private final int status;
        private final String code;
        private final String appointmentId;

        BookingResult(int status, String code, String appointmentId) {
            this.status = status;
            this.code = code;
            this.appointmentId = appointmentId;
        }
    }
}
