package com.thesis.irrigation.service;

import com.thesis.irrigation.domain.dto.TelemetryData;
import com.thesis.irrigation.domain.model.DataRecord;
import com.thesis.irrigation.domain.repository.DataRecordRepository;
import com.thesis.irrigation.domain.repository.GreenhouseRepository;
import com.thesis.irrigation.domain.repository.ZoneRepository;
import com.thesis.irrigation.domain.repository.RowRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TelemetryDataProcessor {

    private final DataRecordRepository dataRecordRepository;
    private final GreenhouseRepository greenhouseRepository;
    private final ZoneRepository zoneRepository;
    private final RowRepository rowRepository;
    private final ObjectMapper objectMapper;

    public Flux<DataRecord> process(Flux<TelemetryData> telemetryFlux) {
        return telemetryFlux
                .flatMap(telemetry -> {
                    try {
                        JsonNode root = objectMapper.readTree(telemetry.payload());
                        if (!root.has("v")) return Mono.empty();

                        String[] parts = telemetry.topic().split("/");
                        String userId = parts.length > 0 ? extractId(parts[0], "user_") : null;
                        String ghStr = parts.length > 1 ? extractId(parts[1], "gh_") : null;
                        String zStr = parts.length > 2 ? extractId(parts[2], "z_") : null;
                        String rStr = parts.length > 3 && parts[3].startsWith("r_") ? extractId(parts[3], "r_") : null;
                        String metric = parts.length > 4 ? parts[4] : (parts.length > 3 && !parts[3].startsWith("r_") ? parts[3] : "unknown");

                        Integer zSeq = parseOrNull(zStr);
                        Integer rSeq = parseOrNull(rStr);
                        double value = root.get("v").asDouble();

                        return resolveIds(userId, ghStr, zSeq, rSeq)
                                .map(ids -> new DataRecord(
                                        null,
                                        telemetry.topic(),
                                        userId,
                                        ids.ghId,
                                        ids.zId,
                                        ids.rId,
                                        metric,
                                        value,
                                        java.time.Instant.now()
                                ));
                    } catch (Exception e) {
                        return Mono.<DataRecord>empty();
                    }
                })
                .bufferTimeout(100, java.time.Duration.ofSeconds(1))
                .flatMap(dataRecordRepository::saveAll);
    }

    private static class ResolvedIds {
        String ghId, zId, rId;
        ResolvedIds(String gh, String z, String r) { ghId = gh; zId = z; rId = r; }
    }

    private Mono<ResolvedIds> resolveIds(String userId, String ghStr, Integer zSeq, Integer rSeq) {
        if (userId == null || ghStr == null) return Mono.just(new ResolvedIds(null, null, null));

        return greenhouseRepository.findByOwnerIdAndGreenhouseId(userId, ghStr)
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

    private String extractId(String part, String prefix) {
        return part.startsWith(prefix) ? part.substring(prefix.length()) : part;
    }

    private Integer parseOrNull(String s) {
        if (s == null) return null;
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
