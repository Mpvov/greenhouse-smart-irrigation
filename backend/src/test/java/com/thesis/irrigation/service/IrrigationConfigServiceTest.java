package com.thesis.irrigation.service;

import com.thesis.irrigation.domain.dto.ConfigRequest;
import com.thesis.irrigation.domain.model.Row;
import com.thesis.irrigation.domain.repository.RowRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class IrrigationConfigServiceTest {

    @Mock
    private com.thesis.irrigation.domain.repository.RowRepository rowRepository;
    @Mock
    private com.thesis.irrigation.domain.repository.GreenhouseRepository greenhouseRepository;
    @Mock
    private com.thesis.irrigation.domain.repository.ZoneRepository zoneRepository;
    @Mock
    private com.thesis.irrigation.config.MqttGateway mqttGateway;
    
    @org.mockito.Spy
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

    @InjectMocks
    private IrrigationConfigServiceImpl irrigationConfigService;

    @BeforeEach
    void setUp() {
        // Additional setup if needed
    }

    @Test
    public void testUpdateConfig_WithValidPayload_ShouldSucceed() {
        ConfigRequest request = new ConfigRequest("AUTO", 40.0, 80.0);
        com.thesis.irrigation.domain.model.Row mockRow = com.thesis.irrigation.domain.model.Row.builder()
                .id("r1")
                .greenhouseId("gh1")
                .zoneId("z1")
                .build();
        com.thesis.irrigation.domain.model.Greenhouse mockGh = com.thesis.irrigation.domain.model.Greenhouse.builder()
                .id("gh1")
                .ownerId("tenantA")
                .build();

        when(rowRepository.findById("r1")).thenReturn(Mono.just(mockRow));
        when(greenhouseRepository.findById("gh1")).thenReturn(Mono.just(mockGh));
        when(rowRepository.save(any())).thenReturn(Mono.just(mockRow));

        org.springframework.security.core.Authentication auth = org.mockito.Mockito.mock(org.springframework.security.core.Authentication.class);
        when(auth.getName()).thenReturn("tenantA");

        StepVerifier.create(irrigationConfigService.updateConfig("r1", request)
                        .contextWrite(org.springframework.security.core.context.ReactiveSecurityContextHolder.withAuthentication(auth)))
                .expectSubscription()
                .verifyComplete();
    }

    @Test
    public void testUpdateConfig_WhenMinGreaterThanMax_ShouldReturnError() {
        ConfigRequest request = new ConfigRequest("AUTO", 90.0, 80.0); // min > max

        StepVerifier.create(irrigationConfigService.updateConfig("r1", request))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    public void testUpdateConfig_WhenThresholdOutOfRange_ShouldReturnError() {
        ConfigRequest request = new ConfigRequest("AUTO", -10.0, 110.0); // Out of range 0-100%

        StepVerifier.create(irrigationConfigService.updateConfig("r1", request))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    public void testUpdateConfig_WhenMissingCurrentMode_ShouldReturnError() {
        // Missing required field "currentMode"
        ConfigRequest request = new ConfigRequest(null, 40.0, 80.0);

        StepVerifier.create(irrigationConfigService.updateConfig("r1", request))
                .expectError(IllegalArgumentException.class)
                .verify();
    }
}
