package com.thesis.irrigation.domain.dto;

public record TelemetryData(
    String topic,
    String payload
) {}
