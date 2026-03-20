package com.thesis.irrigation.domain.repository;

import com.thesis.irrigation.domain.model.Alert;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface AlertRepository extends ReactiveMongoRepository<Alert, String> {
    Flux<Alert> findByUserIdOrderByTimestampDesc(String userId);
}
