package com.thesis.irrigation.domain.repository;

import com.thesis.irrigation.domain.model.Greenhouse;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface GreenhouseRepository extends ReactiveMongoRepository<Greenhouse, String> {
    Flux<Greenhouse> findByOwnerId(String ownerId);
    Mono<Greenhouse> findByOwnerIdAndGreenhouseId(String ownerId, String greenhouseId);
}
