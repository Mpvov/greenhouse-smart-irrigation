package com.thesis.irrigation.service;

import com.thesis.irrigation.config.MqttGateway;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thesis.irrigation.domain.dto.ControlRequest;
import com.thesis.irrigation.domain.dto.ModeConfigResponse;
import com.thesis.irrigation.domain.dto.ThresholdConfigResponse;
import com.thesis.irrigation.domain.model.ControlLog;
import com.thesis.irrigation.domain.model.DataRecord;
import com.thesis.irrigation.domain.model.Greenhouse;
import com.thesis.irrigation.domain.model.Row;
import com.thesis.irrigation.domain.model.Zone;
import com.thesis.irrigation.domain.repository.ControlLogRepository;
import com.thesis.irrigation.domain.repository.DeviceRepository;
import com.thesis.irrigation.domain.repository.GreenhouseRepository;
import com.thesis.irrigation.domain.repository.RowRepository;
import com.thesis.irrigation.domain.repository.ScheduleRepository;
import com.thesis.irrigation.domain.repository.ZoneRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class IrrigationService {

    private final RowRepository rowRepository;
    private final ZoneRepository zoneRepository;
    private final GreenhouseRepository greenhouseRepository;
    private final DeviceRepository deviceRepository;
    private final ScheduleRepository scheduleRepository;
    private final MqttGateway mqttGateway;
    private final ObjectMapper objectMapper;
    private final ControlLogRepository controlLogRepository;

    public Mono<Row> getById(String id, String ownerId) {
        return rowRepository.findById(id)
                .flatMap(row -> greenhouseRepository.findById(row.greenhouseId())
                        .filter(gh -> gh.ownerId().equals(ownerId))
                        .map(gh -> row));
    }

    // Save control log and publish MQTT message
    private Mono<ControlLog> persistAndPublishControlLog(Row row, String userId, String topic, String action,
            String source) {
        ControlLog controlLog = ControlLog.builder()
                .deviceId(topic)
                .userId(userId)
                .greenhouseId(row.greenhouseId())
                .zoneId(row.zoneId())
                .rowId(row.id())
                .action(action)
                .source(source)
                .timestamp(Instant.now())
                .build();
        return controlLogRepository.save(controlLog)
                .doOnSuccess(saved -> {
                    mqttGateway.sendToMqtt(topic, action);
                    log.info("[{} CTRL] Saved control log id={}, rowId={}, action={}", source,
                            saved.id(), saved.rowId(), saved.action());
                })
                .doOnError(err -> log.error("[{} CTRL] Failed to save control log for rowId={}: {}", source,
                        row.id(), err.getMessage(), err));
    }

    // Manual control On Off Toggle
    public Mono<Void> controlPumpManual(Row row, Zone zone, Greenhouse greenhouse, String userId, String action) {
        String topic = "user_" + userId
                + "/gh_" + greenhouse.greenhouseId()
                + "/z_" + zone.zoneId()
                + "/r_" + row.rowId()
                + "/pump";
        String normalizedAction = action != null ? action : "TOGGLE";
        return persistAndPublishControlLog(row, userId, topic, normalizedAction, "USER").then();
    }

    public Mono<Void> controlPump(String rowId, String userId, ControlRequest request) {
        return getById(rowId, userId)
                .switchIfEmpty(Mono.error(new org.springframework.security.access.AccessDeniedException(
                        "Row not found or access denied")))
                .flatMap(
                        row -> zoneRepository.findById(row.zoneId())
                                .switchIfEmpty(Mono.error(new IllegalStateException("Zone not found")))
                                .flatMap(zone -> greenhouseRepository.findById(zone.greenhouseId())
                                        .switchIfEmpty(Mono.error(new IllegalStateException("Greenhouse not found")))
                                        .flatMap(greenhouse -> controlPumpManual(row, zone,
                                                greenhouse, userId,
                                                request != null && request
                                                        .action() != null
                                                                ? request.action()
                                                                : "TOGGLE"))))
                .then();
    }

    public Mono<ThresholdConfigResponse> updateThreshold(String rowId, String userId, Double thresholdMin,
            Double thresholdMax) {
        return getById(rowId, userId)
                .switchIfEmpty(Mono.error(new org.springframework.security.access.AccessDeniedException(
                        "Row not found or access denied")))
                .flatMap(row -> {
                    if (thresholdMin == null || thresholdMax == null) {
                        return Mono.error(new IllegalArgumentException("thresholdMin and thresholdMax are required"));
                    }
                    if (thresholdMin >= thresholdMax) {
                        return Mono.error(new IllegalArgumentException("thresholdMin must be less than thresholdMax"));
                    }
                    if (thresholdMin < 0.0 || thresholdMax > 100.0 || thresholdMin > 100.0 || thresholdMax < 0.0) {
                        return Mono.error(new IllegalArgumentException("thresholds must be between 0.0 and 100.0"));
                    }

                    Row updatedRow = Row.builder()
                            .id(row.id())
                            .rowId(row.rowId())
                            .zoneId(row.zoneId())
                            .greenhouseId(row.greenhouseId())
                            .name(row.name())
                            .plantType(row.plantType())
                            .currentMode(row.currentMode())
                            .thresholdEnabled(Boolean.TRUE)
                            .thresholdMin(thresholdMin)
                            .thresholdMax(thresholdMax)
                            .lastSoilMoisture(row.lastSoilMoisture())
                            .pumpStatus(row.pumpStatus())
                            .build();

                    return rowRepository.save(updatedRow)
                            .flatMap(saved -> zoneRepository.findById(saved.zoneId())
                                    .switchIfEmpty(Mono.error(new IllegalStateException("Zone not found")))
                                    .flatMap(zone -> greenhouseRepository.findById(saved.greenhouseId())
                                            .switchIfEmpty(
                                                    Mono.error(new IllegalStateException("Greenhouse not found")))
                                            .flatMap(greenhouse -> {
                                                ThresholdConfigResponse response = new ThresholdConfigResponse(
                                                        new ThresholdConfigResponse.ThreshHoldConfig(
                                                                saved.thresholdMin(),
                                                                saved.thresholdMax()));

                                                String topic = String.format(
                                                        "user_%s/gh_%s/z_%d/r_%d/config/threshHold",
                                                        userId,
                                                        greenhouse.greenhouseId(),
                                                        zone.zoneId(),
                                                        saved.rowId());
                                                try {
                                                    mqttGateway.sendToMqtt(topic, 1,
                                                            objectMapper.writeValueAsString(response));
                                                    return Mono.just(response);
                                                } catch (JsonProcessingException e) {
                                                    return Mono.error(new RuntimeException(
                                                            "Failed to serialize threshold config",
                                                            e));
                                                }
                                            })));
                });
    }

    public Mono<ModeConfigResponse> updateMode(String rowId, String userId, String currentMode) {
        return getById(rowId, userId)
                .switchIfEmpty(Mono.error(new org.springframework.security.access.AccessDeniedException(
                        "Row not found or access denied")))
                .flatMap(row -> {
                    if (currentMode == null || currentMode.isBlank()) {
                        return Mono.error(new IllegalArgumentException("currentMode is required"));
                    }

                    String normalizedMode = currentMode.trim().toUpperCase();
                    if (!"AUTO".equals(normalizedMode)
                            && !"MANUAL".equals(normalizedMode)
                            && !"SCHEDULE".equals(normalizedMode)) {
                        return Mono.error(
                                new IllegalArgumentException("currentMode must be AUTO, MANUAL, or SCHEDULE"));
                    }

                    Row updatedRow = Row.builder()
                            .id(row.id())
                            .rowId(row.rowId())
                            .zoneId(row.zoneId())
                            .greenhouseId(row.greenhouseId())
                            .name(row.name())
                            .plantType(row.plantType())
                            .currentMode(normalizedMode)
                            .thresholdEnabled(row.thresholdEnabled())
                            .thresholdMin(row.thresholdMin())
                            .thresholdMax(row.thresholdMax())
                            .lastSoilMoisture(row.lastSoilMoisture())
                            .pumpStatus(row.pumpStatus())
                            .build();

                    return rowRepository.save(updatedRow)
                            .flatMap(saved -> zoneRepository.findById(saved.zoneId())
                                    .switchIfEmpty(Mono.error(new IllegalStateException("Zone not found")))
                                    .flatMap(zone -> greenhouseRepository.findById(saved.greenhouseId())
                                            .switchIfEmpty(
                                                    Mono.error(new IllegalStateException("Greenhouse not found")))
                                            .flatMap(greenhouse -> {
                                                ModeConfigResponse response = new ModeConfigResponse(
                                                        saved.currentMode());

                                                String topic = String.format(
                                                        "user_%s/gh_%s/z_%d/r_%d/config/mode",
                                                        userId,
                                                        greenhouse.greenhouseId(),
                                                        zone.zoneId(),
                                                        saved.rowId());
                                                try {
                                                    mqttGateway.sendToMqtt(topic, 1,
                                                            objectMapper.writeValueAsString(response));
                                                    return Mono.just(response);
                                                } catch (JsonProcessingException e) {
                                                    return Mono.error(new RuntimeException(
                                                            "Failed to serialize mode config",
                                                            e));
                                                }
                                            })));
                });
    }

    public Mono<Void> applySoilThresholdControlIfNeeded(String deviceId, String userId, String rowObjectId,
            List<DataRecord> records) {
        // if (rowObjectId == null) {
        // return Mono.empty();
        // }
        // Double soilValue = records.stream()
        // .filter(r -> "soil".equals(r.measurement()) ||
        // "soil_moisture".equals(r.measurement()))
        // .map(DataRecord::value)
        // .reduce((first, second) -> second)
        // .orElse(null);
        // if (soilValue == null) {
        // return Mono.empty();
        // }
        // return rowRepository.findById(rowObjectId)
        // .flatMap(row -> {
        // if (!Boolean.TRUE.equals(row.thresholdEnabled()) || row.thresholdMin() ==
        // null
        // || row.thresholdMax() == null) {
        // return Mono.empty();
        // }
        // String desiredPumpStatus = null;
        // if (soilValue < row.thresholdMin()) {
        // desiredPumpStatus = "ON";
        // } else if (soilValue > row.thresholdMax()) {
        // desiredPumpStatus = "OFF";
        // }
        // if (desiredPumpStatus == null ||
        // desiredPumpStatus.equalsIgnoreCase(row.pumpStatus())) {
        // return Mono.empty();
        // }
        // String pumpTopic = buildPumpTopic(deviceId, row.rowId());
        // if (pumpTopic == null) {
        // return Mono.empty();
        // }
        // String finalDesiredPumpStatus = desiredPumpStatus;
        // Row updatedRow = Row.builder()
        // .id(row.id())
        // .rowId(row.rowId())
        // .zoneId(row.zoneId())
        // .greenhouseId(row.greenhouseId())
        // .name(row.name())
        // .plantType(row.plantType())
        // .currentMode(row.currentMode())
        // .thresholdEnabled(row.thresholdEnabled())
        // .thresholdMin(row.thresholdMin())
        // .thresholdMax(row.thresholdMax())
        // .lastSoilMoisture(row.lastSoilMoisture())
        // .pumpStatus(finalDesiredPumpStatus)
        // .build();
        // return persistAndPublishControlLog(row, userId, pumpTopic,
        // finalDesiredPumpStatus, "AUTO")
        // .doOnSuccess(saved -> log.info("[AUTO CTRL] soil={} min={} max={} -> pump {}
        // topic={}",
        // soilValue, row.thresholdMin(), row.thresholdMax(), finalDesiredPumpStatus,
        // pumpTopic))
        // .then(rowRepository.save(updatedRow))
        // .then();
        // })
        // .onErrorResume(err -> {
        // log.error("[AUTO CTRL] Failed threshold control for rowId={} deviceId={}",
        // rowObjectId, deviceId,
        // err);
        // return Mono.empty();
        // });
        return Mono.empty();
    }

    private String buildPumpTopic(String deviceId, Integer rowSeq) {
        if (deviceId == null) {
            return null;
        }

        String[] parts = deviceId.split("/");
        if (parts.length < 3) {
            return null;
        }

        if (parts.length >= 4 && parts[3].startsWith("r_")) {
            return parts[0] + "/" + parts[1] + "/" + parts[2] + "/" + parts[3] + "/pump";
        }

        if (rowSeq != null) {
            return parts[0] + "/" + parts[1] + "/" + parts[2] + "/r_" + rowSeq + "/pump";
        }

        return null;
    }

}
