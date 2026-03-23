package com.thesis.irrigation.domain.repository;

import com.thesis.irrigation.domain.model.DataRecord;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Repository
public interface DataRecordRepository extends ReactiveMongoRepository<DataRecord, String> {
    Flux<DataRecord> findByRowIdAndTimestampBetweenOrderByTimestampAsc(String rowId, Instant from, Instant to);

    Flux<DataRecord> findByZoneIdAndTimestampBetweenOrderByTimestampAsc(String zoneId, Instant from, Instant to);

    @Query(value = "{ '$or': [ { 'rowId': ?0 }, { 'zoneId': ?1, 'rowId': null } ], 'timestamp': { '$gte': ?2, '$lte': ?3 } }", sort = "{ 'timestamp': 1 }")
    Flux<DataRecord> findByRowOrZoneParent(String rowId, String zoneId, Instant from, Instant to);

    Flux<DataRecord> findByDeviceIdAndTimestampBetweenOrderByTimestampAsc(String deviceId, Instant from, Instant to);

    Mono<Void> deleteByGreenhouseId(String greenhouseId);
}
