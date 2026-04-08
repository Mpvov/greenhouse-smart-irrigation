package com.thesis.irrigation.domain.dto;

public record ThresholdRequest(
        Double thresholdMin,
        Double thresholdMax) {
}