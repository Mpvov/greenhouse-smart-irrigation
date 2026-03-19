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
        String zoneId,
        String name,
        String plantType,       // e.g., "Cà chua", "Dưa leo"
        String currentMode,     // "AUTO" | "MANUAL"
        Double lastSoilMoisture,
        String pumpStatus       // "ON" | "OFF"
) {}
