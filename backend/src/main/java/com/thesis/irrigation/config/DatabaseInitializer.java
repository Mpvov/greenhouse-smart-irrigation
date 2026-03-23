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
        log.info("[DatabaseInitializer] Checking if database needs seeding...");

        try {
            userRepository.count()
                    .flatMap(count -> {
                        if (count == 0) {
                            log.info("[DatabaseInitializer] 🚨 Database is empty. Initiating clean seed...");
                            return dropAll().then(seedData());
                        } else {
                            log.info("[DatabaseInitializer] ✅ Database already has {} users. Skipping seeding.", count);
                            return Mono.empty();
                        }
                    })
                    .timeout(java.time.Duration.ofSeconds(10))
                    .doOnError(e -> log.error("[DatabaseInitializer] ❌ FAILED during initialization: {}", e.getMessage()))
                    .block();
            log.info("[DatabaseInitializer] Initialization check sequence finished.");
        } catch (Exception error) {
            log.error("[DatabaseInitializer] 🛑 CRITICAL Error during initialization: {}", error.getMessage(), error);
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
                .userId("admin")
                .email("admin@bk.hcm")
                .passwordHash("$2a$10$EN2xrec0SJEyyFic3TYtmOuo1pSVUqOPD6nS2DdpCGRL55BHSl5MC") // placeholder
                .role("ADMIN")
                .build();

        // ── 2. Greenhouse ──
        Greenhouse gh = Greenhouse.builder()
                .greenhouseId("alpha")
                .ownerId("admin")
                .name("Nhà kính Alpha")
                .location("TP.HCM, Việt Nam")
                .build();

        // ── 3. Zone ──
        Zone zone = Zone.builder()
                .zoneId(1)
                .greenhouseId("alpha")
                .name("Khu Vực 1")
                .lastTemperature(28.5)
                .lastHumidity(65.0)
                .build();

        // ── 4. Row ──
        Row row = Row.builder()
                .rowId(1)
                .zoneId("1")
                .greenhouseId("alpha")
                .name("Luống Cà Chua")
                .plantType("Cà chua")
                .currentMode("AUTO")
                .lastSoilMoisture(45.0)
                .pumpStatus("OFF")
                .build();

        return userRepository.save(user)
                .flatMap(u -> {
                    Greenhouse ghToSave = Greenhouse.builder()
                            .greenhouseId("alpha")
                            .ownerId(u.userId())
                            .name("Nhà kính Alpha")
                            .location("TP.HCM, Việt Nam")
                            .build();
                    return greenhouseRepository.save(ghToSave);
                })
                .flatMap(savedGh -> {
                    Zone zToSave = Zone.builder()
                            .zoneId(1)
                            .greenhouseId(savedGh.id())
                            .name("Khu Vực 1")
                            .lastTemperature(28.5)
                            .lastHumidity(65.0)
                            .build();
                    return zoneRepository.save(zToSave)
                            .map(savedZ -> new Object[]{savedGh, savedZ});
                })
                .flatMap(objs -> {
                    Greenhouse savedGh = (Greenhouse) objs[0];
                    Zone savedZ = (Zone) objs[1];
                    Row rToSave = Row.builder()
                            .rowId(1)
                            .zoneId(savedZ.id())
                            .greenhouseId(savedGh.id())
                            .name("Luống Cà Chua")
                            .plantType("Cà chua")
                            .currentMode("AUTO")
                            .lastSoilMoisture(45.0)
                            .pumpStatus("OFF")
                            .build();
                    return rowRepository.save(rToSave)
                            .map(savedRow -> new Object[]{savedGh, savedZ, savedRow});
                })
                .flatMap(objs -> seedHistoricalData((Greenhouse) objs[0], (Zone) objs[1], (Row) objs[2]))
                .doOnSuccess(v -> log.info("[DatabaseInitializer] Seeded: User(1), Greenhouse(1), Zone(1), Row(1)"));
    }

    /**
     * Seeds 24 hours of DataRecord (temperature, humidity, soil_moisture)
     * and ControlLog entries (pump start/stop every 6h).
     */
    private Mono<Void> seedHistoricalData(Greenhouse gh, Zone z, Row r) {
        log.info("[DatabaseInitializer] Seeding 24h of historical data...");

        List<DataRecord> records = new ArrayList<>();
        List<ControlLog> logs = new ArrayList<>();
        Instant now = Instant.now();

        String userId = gh.ownerId();

        for (int i = 24; i >= 0; i--) {
            Instant ts = now.minus(i, ChronoUnit.HOURS);

            String zTopic = "user_" + userId + "/gh_" + gh.greenhouseId() + "/z_" + z.zoneId() + "/temp";
            String rTopic = "user_" + userId + "/gh_" + gh.greenhouseId() + "/z_" + z.zoneId() + "/r_" + r.rowId() + "/soil";

            // Zone-level: temperature & humidity
            records.add(new DataRecord(null, zTopic, userId, gh.id(), z.id(), null, "temperature", 25.0 + random.nextDouble() * 10, ts));
            records.add(new DataRecord(null, zTopic, userId, gh.id(), z.id(), null, "humidity", 50.0 + random.nextDouble() * 30, ts));

            // Row-level: soil moisture
            records.add(new DataRecord(null, rTopic, userId, gh.id(), z.id(), r.id(), "soil_moisture", 30.0 + random.nextDouble() * 40, ts));

            // ControlLog every 6 hours
            if (i % 6 == 0) {
                logs.add(ControlLog.builder()
                        .deviceId(rTopic)
                        .userId(userId)
                        .greenhouseId(gh.id())
                        .zoneId(z.id())
                        .rowId(r.id())
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
