package com.thesis.irrigation.service;

import com.thesis.irrigation.domain.model.Zone;
import com.thesis.irrigation.domain.repository.DeviceRepository;
import com.thesis.irrigation.domain.repository.GreenhouseRepository;
import com.thesis.irrigation.domain.repository.RowRepository;
import com.thesis.irrigation.domain.repository.ZoneRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class ZoneService {

    private final ZoneRepository zoneRepository;
    private final GreenhouseRepository greenhouseRepository;
    private final RowRepository rowRepository;
    private final DeviceRepository deviceRepository;

    public Flux<Zone> getByGreenhouse(String greenhouseId, String ownerId) {
        return greenhouseRepository.findById(greenhouseId)
                .filter(gh -> gh.ownerId().equals(ownerId))
                .flatMapMany(gh -> zoneRepository.findByGreenhouseId(greenhouseId));
    }

    public Mono<Zone> getById(String id, String ownerId) {
        return zoneRepository.findById(id)
                .flatMap(zone -> greenhouseRepository.findById(zone.greenhouseId())
                        .filter(gh -> gh.ownerId().equals(ownerId))
                        .map(gh -> zone));
    }

    public Mono<Zone> createZone(Zone zone, String ownerId) {
        return greenhouseRepository.findById(zone.greenhouseId())
                .filter(gh -> gh.ownerId().equals(ownerId))
                .flatMap(gh -> zoneRepository.findByGreenhouseId(zone.greenhouseId())
                        .map(Zone::zoneId)
                        .reduce(Math::max)
                        .defaultIfEmpty(0)
                        .map(maxId -> maxId + 1)
                        .flatMap(nextId -> {
                            Zone toSave = Zone.builder()
                                    .zoneId(nextId)
                                    .greenhouseId(zone.greenhouseId())
                                    .name(zone.name())
                                    .lastTemperature(null)
                                    .lastHumidity(null)
                                    .build();
                            return zoneRepository.save(toSave);
                        }))
                .switchIfEmpty(Mono.error(new RuntimeException("Greenhouse not found or access denied")));
    }

    public Mono<Zone> updateZone(String id, Zone zone, String ownerId) {
        return getById(id, ownerId)
                .flatMap(existing -> {
                    Zone updated = Zone.builder()
                            .id(id)
                            .zoneId(existing.zoneId())
                            .greenhouseId(existing.greenhouseId())
                            .name(zone.name())
                            .lastTemperature(existing.lastTemperature())
                            .lastHumidity(existing.lastHumidity())
                            .build();
                    return zoneRepository.save(updated);
                });
    }

    @Transactional
    public Mono<Void> deleteZone(String id, String ownerId) {
        return getById(id, ownerId)
                .flatMap(zone -> {
                    // 1. Delete rows in this zone
                    Mono<Void> deleteRows = rowRepository.deleteByZoneId(id);
                    // 2. Delete devices in this zone
                    Mono<Void> deleteDevices = deviceRepository.deleteByZoneId(id);
                    // 3. Delete zone itself
                    return Mono.when(deleteRows, deleteDevices)
                            .then(zoneRepository.delete(zone));
                });
    }
}
