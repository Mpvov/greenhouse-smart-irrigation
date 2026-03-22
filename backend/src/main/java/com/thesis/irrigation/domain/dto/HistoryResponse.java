package com.thesis.irrigation.domain.dto;

import com.thesis.irrigation.domain.model.ControlLog;
import com.thesis.irrigation.domain.model.DataRecord;

import java.util.List;

public record HistoryResponse(
        String rowId,
        List<DataRecord> telemetryData,
        List<ControlLog> pumpLogs
) {}
