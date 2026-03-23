package com.thesis.irrigation.domain.model;

import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Level 3: Row (Micro parameters like Soil Moisture / Pump status)
 */
@Document(collection = "rows")
@Builder
public record Row(
        @Id
        String id,
        Integer rowId,
        String zoneId,
        String greenhouseId,
        String name,
        String plantType,       // e.g., "Cà chua", "Dưa leo"
        String currentMode,     // "AUTO" | "MANUAL"
        Double thresholdMin,
        Double thresholdMax,
        Double lastSoilMoisture,
        String pumpStatus       // "ON" | "OFF"
) {}
