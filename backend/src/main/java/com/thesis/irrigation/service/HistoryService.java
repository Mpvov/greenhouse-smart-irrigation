package com.thesis.irrigation.service;

import com.thesis.irrigation.domain.dto.HistoryResponse;
import com.thesis.irrigation.domain.repository.ControlLogRepository;
import com.thesis.irrigation.domain.repository.DataRecordRepository;
import com.thesis.irrigation.domain.repository.RowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class HistoryService {

    private final DataRecordRepository dataRecordRepository;
    private final ControlLogRepository controlLogRepository;
    private final RowRepository rowRepository;

    public Mono<HistoryResponse> getRowHistory(String rowId, Integer hours) {
        Instant to = Instant.now();
        Instant from = to.minus(hours != null ? hours : 24, ChronoUnit.HOURS);

        return rowRepository.findById(rowId)
                .flatMap(row -> Mono.zip(
                        dataRecordRepository.findByRowOrZoneParent(rowId, row.zoneId(), from, to).collectList(),
                        controlLogRepository.findByDeviceIdAndTimestampBetweenOrderByTimestampAsc(rowId, from, to).collectList()
                ).map(tuple -> new HistoryResponse(rowId, tuple.getT1(), tuple.getT2())));
    }
}
