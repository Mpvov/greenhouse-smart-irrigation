package com.thesis.irrigation.service;

import com.thesis.irrigation.domain.dto.TelemetryData;
import com.thesis.irrigation.domain.model.DataRecord;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.thesis.irrigation.domain.repository.DataRecordRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mockito.ArgumentMatchers;
import static org.mockito.Mockito.when;
import java.util.List;

@ExtendWith(MockitoExtension.class)
public class TelemetryDataProcessorTest {

    @Mock
    private DataRecordRepository dataRecordRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private TelemetryDataProcessor processor;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        processor = new TelemetryDataProcessor(dataRecordRepository, objectMapper);
    }

    @Test
    public void testProcess_ShouldTransformTelemetryDataToDataRecord() {
        // Mock saveAll to return the input flux
        when(dataRecordRepository.saveAll((Iterable<DataRecord>) ArgumentMatchers.<Iterable<DataRecord>>any()))
                .thenAnswer(invocation -> {
                    Iterable<DataRecord> items = invocation.getArgument(0);
                    return Flux.fromIterable(items);
                });

        // Mock a stream of MQTT TelemetryData
        TelemetryData msg1 = new TelemetryData("userA/gh1/z1/r1/soil", "{\"v\":45.0}");
        TelemetryData msg2 = new TelemetryData("userA/gh1/z1/r1/pump", "{\"v\":1.0}");

        Flux<TelemetryData> sourceFlux = Flux.just(msg1, msg2);

        // Process stream
        Flux<DataRecord> resultFlux = processor.process(sourceFlux);

        // Use StepVerifier to ensure non-blocking and proper transformation
        StepVerifier.create(resultFlux)
                // In TDD, this test will fail until we actually implement the mapping
                // Expect mapping to DataRecord, e.g., checking measurement and value.
                .expectNextMatches(record -> 
                         "soil".equals(record.measurement()) && record.value() == 45.0)
                .expectNextMatches(record -> 
                         "pump".equals(record.measurement()) && record.value() == 1.0)
                .verifyComplete();
    }
}
