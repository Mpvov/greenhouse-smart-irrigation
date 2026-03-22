package com.thesis.irrigation.domain.dto;

public record ConfigRequest(
    String currentMode,
    Double thresholdMin,
    Double thresholdMax
) {}
