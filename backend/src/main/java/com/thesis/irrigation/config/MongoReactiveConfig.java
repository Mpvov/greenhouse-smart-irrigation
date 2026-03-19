package com.thesis.irrigation.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;

/**
 * MongoReactiveConfig — Activates Reactive MongoDB Repositories.
 *
 * Spring Boot auto-configures the ReactiveMongoClient from application.yml:
 *   spring.data.mongodb.uri
 *
 * This class explicitly enables reactive repository scanning to ensure
 * all repositories under the 'repository' package return Mono/Flux (never blocking).
 */
@Slf4j
@Configuration
@EnableReactiveMongoRepositories(basePackages = "com.thesis.irrigation.domain.repository")
public class MongoReactiveConfig {

    public MongoReactiveConfig() {
        log.info("[MongoReactiveConfig] Reactive MongoDB repositories enabled. " +
                 "Scanning package: com.thesis.irrigation.domain.repository");
    }
}
