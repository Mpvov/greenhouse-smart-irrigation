package com.thesis.irrigation.domain.model;

import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Represents a physical hardware device (sensor or actuator).
 */
@Document(collection = "devices")
@Builder
public record Device(
        @Id
        String id,              // Device MAC/Serial
        String type,            // DHT20 | SOIL_MOISTURE | PUMP
        String greenhouseId,
        String zoneId,
        String rowId
) {}
