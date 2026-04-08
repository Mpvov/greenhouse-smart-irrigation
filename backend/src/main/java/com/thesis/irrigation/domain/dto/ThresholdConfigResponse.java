package com.thesis.irrigation.domain.dto;

public record ThresholdConfigResponse(
        ThreshHoldConfig threshHoldConfig) {

    public record ThreshHoldConfig(
            Double threshHoldMin,
            Double threshHoldMax) {
    }
}