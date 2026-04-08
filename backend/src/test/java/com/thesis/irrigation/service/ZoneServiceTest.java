package com.thesis.irrigation.service;

import com.thesis.irrigation.domain.model.Greenhouse;
import com.thesis.irrigation.domain.model.Zone;
import com.thesis.irrigation.domain.repository.DeviceRepository;
import com.thesis.irrigation.domain.repository.GreenhouseRepository;
import com.thesis.irrigation.domain.repository.RowRepository;
import com.thesis.irrigation.domain.repository.ZoneRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ZoneServiceTest {

    @Mock
    private ZoneRepository zoneRepository;

    @Mock
    private GreenhouseRepository greenhouseRepository;

    @Mock
    private RowRepository rowRepository;

    @Mock
    private DeviceRepository deviceRepository;

    @InjectMocks
    private ZoneService zoneService;

    @Test
    public void testGetById_Success() {
        String zoneId = "zone-id-1";
        String greenhouseId = "gh-1";
        String ownerId = "user-1";

        Zone mockZone = Zone.builder()
                .id(zoneId)
                .zoneId(1)
                .greenhouseId(greenhouseId)
                .name("Tomato Zone")
                .lastTemperature(25.0)
                .lastHumidity(60.0)
                .build();

        Greenhouse mockGreenhouse = Greenhouse.builder()
                .id(greenhouseId)
                .ownerId(ownerId) // Matches owner
                .build();

        when(zoneRepository.findById(zoneId)).thenReturn(Mono.just(mockZone));
        when(greenhouseRepository.findById(greenhouseId)).thenReturn(Mono.just(mockGreenhouse));

        Mono<Zone> resultMono = zoneService.getById(zoneId, ownerId);

        StepVerifier.create(resultMono)
                .assertNext(zone -> {
                    assertEquals("Tomato Zone", zone.name());
                    assertEquals("gh-1", zone.greenhouseId());
                })
                .verifyComplete();
    }

    @Test
    public void testGetById_AccessDenied_WhenOwnerMismatch() {
        String zoneId = "zone-id-1";
        String greenhouseId = "gh-1";
        String requesterId = "hacker-user";

        Zone mockZone = Zone.builder()
                .id(zoneId)
                .greenhouseId(greenhouseId)
                .build();

        Greenhouse mockGreenhouse = Greenhouse.builder()
                .id(greenhouseId)
                .ownerId("user-1") // Different owner
                .build();

        when(zoneRepository.findById(zoneId)).thenReturn(Mono.just(mockZone));
        when(greenhouseRepository.findById(greenhouseId)).thenReturn(Mono.just(mockGreenhouse));

        Mono<Zone> resultMono = zoneService.getById(zoneId, requesterId);

        // Should be empty because filter(gh -> gh.ownerId().equals(ownerId)) will fail
        StepVerifier.create(resultMono)
                .verifyComplete();
    }

    @Test
    public void testGetByGreenhouse_Success() {
        String greenhouseId = "gh-1";
        String ownerId = "user-1";

        Greenhouse mockGreenhouse = Greenhouse.builder()
                .id(greenhouseId)
                .ownerId(ownerId)
                .build();

        Zone zone1 = Zone.builder().id("zone-1").name("Zone 1").build();
        Zone zone2 = Zone.builder().id("zone-2").name("Zone 2").build();

        when(greenhouseRepository.findById(greenhouseId)).thenReturn(Mono.just(mockGreenhouse));
        when(zoneRepository.findByGreenhouseId(greenhouseId)).thenReturn(Flux.just(zone1, zone2));

        Flux<Zone> resultFlux = zoneService.getByGreenhouse(greenhouseId, ownerId);

        StepVerifier.create(resultFlux)
                .expectNextCount(2)
                .verifyComplete();
    }

    @Test
    public void testCreateZone_Success() {
        String greenhouseId = "gh-1";
        String ownerId = "user-1";

        Zone newZoneRequest = Zone.builder()
                .greenhouseId(greenhouseId)
                .name("New Zone")
                .build();

        Greenhouse mockGreenhouse = Greenhouse.builder()
                .id(greenhouseId)
                .ownerId(ownerId)
                .build();

        Zone existingZone = Zone.builder().zoneId(5).build(); // Current max zoneId

        when(greenhouseRepository.findById(greenhouseId)).thenReturn(Mono.just(mockGreenhouse));
        when(zoneRepository.findByGreenhouseId(greenhouseId)).thenReturn(Flux.just(existingZone));
        when(zoneRepository.save(any(Zone.class))).thenAnswer(invocation -> {
            Zone z = invocation.getArgument(0);
            return Mono.just(Zone.builder()
                    .id("new-id")
                    .zoneId(z.zoneId())
                    .greenhouseId(z.greenhouseId())
                    .name(z.name())
                    .build());
        });

        Mono<Zone> resultMono = zoneService.createZone(newZoneRequest, ownerId);

        StepVerifier.create(resultMono)
                .assertNext(zone -> {
                    assertEquals("new-id", zone.id());
                    assertEquals(6, zone.zoneId()); // Should be max + 1
                    assertEquals("New Zone", zone.name());
                })
                .verifyComplete();
    }

    @Test
    public void testDeleteZone_Success() {
        String zoneId = "zone-1";
        String greenhouseId = "gh-1";
        String ownerId = "user-1";

        Zone mockZone = Zone.builder().id(zoneId).greenhouseId(greenhouseId).build();
        Greenhouse mockGreenhouse = Greenhouse.builder().id(greenhouseId).ownerId(ownerId).build();

        when(zoneRepository.findById(zoneId)).thenReturn(Mono.just(mockZone));
        when(greenhouseRepository.findById(greenhouseId)).thenReturn(Mono.just(mockGreenhouse));
        
        when(rowRepository.deleteByZoneId(zoneId)).thenReturn(Mono.empty());
        when(deviceRepository.deleteByZoneId(zoneId)).thenReturn(Mono.empty());
        when(zoneRepository.delete(mockZone)).thenReturn(Mono.empty());

        Mono<Void> resultMono = zoneService.deleteZone(zoneId, ownerId);

        StepVerifier.create(resultMono)
                .verifyComplete();

        // Verify dependencies were called
        verify(rowRepository, times(1)).deleteByZoneId(zoneId);
        verify(deviceRepository, times(1)).deleteByZoneId(zoneId);
        verify(zoneRepository, times(1)).delete(mockZone);
    }
}
