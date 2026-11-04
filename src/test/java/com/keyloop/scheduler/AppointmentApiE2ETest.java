package com.keyloop.scheduler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class AppointmentApiE2ETest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void servesSwaggerUiAndSwaggerTwoContract() {
        String baseUrl = "http://localhost:" + port;

        ResponseEntity<String> swaggerUi = restTemplate.getForEntity(
                baseUrl + "/swagger-ui/index.html", String.class);
        assertEquals(HttpStatus.OK, swaggerUi.getStatusCode());
        assertTrue(swaggerUi.getBody().contains("/openapi.yaml"));

        ResponseEntity<String> contract = restTemplate.getForEntity(
                baseUrl + "/openapi.yaml", String.class);
        assertEquals(HttpStatus.OK, contract.getStatusCode());
        assertTrue(contract.getBody().startsWith("swagger: '2.0'"));

        ResponseEntity<Void> swaggerBundle = restTemplate.exchange(
                baseUrl + "/webjars/swagger-ui/5.17.14/swagger-ui-bundle.js",
                HttpMethod.HEAD,
                null,
                Void.class);
        assertEquals(HttpStatus.OK, swaggerBundle.getStatusCode());
    }

    @Test
    void booksAppointmentThenReadsItAndFindsItInPaginatedList() throws Exception {
        String baseUrl = "http://localhost:" + port + "/api/v1";
        String idempotencyKey = UUID.randomUUID().toString();
        String startAt = OffsetDateTime.now(ZoneOffset.UTC).plusDays(60).toString();
        String payload = "{\"dealershipId\":\"dealer-1\",\"customerId\":\"customer-1\","
                + "\"vehicleId\":\"vehicle-1\",\"serviceTypeId\":\"oil-change\","
                + "\"startAt\":\"" + startAt + "\"}";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Idempotency-Key", idempotencyKey);

        ResponseEntity<String> created = restTemplate.postForEntity(
                baseUrl + "/appointments", new HttpEntity<>(payload, headers), String.class);
        assertEquals(HttpStatus.CREATED, created.getStatusCode());
        assertNotNull(created.getHeaders().getLocation());

        JsonNode createdBody = objectMapper.readTree(created.getBody());
        String appointmentId = createdBody.get("id").asText();
        assertEquals("CONFIRMED", createdBody.get("status").asText());
        assertTrue(createdBody.hasNonNull("serviceBayId"));
        assertTrue(createdBody.hasNonNull("technicianId"));

        ResponseEntity<String> readBack = restTemplate.getForEntity(
                baseUrl + "/appointments/" + appointmentId, String.class);
        assertEquals(HttpStatus.OK, readBack.getStatusCode());
        assertEquals(appointmentId,
                objectMapper.readTree(readBack.getBody()).get("id").asText());

        ResponseEntity<String> page = restTemplate.getForEntity(
                baseUrl + "/appointments?page=0&size=100", String.class);
        assertEquals(HttpStatus.OK, page.getStatusCode());
        JsonNode pageBody = objectMapper.readTree(page.getBody());
        boolean appointmentFound = false;
        for (JsonNode listedAppointment : pageBody.path("content")) {
            if (appointmentId.equals(listedAppointment.path("id").asText())) {
                appointmentFound = true;
                break;
            }
        }
        assertTrue(appointmentFound);

        ResponseEntity<String> replay = restTemplate.exchange(
                baseUrl + "/appointments",
                HttpMethod.POST,
                new HttpEntity<>(payload, headers),
                String.class);
        assertEquals(HttpStatus.CREATED, replay.getStatusCode());
        assertEquals(appointmentId,
                objectMapper.readTree(replay.getBody()).get("id").asText());
    }

    @Test
    void invalidHttpRequestReturnsClientErrorAndStructuredBody() throws Exception {
        String url = "http://localhost:" + port + "/api/v1/appointments";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Idempotency-Key", UUID.randomUUID().toString());

        ResponseEntity<String> response = restTemplate.postForEntity(
                url, new HttpEntity<>("{\"vehicleId\":\"vehicle-1\"}", headers), String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("INVALID_REQUEST", objectMapper.readTree(response.getBody())
                .get("code").asText());
    }
}
