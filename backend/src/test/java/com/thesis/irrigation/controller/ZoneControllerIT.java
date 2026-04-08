package com.thesis.irrigation.controller;

import com.thesis.irrigation.AbstractIntegrationTest;
import com.thesis.irrigation.domain.model.Greenhouse;
import com.thesis.irrigation.domain.model.Zone;
import com.thesis.irrigation.domain.repository.GreenhouseRepository;
import com.thesis.irrigation.domain.repository.ZoneRepository;
import com.thesis.irrigation.utils.JwtUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@AutoConfigureWebTestClient
public class ZoneControllerIT extends AbstractIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private GreenhouseRepository greenhouseRepository;

    @Autowired
    private ZoneRepository zoneRepository;

    @MockBean
    private JwtUtil jwtUtil;

    private Greenhouse testGreenhouse;

    @BeforeEach
    public void setup() {
        // Clear old data first
        zoneRepository.deleteAll().block();
        greenhouseRepository.deleteAll().block();

        // Setup fresh data in MongoDB Container
        testGreenhouse = Greenhouse.builder()
                .ownerId("user-123")
                .name("Integration Test GH")
                .location("Campus GH")
                .build();
        testGreenhouse = greenhouseRepository.save(testGreenhouse).block();

        Zone testZone = Zone.builder()
                .greenhouseId(testGreenhouse.id())
                .name("Initial Zone")
                .zoneId(1)
                .build();
        zoneRepository.save(testZone).block();

        // Mock valid JWT behavior
        when(jwtUtil.validateToken(anyString())).thenReturn(true);
        when(jwtUtil.getUserIdFromToken(anyString())).thenReturn("user-123");
    }

    @AfterEach
    public void cleanup() {
        zoneRepository.deleteAll().block();
        greenhouseRepository.deleteAll().block();
    }

    @Test
    public void testCreateZone_ShouldReturn201() {
        Zone newZone = Zone.builder()
                .greenhouseId(testGreenhouse.id())
                .name("New Zone IT")
                .build();

        webTestClient.post()
                .uri("/api/v1/zones")
                .header("Authorization", "Bearer mock-token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(newZone)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Created")
                .jsonPath("$.data.name").isEqualTo("New Zone IT")
                .jsonPath("$.data.zoneId").isEqualTo(2); // Since 1 is taken
    }

    @Test
    public void testGetByGreenhouse_ShouldReturn200() {
        webTestClient.get()
                .uri("/api/v1/zones/greenhouse/" + testGreenhouse.id())
                .header("Authorization", "Bearer mock-token")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Success")
                .jsonPath("$.data").isArray()
                .jsonPath("$.data[0].name").isEqualTo("Initial Zone");
    }

    @Test
    public void testUnauthorizedRequest_ShouldReturn401() {
        // Mock token validation failed
        when(jwtUtil.validateToken(anyString())).thenReturn(false);

        webTestClient.get()
                .uri("/api/v1/zones/greenhouse/" + testGreenhouse.id())
                .header("Authorization", "Bearer invalid-token")
                .exchange()
                .expectStatus().isUnauthorized();
    }
    
    @Test
    public void testCreateZone_Forbidden_WhenOwnerMismatch() {
        // User is authenticated but tries to add zone to a greenhouse they don't own
        when(jwtUtil.getUserIdFromToken(anyString())).thenReturn("hacker-user");

        Zone newZone = Zone.builder()
                .greenhouseId(testGreenhouse.id())
                .name("Hacker Zone")
                .build();

        webTestClient.post()
                .uri("/api/v1/zones")
                .header("Authorization", "Bearer hacker-token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(newZone)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Greenhouse not found or access denied");
    }
}
