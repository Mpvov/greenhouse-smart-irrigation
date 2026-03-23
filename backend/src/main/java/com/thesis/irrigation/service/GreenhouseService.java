package com.thesis.irrigation.service;

import com.thesis.irrigation.domain.model.Greenhouse;
import com.thesis.irrigation.domain.repository.*;
import com.thesis.irrigation.integration.MqttSubscriptionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class GreenhouseService {

    private final GreenhouseRepository greenhouseRepository;
    private final ZoneRepository zoneRepository;
    private final RowRepository rowRepository;
    private final DeviceRepository deviceRepository;
    private final DataRecordRepository dataRecordRepository;
    private final ControlLogRepository controlLogRepository;
    private final MqttSubscriptionManager mqttSubscriptionManager;

    public Flux<Greenhouse> getAllByOwner(String ownerId) {
        return greenhouseRepository.findByOwnerId(ownerId);
    }

    public Mono<Greenhouse> getById(String id, String ownerId) {
        return greenhouseRepository.findById(id)
                .filter(gh -> gh.ownerId().equals(ownerId));
    }

    public Mono<Greenhouse> createGreenhouse(Greenhouse greenhouse) {
        // Create a URL-safe string from the name to use as greenhouseId
        String slug = greenhouse.name().toLowerCase().replaceAll("[^a-z0-9]", "_");
        
        Greenhouse toSave = Greenhouse.builder()
                .greenhouseId(slug)
                .ownerId(greenhouse.ownerId())
                .name(greenhouse.name())
                .location(greenhouse.location())
                .build();
                
        return greenhouseRepository.save(toSave)
                .doOnNext(saved -> mqttSubscriptionManager.addSubscription(saved.ownerId(), saved.greenhouseId()));
    }

    public Mono<Greenhouse> updateGreenhouse(String id, Greenhouse greenhouse, String ownerId) {
        return greenhouseRepository.findById(id)
                .filter(gh -> gh.ownerId().equals(ownerId))
                .flatMap(existing -> {
                    Greenhouse updated = Greenhouse.builder()
                            .id(id)
                            .greenhouseId(existing.greenhouseId())
                            .ownerId(ownerId)
                            .name(greenhouse.name())
                            .location(greenhouse.location())
                            .build();
                    return greenhouseRepository.save(updated);
                });
    }

    @Transactional
    public Mono<Void> deleteGreenhouse(String id, String ownerId) {
        log.info("[GreenhouseService] Initiating cascading delete for Greenhouse: {} (Owner: {})", id, ownerId);
        
        return greenhouseRepository.findById(id)
                .filter(gh -> gh.ownerId().equals(ownerId))
                .flatMap(gh -> {
                    // 1. Delete Rows under all Zones of this Greenhouse
                    Mono<Void> deleteRowsInZones = zoneRepository.findByGreenhouseId(id)
                            .flatMap(zone -> rowRepository.deleteByZoneId(zone.id()))
                            .then();

                    // 2. Delete Zones
                    Mono<Void> deleteZones = zoneRepository.deleteByGreenhouseId(id);

                    // 3. Delete Devices
                    Mono<Void> deleteDevices = deviceRepository.deleteByGreenhouseId(id);

                    // 4. Delete Timeseries Data (Records & Logs)
                    Mono<Void> deleteRecords = dataRecordRepository.deleteByGreenhouseId(id);
                    Mono<Void> deleteLogs = controlLogRepository.deleteByGreenhouseId(id);

                    return Mono.when(deleteRowsInZones, deleteZones, deleteDevices, deleteRecords, deleteLogs)
                            .then(greenhouseRepository.delete(gh))
                            .doOnSuccess(v -> {
                                log.info("[GreenhouseService] ✅ Cascading delete complete for Greenhouse: {}", id);
                                mqttSubscriptionManager.removeSubscription(ownerId, gh.greenhouseId());
                            });
                });
    }
}
