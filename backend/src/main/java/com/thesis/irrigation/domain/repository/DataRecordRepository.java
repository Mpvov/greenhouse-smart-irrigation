package com.thesis.irrigation.domain.repository;

import com.thesis.irrigation.domain.model.DataRecord;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.time.Instant;

@Repository
public interface DataRecordRepository extends ReactiveMongoRepository<DataRecord, String> {
    Flux<DataRecord> findByDeviceIdAndTimestampBetweenOrderByTimestampAsc(String deviceId, Instant from, Instant to);
}
