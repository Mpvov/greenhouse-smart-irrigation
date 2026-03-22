package com.thesis.irrigation.domain.repository;

import com.thesis.irrigation.domain.model.Device;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface DeviceRepository extends ReactiveMongoRepository<Device, String> {
    Flux<Device> findByGreenhouseId(String greenhouseId);
    Flux<Device> findByZoneId(String zoneId);
    Flux<Device> findByRowId(String rowId);

    Mono<Void> deleteByGreenhouseId(String greenhouseId);
    Mono<Void> deleteByZoneId(String zoneId);
    Mono<Void> deleteByRowId(String rowId);
}
