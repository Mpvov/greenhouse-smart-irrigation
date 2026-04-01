package com.thesis.irrigation.service;

import com.thesis.irrigation.config.MqttGateway;
import com.thesis.irrigation.domain.model.ControlLog;
import com.thesis.irrigation.domain.model.DataRecord;
import com.thesis.irrigation.domain.model.Greenhouse;
import com.thesis.irrigation.domain.model.Row;
import com.thesis.irrigation.domain.model.Zone;
import com.thesis.irrigation.domain.repository.ControlLogRepository;
import com.thesis.irrigation.domain.repository.RowRepository;
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
    private final ControlLogRepository controlLogRepository;
    private final MqttGateway mqttGateway;

    public Mono<Void> controlPumpManual(Row row, Zone zone, Greenhouse greenhouse, String userId, String action) {
        String topic = "user_" + userId
                + "/gh_" + greenhouse.greenhouseId()
                + "/z_" + zone.zoneId()
                + "/r_" + row.rowId()
                + "/pump";

        String normalizedAction = action != null ? action : "TOGGLE";
        return persistAndPublishControlLog(row, userId, topic, normalizedAction, "USER").then();
    }

    public Mono<Void> applySoilThresholdControlIfNeeded(String deviceId, String userId, String rowObjectId,
            List<DataRecord> records) {
        if (rowObjectId == null) {
            return Mono.empty();
        }

        Double soilValue = records.stream()
                .filter(r -> "soil".equals(r.measurement()) || "soil_moisture".equals(r.measurement()))
                .map(DataRecord::value)
                .reduce((first, second) -> second)
                .orElse(null);

        if (soilValue == null) {
            return Mono.empty();
        }

        return rowRepository.findById(rowObjectId)
                .flatMap(row -> {
                    if (!Boolean.TRUE.equals(row.thresholdEnabled()) || row.thresholdMin() == null
                            || row.thresholdMax() == null) {
                        return Mono.empty();
                    }

                    String desiredPumpStatus = null;
                    if (soilValue < row.thresholdMin()) {
                        desiredPumpStatus = "ON";
                    } else if (soilValue > row.thresholdMax()) {
                        desiredPumpStatus = "OFF";
                    }

                    if (desiredPumpStatus == null || desiredPumpStatus.equalsIgnoreCase(row.pumpStatus())) {
                        return Mono.empty();
                    }

                    String pumpTopic = buildPumpTopic(deviceId, row.rowId());
                    if (pumpTopic == null) {
                        return Mono.empty();
                    }

                    String finalDesiredPumpStatus = desiredPumpStatus;

                    Row updatedRow = Row.builder()
                            .id(row.id())
                            .rowId(row.rowId())
                            .zoneId(row.zoneId())
                            .greenhouseId(row.greenhouseId())
                            .name(row.name())
                            .plantType(row.plantType())
                            .currentMode(row.currentMode())
                            .thresholdEnabled(row.thresholdEnabled())
                            .thresholdMin(row.thresholdMin())
                            .thresholdMax(row.thresholdMax())
                            .lastSoilMoisture(row.lastSoilMoisture())
                            .pumpStatus(finalDesiredPumpStatus)
                            .build();

                    return persistAndPublishControlLog(row, userId, pumpTopic, finalDesiredPumpStatus, "AUTO")
                            .doOnSuccess(saved -> log.info("[AUTO CTRL] soil={} min={} max={} -> pump {} topic={}",
                                    soilValue, row.thresholdMin(), row.thresholdMax(), finalDesiredPumpStatus,
                                    pumpTopic))
                            .then(rowRepository.save(updatedRow))
                            .then();
                })
                .onErrorResume(err -> {
                    log.error("[AUTO CTRL] Failed threshold control for rowId={} deviceId={}", rowObjectId, deviceId,
                            err);
                    return Mono.empty();
                });
    }

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
