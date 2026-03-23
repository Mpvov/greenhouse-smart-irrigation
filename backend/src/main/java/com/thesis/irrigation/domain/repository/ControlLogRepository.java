package com.thesis.irrigation.domain.repository;

import com.thesis.irrigation.domain.model.ControlLog;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Repository
public interface ControlLogRepository extends ReactiveMongoRepository<ControlLog, String> {
    Flux<ControlLog> findByDeviceIdAndTimestampBetweenOrderByTimestampAsc(String deviceId, Instant from, Instant to);
    Flux<ControlLog> findByRowIdAndTimestampBetweenOrderByTimestampAsc(String rowId, Instant from, Instant to);

    Mono<Void> deleteByGreenhouseId(String greenhouseId);
}
