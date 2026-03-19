package com.thesis.irrigation.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thesis.irrigation.domain.model.DataRecord;
import com.thesis.irrigation.domain.model.Row;
import com.thesis.irrigation.domain.model.Zone;
import com.thesis.irrigation.domain.repository.DataRecordRepository;
import com.thesis.irrigation.domain.repository.RowRepository;
import com.thesis.irrigation.domain.repository.ZoneRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelemetryService {

    private final DataRecordRepository dataRecordRepository;
    private final RowRepository rowRepository;
    private final ZoneRepository zoneRepository;
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String REDIS_TOPIC = "iot.telemetry.realtime";

    /**
     * Entry point for processing raw MQTT telemetry payloads.
     * Topic format: {userId}/{ghId}/{zId}/{rId}/{metric}  (e.g., "1/1/1/1/soil")
     */
    public Mono<Void> processMessage(String topic, String payload) {
        // ── Drop System & Malformed Topics ──
        if (topic == null || topic.startsWith("$SYS") || topic.split("/").length < 2) {
            return Mono.empty(); // Silently ignore system/garbage topics
        }

        log.debug("[TelemetryService] Topic: {}, Payload: {}", topic, payload);

        // ── Parse userId from topic for data isolation ──
        String[] parts = topic.split("/");
        String userId = parts.length > 0 ? parts[0] : "unknown";

        // ── Hot Path: Enrich payload with userId, push to Redis ──
        String enrichedPayload;
        try {
            JsonNode root = objectMapper.readTree(payload);
            // Wrap with userId for WebSocket filtering
            enrichedPayload = objectMapper.writeValueAsString(
                    objectMapper.createObjectNode()
                            .put("userId", userId)
                            .set("data", root)
            );
        } catch (Exception e) {
            enrichedPayload = payload; // Fallback: raw payload
        }

        Mono<Long> hotPath = redisTemplate.convertAndSend(REDIS_TOPIC, enrichedPayload)
                .doOnSuccess(v -> log.debug("[Hot Path] Published to Redis. userId={}", userId));

        // ── Cold Path: Parse SenML and persist ──
        Mono<Void> coldPath = parseAndSave(topic, payload);

        return Mono.when(hotPath, coldPath).then();
    }

    /**
     * Parses SenML payload and saves to MongoDB.
     * Also updates last known state in Zone/Row documents.
     */
    private Mono<Void> parseAndSave(String topic, String payload) {
        try {
            // Extract IDs from topic: {userId}/{ghId}/{zId}/{rId}/... or {userId}/{ghId}/{zId}/...
            String[] parts = topic.split("/");
            String zoneId = parts.length > 2 ? parts[2] : null;
            String rowId  = parts.length > 4 ? parts[3] : null;

            JsonNode root = objectMapper.readTree(payload);
            JsonNode entries = root.path("e");

            if (entries.isMissingNode() || !entries.isArray()) {
                // Try parsing as flat key-value: topic ends with metric name
                String metricName = parts.length > 4 ? parts[4] : "unknown";
                double value = root.has("v") ? root.path("v").asDouble() : 0.0;
                String deviceId = rowId != null ? rowId : (zoneId != null ? zoneId : "unknown");

                DataRecord record = new DataRecord(null, deviceId, metricName, value, Instant.now());
                return dataRecordRepository.save(record)
                        .then(updateLastKnownState(deviceId, List.of(record)))
                        .then();
            }

            // Standard SenML with "e" array
            String deviceId = rowId != null ? rowId : (zoneId != null ? zoneId : "unknown");
            List<DataRecord> records = new ArrayList<>();
            Instant now = Instant.now();

            for (JsonNode entry : entries) {
                String name = entry.path("n").asText();
                double value = entry.path("v").asDouble();
                records.add(new DataRecord(null, deviceId, name, value, now));
            }

            if (records.isEmpty()) return Mono.empty();

            return dataRecordRepository.saveAll(records)
                    .then(updateLastKnownState(deviceId, records))
                    .then();

        } catch (Exception e) {
            log.error("[Cold Path] Failed to parse: {}", e.getMessage());
            return Mono.empty();
        }
    }

    /**
     * Updates the last known state in the Row or Zone document.
     */
    private Mono<Void> updateLastKnownState(String deviceId, List<DataRecord> records) {
        // Try Row first, then Zone
        return rowRepository.findById(deviceId)
                .flatMap(row -> {
                    Row.RowBuilder builder = Row.builder()
                            .id(row.id())
                            .zoneId(row.zoneId())
                            .name(row.name())
                            .plantType(row.plantType())
                            .currentMode(row.currentMode())
                            .lastSoilMoisture(row.lastSoilMoisture())
                            .pumpStatus(row.pumpStatus());

                    for (DataRecord rec : records) {
                        if ("soil".equals(rec.measurement()) || "soil_moisture".equals(rec.measurement())) {
                            builder.lastSoilMoisture(rec.value());
                        }
                        if ("pump".equals(rec.measurement())) {
                            builder.pumpStatus(rec.value() > 0 ? "ON" : "OFF");
                        }
                    }
                    return rowRepository.save(builder.build()).then();
                })
                .switchIfEmpty(
                        zoneRepository.findById(deviceId)
                                .flatMap(zone -> {
                                    Zone.ZoneBuilder builder = Zone.builder()
                                            .id(zone.id())
                                            .greenhouseId(zone.greenhouseId())
                                            .name(zone.name())
                                            .lastTemperature(zone.lastTemperature())
                                            .lastHumidity(zone.lastHumidity());

                                    for (DataRecord rec : records) {
                                        if ("temperature".equals(rec.measurement()) || "t".equals(rec.measurement()) || "temp".equals(rec.measurement())) {
                                            builder.lastTemperature(rec.value());
                                        }
                                        if ("humidity".equals(rec.measurement()) || "h".equals(rec.measurement())) {
                                            builder.lastHumidity(rec.value());
                                        }
                                    }
                                    return zoneRepository.save(builder.build()).then();
                                })
                )
                .then();
    }
}
