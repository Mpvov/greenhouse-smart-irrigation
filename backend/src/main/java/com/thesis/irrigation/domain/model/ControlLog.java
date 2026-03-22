package com.thesis.irrigation.domain.model;

import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.TimeSeries;

import java.time.Instant;

@Document(collection = "control_logs")
@TimeSeries(timeField = "timestamp", metaField = "deviceId")
@Builder
public record ControlLog(
        @Id
        String id,

        String deviceId,     // metaField: Full topic path
        String userId,
        String greenhouseId,
        String zoneId,
        String rowId,

        String action,       // "start" or "stop"
        String source,       // "MANUAL", "AUTO", "SCHEDULE", "AI"
        Instant timestamp
) {}
