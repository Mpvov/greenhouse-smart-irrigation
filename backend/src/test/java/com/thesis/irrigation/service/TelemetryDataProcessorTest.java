package com.thesis.irrigation.service;

import com.thesis.irrigation.domain.dto.TelemetryData;
import com.thesis.irrigation.domain.model.DataRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.thesis.irrigation.domain.repository.DataRecordRepository;
import com.thesis.irrigation.domain.repository.GreenhouseRepository;
import com.thesis.irrigation.domain.repository.ZoneRepository;
import com.thesis.irrigation.domain.repository.RowRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mockito.ArgumentMatchers;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TelemetryDataProcessorTest {

    @Mock
    private DataRecordRepository dataRecordRepository;
    @Mock
    private GreenhouseRepository greenhouseRepository;
    @Mock
    private ZoneRepository zoneRepository;
    @Mock
    private RowRepository rowRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private TelemetryDataProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new TelemetryDataProcessor(
                dataRecordRepository, greenhouseRepository,
                zoneRepository, rowRepository, objectMapper);
    }

    @Test
    public void testProcess_ShouldTransformTelemetryDataToDataRecord() {
        // Mock saveAll: trả lại chính các item vừa nhận
        when(dataRecordRepository.saveAll(
                (Iterable<DataRecord>) ArgumentMatchers.<Iterable<DataRecord>>any()))
                .thenAnswer(invocation -> {
                    Iterable<DataRecord> items = invocation.getArgument(0);
                    return Flux.fromIterable(items);
                });

        // FIX #1: Stub greenhouseRepository để tránh NPE âm thầm
        // Unstubbed mock trả null → NPE trong flatMap → bị catch → Mono.empty() → stream rỗng
        // Trả Mono.empty() → resolveIds dùng defaultIfEmpty → DataRecord vẫn được tạo
        when(greenhouseRepository.findByOwnerIdAndGreenhouseId(anyString(), anyString()))
                .thenReturn(Mono.empty());

        TelemetryData msg1 = new TelemetryData("userA/gh_1/z_1/r_1/soil", "{\"v\":45.0}");
        TelemetryData msg2 = new TelemetryData("userA/gh_1/z_1/r_1/pump", "{\"v\":1.0}");

        // FIX #2: withVirtualTime cho phép tua thời gian ảo qua bufferTimeout(1s)
        // mà không phải chờ 1 giây thực tế
        StepVerifier.withVirtualTime(() -> processor.process(Flux.just(msg1, msg2)))
                .thenAwait(Duration.ofSeconds(5)) // advance qua bufferTimeout(100, 1s)
                .expectNextMatches(record ->
                        "soil".equals(record.measurement()) && record.value() == 45.0)
                .expectNextMatches(record ->
                        "pump".equals(record.measurement()) && record.value() == 1.0)
                .verifyComplete();
    }
}
