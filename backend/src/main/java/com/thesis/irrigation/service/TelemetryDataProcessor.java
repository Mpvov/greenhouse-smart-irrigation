package com.thesis.irrigation.service;

import com.thesis.irrigation.domain.dto.TelemetryData;
import com.thesis.irrigation.domain.model.DataRecord;
import reactor.core.publisher.Flux;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TelemetryDataProcessor {
    private final com.thesis.irrigation.domain.repository.DataRecordRepository dataRecordRepository;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    public Flux<DataRecord> process(Flux<TelemetryData> telemetryFlux) {
        return telemetryFlux
                .mapNotNull(telemetry -> {
                    try {
                        com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(telemetry.payload());
                        if (!root.has("v")) return null; // Filter bad format

                        String[] parts = telemetry.topic().split("/");
                        String userId = parts.length > 0 ? parts[0] : "unknown";
                        String ghId = parts.length > 1 ? parts[1] : null;
                        String zoneId = parts.length > 2 ? parts[2] : null;
                        String rowId = parts.length > 3 ? parts[3] : null;
                        String metric = parts.length > 4 ? parts[4] : "unknown";

                        double value = root.get("v").asDouble();

                        return new DataRecord(
                                null,
                                telemetry.topic(),
                                userId,
                                ghId,
                                zoneId,
                                rowId,
                                metric,
                                value,
                                java.time.Instant.now()
                        );
                    } catch (Exception e) {
                        return null; // Skip bad format
                    }
                })
                .bufferTimeout(100, java.time.Duration.ofSeconds(1))
                .flatMap(batch -> dataRecordRepository.saveAll(batch));
    }
}
