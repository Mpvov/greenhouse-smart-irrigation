package com.thesis.irrigation.domain.model;

import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Level 2: Zone (Macro parameters like Temp/Humidity)
 */
@Document(collection = "zones")
@Builder
public record Zone(
        @Id
        String id,
        Integer zoneId,
        String greenhouseId,
        String name,
        Double lastTemperature,
        Double lastHumidity
) {}
