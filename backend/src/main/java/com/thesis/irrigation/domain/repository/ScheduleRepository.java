package com.thesis.irrigation.domain.repository;

import com.thesis.irrigation.domain.model.Schedule;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface ScheduleRepository extends ReactiveMongoRepository<Schedule, String> {
    Flux<Schedule> findByRowId(String rowId);
    Flux<Schedule> findByRowIdAndIsActiveTrue(String rowId);
    Mono<Void> deleteByRowId(String rowId);
}
