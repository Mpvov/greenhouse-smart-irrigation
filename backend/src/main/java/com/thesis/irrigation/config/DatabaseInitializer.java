package com.thesis.irrigation.config;

import com.thesis.irrigation.domain.model.*;
import com.thesis.irrigation.domain.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.annotation.Order;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * DatabaseInitializer — Drops all collections and seeds fresh mock data on
 * startup.
 * Runs with @Order(1) so MqttSubscriptionManager (@Order(2)) can read the data
 * after.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(1)
public class DatabaseInitializer implements ApplicationListener<ApplicationReadyEvent> {

    private final ReactiveMongoTemplate mongoTemplate;
    private final UserRepository userRepository;
    private final GreenhouseRepository greenhouseRepository;
    private final ZoneRepository zoneRepository;
    private final RowRepository rowRepository;
    private final DataRecordRepository dataRecordRepository;
    private final ControlLogRepository controlLogRepository;

    private final Random random = new Random();

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("[DatabaseInitializer] Dropping old data and seeding fresh mock data...");

        try {
            dropAll()
                    .then(seedData())
                    .block();
            log.info("[DatabaseInitializer] ✅ Database initialization completed.");
        } catch (Exception error) {
            log.error("[DatabaseInitializer] Error: {}", error.getMessage(), error);
        }
    }

    private Mono<Void> dropAll() {
        return Mono.when(
                mongoTemplate.dropCollection("users"),
                mongoTemplate.dropCollection("greenhouses"),
                mongoTemplate.dropCollection("zones"),
                mongoTemplate.dropCollection("rows"),
                mongoTemplate.dropCollection("data_records"),
                mongoTemplate.dropCollection("control_logs"))
                .doOnSuccess(v -> log.info("[DatabaseInitializer] All collections dropped."));
    }

    private Mono<Void> seedData() {
        // ── 1. User ──
        User user = User.builder()
                .id("1")
                .email("admin@bk.hcm")
                .passwordHash("$2a$10$EN2xrec0SJEyyFic3TYtmOuo1pSVUqOPD6nS2DdpCGRL55BHSl5MC") // placeholder
                .role("OWNER")
                .build();

        // ── 2. Greenhouse ──
        Greenhouse gh = Greenhouse.builder()
                .id("1")
                .ownerId("1")
                .name("Nhà kính Alpha")
                .location("TP.HCM, Việt Nam")
                .build();

        // ── 3. Zone ──
        Zone zone = Zone.builder()
                .id("1")
                .greenhouseId("1")
                .name("Khu Vực 1")
                .lastTemperature(28.5)
                .lastHumidity(65.0)
                .build();

        // ── 4. Row ──
        Row row = Row.builder()
                .id("1")
                .zoneId("1")
                .name("Luống 1")
                .plantType("Cà chua")
                .currentMode("AUTO")
                .lastSoilMoisture(45.0)
                .pumpStatus("OFF")
                .build();

        return userRepository.save(user)
                .then(greenhouseRepository.save(gh))
                .then(zoneRepository.save(zone))
                .then(rowRepository.save(row))
                .then(seedHistoricalData())
                .doOnSuccess(v -> log.info("[DatabaseInitializer] Seeded: User(1), Greenhouse(1), Zone(1), Row(1)"));
    }

    /**
     * Seeds 24 hours of DataRecord (temperature, humidity, soil_moisture)
     * and ControlLog entries (pump start/stop every 6h).
     */
    private Mono<Void> seedHistoricalData() {
        log.info("[DatabaseInitializer] Seeding 24h of historical data...");

        List<DataRecord> records = new ArrayList<>();
        List<ControlLog> logs = new ArrayList<>();
        Instant now = Instant.now();

        for (int i = 24; i >= 0; i--) {
            Instant ts = now.minus(i, ChronoUnit.HOURS);

            // Zone-level: temperature & humidity
            records.add(new DataRecord(null, "1", "temperature", 25.0 + random.nextDouble() * 10, ts));
            records.add(new DataRecord(null, "1", "humidity", 50.0 + random.nextDouble() * 30, ts));

            // Row-level: soil moisture
            records.add(new DataRecord(null, "1", "soil_moisture", 30.0 + random.nextDouble() * 40, ts));

            // ControlLog every 6 hours
            if (i % 6 == 0) {
                logs.add(ControlLog.builder()
                        .deviceId("1")
                        .action(i % 12 == 0 ? "start" : "stop")
                        .source("AUTO")
                        .timestamp(ts.plus(30, ChronoUnit.MINUTES))
                        .build());
            }
        }

        return dataRecordRepository.saveAll(records)
                .thenMany(controlLogRepository.saveAll(logs))
                .then();
    }
}
