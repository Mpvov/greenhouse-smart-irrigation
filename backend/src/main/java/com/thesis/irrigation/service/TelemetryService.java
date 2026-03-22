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
     * Topic format: {userId}/{ghId}/{zId}/{rId}/{metric} (e.g., "1/1/1/1/soil")
     */
    public Mono<Void> processMessage(String topic, String payload) {
        // ── Drop System & Malformed Topics ──
        if (topic == null || topic.startsWith("$SYS") || topic.split("/").length < 2) {
            return Mono.empty(); // Silently ignore system/garbage topics
        }

        log.debug("[TelemetryService] Topic: {}, Payload: {}", topic, payload);

        // ── Parse pure userId from topic for data isolation (Strip 'user_' prefix) ──
        String[] parts = topic.split("/");
        String rawUserId = parts.length > 0 ? parts[0] : "unknown";
        String userId = extractId(rawUserId, "user_");

        // ── Hot Path: Enrich payload with userId, push to Redis ──
        String enrichedPayload;
        try {
            JsonNode root = objectMapper.readTree(payload);
            // Wrap with userId for WebSocket filtering
            enrichedPayload = objectMapper.writeValueAsString(
                    objectMapper.createObjectNode()
                            .put("userId", userId)
                            .set("data", root));
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
            JsonNode root = objectMapper.readTree(payload);

            // Per Skill Rule 4.1: 'bn' (Base Name) should be the full Cloud Topic path
            String deviceId = root.has("bn") ? root.get("bn").asText() : topic;

            // Hierarchical Topic: {userId}/{ghId}/{zId}/{rId}/{metric} OR {userId}/{ghId}/{zId}/{metric}
            String[] parts = deviceId.split("/");
            String userId = parts.length > 0 ? extractId(parts[0], "user_") : null;
            String greenhouseId = parts.length > 1 ? extractId(parts[1], "gh_") : null;
            String zoneId = parts.length > 2 ? extractId(parts[2], "z_") : null;
            // Row ID is at parts[3] if length > 4 (e.g. user/gh/z/r/soil)
            String rowId = parts.length > 4 ? extractId(parts[3], "r_") : null;

            // State updates apply to specific Row or Zone objects (using pure IDs)
            String stateUpdateId = (rowId != null) ? rowId : zoneId;

            JsonNode entries = root.path("e");
            List<DataRecord> records = new ArrayList<>();

            if (entries.isMissingNode() || !entries.isArray()) {
                // Fallback: Metric name from last part of topic/bn, 'v' for value, 't' for epoch seconds
                String metric = (parts.length > 0) ? parts[parts.length - 1] : "unknown";
                double v = root.has("v") ? root.path("v").asDouble() : 0.0;
                long t = root.has("t") ? root.path("t").asLong() : 0;
                Instant ts = (t > 0) ? Instant.ofEpochSecond(t) : Instant.now();

                records.add(new DataRecord(null, deviceId, userId, greenhouseId, zoneId, rowId, metric, v, ts));
            } else {
                // Standard SenML Array processing
                for (JsonNode entry : entries) {
                    String n = entry.path("n").asText();
                    double v = entry.path("v").asDouble();
                    // entry.t takes precedence over root.t
                    long t = entry.has("t") ? entry.get("t").asLong() : (root.has("t") ? root.get("t").asLong() : 0);
                    Instant ts = (t > 0) ? Instant.ofEpochSecond(t) : Instant.now();

                    records.add(new DataRecord(null, deviceId, userId, greenhouseId, zoneId, rowId, n, v, ts));
                }
            }

            if (records.isEmpty()) return Mono.empty();

            return dataRecordRepository.saveAll(records)
                    .then(updateLastKnownState(stateUpdateId, records))
                    .then();

        } catch (Exception e) {
            log.error("[Cold Path] Failed to parse payload for topic {}: {}", topic, e.getMessage());
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
                            .greenhouseId(row.greenhouseId())
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
                                        if ("temperature".equals(rec.measurement()) || "t".equals(rec.measurement())
                                                || "temp".equals(rec.measurement())) {
                                            builder.lastTemperature(rec.value());
                                        }
                                        if ("humidity".equals(rec.measurement()) || "h".equals(rec.measurement())) {
                                            builder.lastHumidity(rec.value());
                                        }
                                    }
                                    return zoneRepository.save(builder.build()).then();
                                }))
                .then();
    }

    /**
     * Extracts pure ID by stripping prefix (e.g., "user_123" -> "123").
     */
    private String extractId(String part, String prefix) {
        if (part == null) return null;
        if (part.startsWith(prefix)) {
            return part.substring(prefix.length());
        }
        return part;
    }
}
