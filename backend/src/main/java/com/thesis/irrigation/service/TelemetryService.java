package com.thesis.irrigation.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thesis.irrigation.domain.model.DataRecord;
import com.thesis.irrigation.domain.model.Row;
import com.thesis.irrigation.domain.model.Zone;
import com.thesis.irrigation.domain.repository.DataRecordRepository;
import com.thesis.irrigation.domain.repository.RowRepository;
import com.thesis.irrigation.domain.repository.ZoneRepository;
import com.thesis.irrigation.domain.repository.GreenhouseRepository;
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
    private final GreenhouseRepository greenhouseRepository;
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

        try {
            JsonNode root = objectMapper.readTree(payload);
            String deviceId = root.has("bn") ? root.get("bn").asText() : topic;

            String[] rawParts = deviceId.split("/");
            // Keep the raw userId (e.g., 'user_1' or 'user_tp1') instead of stripping 'user_'
            // We use this raw ID to query MongoDB's User collection directly.
            String rawUserId = rawParts.length > 0 ? rawParts[0] : null;
            // The frontend logic expects "userId" to match the database exactly
            // BUT if rawUserId is like "user_user_1", we only strip ONE "user_" if it was double prefixed
            // Wait, MqttSubscriptionManager appends "user_" + ownerId
            // So if ownerId is "tp1", topic is "user_tp1/...". If ownerId is "user_1", topic is "user_user_1/..."
            String dbOwnerId = extractId(rawUserId, "user_"); // This gives "tp1" or "user_1" exactly as in DB!

            String ghStr = rawParts.length > 1 ? extractId(rawParts[1], "gh_") : null;
            String zStr = rawParts.length > 2 ? extractId(rawParts[2], "z_") : null;
            String rStr = (rawParts.length > 3 && rawParts[3].startsWith("r_")) ? extractId(rawParts[3], "r_") : null;

            Integer zSeq = parseOrNull(zStr);
            Integer rSeq = parseOrNull(rStr);

            // --- DEV DEBUGGER OUTPUT ---
            log.info("====== MQTT PARSER DEBUGGER ======");
            log.info("1. Raw Topic/Device: '{}'", deviceId);
            log.info("2. Parsed STRINGS:");
            log.info("   - rawUserId: '{}'  -> DB ownerId: '{}'", rawUserId, dbOwnerId);
            log.info("   - ghStr (now String greenhouseId!): '{}'", ghStr);
            log.info("   - zStr:  '{}'", zStr);
            log.info("   - rStr:  '{}'", rStr);
            log.info("3. Parsed INTEGERS (zoneId and rowId are still Integers!):");
            log.info("   - zoneId (zSeq): {}", zSeq);
            log.info("   - rowId (rSeq): {}", rSeq);
            log.info("==================================");

            return resolveIds(dbOwnerId, ghStr, zSeq, rSeq)
                    .flatMap(ids -> {
                        // ── Hot Path: Enrich payload with physical IDs for UI routing ──
                        String enrichedPayload;
                        try {
                            enrichedPayload = objectMapper.writeValueAsString(
                                    objectMapper.createObjectNode()
                                            .put("userId", dbOwnerId)
                                            .put("ghId", ids.ghId) 
                                            .put("zId", ids.zId)   
                                            .put("rId", ids.rId)   
                                            .set("data", root));
                        } catch (Exception e) {
                            enrichedPayload = payload;
                        }

                        Mono<Long> hotPath = redisTemplate.convertAndSend(REDIS_TOPIC, enrichedPayload)
                                .doOnSuccess(v -> log.debug("[Hot Path] Published to Redis. ownerId={} rId={}", dbOwnerId, ids.rId))
                                .onErrorResume(e -> Mono.empty()); 

                        // ── Cold Path: Parse SenML and persist ──
                        List<DataRecord> records = buildDataRecords(deviceId, root, dbOwnerId, ids.ghId, ids.zId, ids.rId);
                        Mono<Void> coldPath = Mono.empty();
                        if (!records.isEmpty()) {
                            coldPath = dataRecordRepository.saveAll(records)
                                    .doOnNext(saved -> log.info("[Cold Path] Saved DataRecord: {}", saved))
                                    .doOnComplete(() -> log.info("[Cold Path] Successfully flushed records for: {}", deviceId))
                                    .doOnError(err -> log.error("[Cold Path] Failed to save records for {}", deviceId, err))
                                    .then(updateLastKnownState(ids.zId, ids.rId, records))
                                    .doOnSuccess(res -> log.info("[Cold Path] Updated last known state for: zId={}, rId={}", ids.zId, ids.rId))
                                    .then();
                        } else {
                            log.warn("[Cold Path] buildDataRecords returned empty for deviceId: {}", deviceId);
                        }

                        return Mono.when(hotPath, coldPath).then();
                    });
        } catch (Exception e) {
            log.error("[TelemetryService] Failed to parse payload for topic {}: {}", topic, e.getMessage());
            return Mono.empty();
        }
    }

    private Integer parseOrNull(String str) {
        if (str == null) return null;
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static class ResolvedIds {
        String ghId, zId, rId;
        ResolvedIds(String gh, String z, String r) { ghId = gh; zId = z; rId = r; }
    }

    private Mono<ResolvedIds> resolveIds(String userId, String ghStr, Integer zSeq, Integer rSeq) {
        if (userId == null || ghStr == null) return Mono.just(new ResolvedIds(null, null, null));

        return greenhouseRepository.findByOwnerIdAndGreenhouseId(userId, ghStr)
                .switchIfEmpty(greenhouseRepository.findByOwnerIdAndGreenhouseId("user_" + userId, ghStr))
                .flatMap(gh -> {
                    if (zSeq == null) return Mono.just(new ResolvedIds(gh.id(), null, null));
                    return zoneRepository.findByGreenhouseIdAndZoneId(gh.id(), zSeq)
                            .flatMap(z -> {
                                if (rSeq == null) return Mono.just(new ResolvedIds(gh.id(), z.id(), null));
                                return rowRepository.findByZoneIdAndRowId(z.id(), rSeq)
                                        .map(r -> new ResolvedIds(gh.id(), z.id(), r.id()))
                                        .defaultIfEmpty(new ResolvedIds(gh.id(), z.id(), null));
                            })
                            .defaultIfEmpty(new ResolvedIds(gh.id(), null, null));
                })
                .defaultIfEmpty(new ResolvedIds(null, null, null));
    }

    private List<DataRecord> buildDataRecords(String deviceId, JsonNode root, 
                                              String userId, String ghId, String zId, String rId) {
        List<DataRecord> records = new ArrayList<>();
        try {
            JsonNode entries = root.path("e");

            if (entries.isMissingNode() || !entries.isArray()) {
                String[] parts = deviceId.split("/");
                String metric = (parts.length > 0) ? parts[parts.length - 1] : "unknown";
                double v = root.has("v") ? root.path("v").asDouble() : 0.0;
                long t = root.has("t") ? root.path("t").asLong() : 0;
                Instant ts = (t > 0) ? Instant.ofEpochSecond(t) : Instant.now();
                records.add(new DataRecord(null, deviceId, userId, ghId, zId, rId, metric, v, ts));
            } else {
                for (JsonNode entry : entries) {
                    String n = entry.path("n").asText();
                    double v = entry.path("v").asDouble();
                    long t = entry.has("t") ? entry.get("t").asLong() : (root.has("t") ? root.get("t").asLong() : 0);
                    Instant ts = (t > 0) ? Instant.ofEpochSecond(t) : Instant.now();
                    records.add(new DataRecord(null, deviceId, userId, ghId, zId, rId, n, v, ts));
                }
            }
        } catch (Exception e) {
            log.warn("[TelemetryService] Error building data records: {}", e.getMessage());
        }
        return records;
    }

    private Mono<Void> updateLastKnownState(String zoneObjectId, String rowObjectId, List<DataRecord> records) {
        if (rowObjectId != null) {
            return rowRepository.findById(rowObjectId)
                    .flatMap(row -> {
                        Row.RowBuilder builder = Row.builder()
                                .id(row.id())
                                .rowId(row.rowId())
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
                    });
        } else if (zoneObjectId != null) {
            return zoneRepository.findById(zoneObjectId)
                    .flatMap(zone -> {
                        Zone.ZoneBuilder builder = Zone.builder()
                                .id(zone.id())
                                .zoneId(zone.zoneId())
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
                    });
        }
        return Mono.empty();
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
