package com.thesis.irrigation.service;

import com.thesis.irrigation.domain.dto.ConfigRequest;
import reactor.core.publisher.Mono;

public interface IrrigationConfigService {
    Mono<Void> updateConfig(String rowId, ConfigRequest request);
}
