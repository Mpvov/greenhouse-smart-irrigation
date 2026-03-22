package com.thesis.irrigation.service;

import com.thesis.irrigation.domain.dto.ConfigRequest;
import reactor.core.publisher.Mono;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class IrrigationConfigServiceImpl implements IrrigationConfigService {
    private final com.thesis.irrigation.domain.repository.RowRepository rowRepository;
    private final com.thesis.irrigation.domain.repository.GreenhouseRepository greenhouseRepository;
    private final com.thesis.irrigation.domain.repository.ZoneRepository zoneRepository;
    private final com.thesis.irrigation.config.MqttGateway mqttGateway;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Override
    public Mono<Void> updateConfig(String rowId, ConfigRequest request) {
        if (request.currentMode() == null) {
            return Mono.error(new IllegalArgumentException("currentMode is required"));
        }
        if (request.thresholdMin() == null || request.thresholdMax() == null) {
            return Mono.error(new IllegalArgumentException("thresholdMin and thresholdMax are required"));
        }
        if (request.thresholdMin() >= request.thresholdMax()) {
            return Mono.error(new IllegalArgumentException("thresholdMin must be less than thresholdMax"));
        }
        if (request.thresholdMin() < 0.0 || request.thresholdMax() > 100.0 || request.thresholdMin() > 100.0 || request.thresholdMax() < 0.0) {
            return Mono.error(new IllegalArgumentException("thresholds must be between 0.0 and 100.0"));
        }

        return org.springframework.security.core.context.ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication().getName())
                .flatMap(tenantId -> rowRepository.findById(rowId)
                        .switchIfEmpty(Mono.error(new org.springframework.security.access.AccessDeniedException("Row not found or access denied")))
                        .flatMap(row -> greenhouseRepository.findById(row.greenhouseId())
                                .filter(gh -> gh.ownerId().equals(tenantId))
                                .switchIfEmpty(Mono.error(new org.springframework.security.access.AccessDeniedException("Access Denied")))
                                .thenReturn(row))
                        .flatMap(row -> {
                            com.thesis.irrigation.domain.model.Row updatedRow = com.thesis.irrigation.domain.model.Row.builder()
                                    .id(row.id())
                                    .zoneId(row.zoneId())
                                    .greenhouseId(row.greenhouseId())
                                    .name(row.name())
                                    .plantType(row.plantType())
                                    .currentMode(request.currentMode())
                                    .thresholdMin(request.thresholdMin())
                                    .thresholdMax(request.thresholdMax())
                                    .lastSoilMoisture(row.lastSoilMoisture())
                                    .pumpStatus(row.pumpStatus())
                                    .build();

                            return rowRepository.save(updatedRow)
                                    .flatMap(saved -> {
                                        try {
                                            String topic = tenantId + "/" + row.greenhouseId() + "/" + row.zoneId() + "/" + row.id() + "/config";
                                            String payload = objectMapper.writeValueAsString(request);
                                            mqttGateway.sendToMqtt(topic, payload);
                                            return Mono.empty();
                                        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                                            return Mono.error(new RuntimeException("Failed to serialize config", e));
                                        }
                                    });
                        }));
    }
}
