package com.thesis.irrigation.domain.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.TimeSeries;

import java.time.Instant;

/**
 * Stores time-series telemetry data from IoT sensors (temperature, humidity, soil moisture).
 *
 * CRITICAL: This document MUST use @TimeSeries annotation to enable MongoDB's native
 * time-series collection optimization for IoT data ingestion and compression.
 *
 * - timeField  = "timestamp" : The primary time dimension for ordering and bucketing data.
 * - metaField  = "deviceId"  : Groups data by device, enabling efficient range queries per device.
 *
 * Maps to the 'data_records' MongoDB collection (must be pre-created as a time-series collection).
 */
@Document(collection = "data_records")
@TimeSeries(timeField = "timestamp", metaField = "deviceId")
public record DataRecord(
        @Id
        String id,

        String deviceId,     // metaField: Groups data by device (e.g., "device-001")
        String measurement,  // Type of measurement: "temperature" | "humidity" | "soil_moisture"
        Double value,        // Measured value
        Instant timestamp    // timeField: Mandatory time anchor for time-series bucketing
) {}
