package com.thesis.irrigation.domain.model;

import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

/**
 * System alert for a User.
 */
@Document(collection = "alerts")
@Builder
public record Alert(
        @Id
        String id,
        String userId,
        String level,       // INFO | WARNING | CRITICAL
        String errorCode,   // WATER_LOW | SENSOR_FAIL | DEVICE_OFFLINE
        String message,
        Instant timestamp
) {}
