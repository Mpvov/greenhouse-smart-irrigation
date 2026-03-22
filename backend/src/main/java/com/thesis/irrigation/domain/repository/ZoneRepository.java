package com.thesis.irrigation.domain.repository;

import com.thesis.irrigation.domain.model.Zone;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface ZoneRepository extends ReactiveMongoRepository<Zone, String> {
    Flux<Zone> findByGreenhouseId(String greenhouseId);
    Mono<Void> deleteByGreenhouseId(String greenhouseId);
}
