package com.thesis.irrigation.domain.repository;

import com.thesis.irrigation.domain.model.Row;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface RowRepository extends ReactiveMongoRepository<Row, String> {
    Flux<Row> findByZoneId(String zoneId);
    Flux<Row> findByGreenhouseId(String greenhouseId);
    Mono<Row> findByZoneIdAndRowId(String zoneId, Integer rowId);
    Mono<Void> deleteByGreenhouseId(String greenhouseId);
    Mono<Void> deleteByZoneId(String zoneId);
}
