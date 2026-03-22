package com.thesis.irrigation.controller;

import com.thesis.irrigation.AbstractIntegrationTest;
import com.thesis.irrigation.domain.model.Greenhouse;
import com.thesis.irrigation.domain.repository.GreenhouseRepository;
import com.thesis.irrigation.utils.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@AutoConfigureWebTestClient
public class GreenhouseSecurityIT extends AbstractIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private GreenhouseRepository greenhouseRepository;

    @MockBean
    private JwtUtil jwtUtil;

    @BeforeEach
    public void setup() {
        // Prepare Tenant B's data
        Greenhouse ghB = Greenhouse.builder()
                .id("ghB")
                .ownerId("tenantB")
                .name("Greenhouse B")
                .build();
        greenhouseRepository.save(ghB).block();

        // Mock token validation for tenantA and tenantB
        when(jwtUtil.validateToken("tokenA")).thenReturn(true);
        when(jwtUtil.getUserIdFromToken("tokenA")).thenReturn("tenantA");

        when(jwtUtil.validateToken("tokenB")).thenReturn(true);
        when(jwtUtil.getUserIdFromToken("tokenB")).thenReturn("tenantB");
    }

    @Test
    public void testGetGreenhouse_WithTenantATokenAccessingTenantBGreenhouse_ShouldReturn403() {
        // Call API to get ghB passing tokenA
        webTestClient.get()
                .uri("/api/v1/greenhouses/ghB")
                .header("Authorization", "Bearer tokenA")
                .exchange()
                // Expect 403 Forbidden (Currently default implementation returns 404 or empty if not found, 
                // but for Security test we expect it to be 403 Forbidden per requirements)
                .expectStatus().isForbidden();
    }
}
