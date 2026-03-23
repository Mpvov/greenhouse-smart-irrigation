package com.thesis.irrigation.service;

import com.thesis.irrigation.domain.model.Row;
import com.thesis.irrigation.domain.repository.DeviceRepository;
import com.thesis.irrigation.domain.repository.GreenhouseRepository;
import com.thesis.irrigation.domain.repository.RowRepository;
import com.thesis.irrigation.domain.repository.ScheduleRepository;
import com.thesis.irrigation.domain.repository.ZoneRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class RowService {

    private final RowRepository rowRepository;
    private final ZoneRepository zoneRepository;
    private final GreenhouseRepository greenhouseRepository;
    private final DeviceRepository deviceRepository;
    private final ScheduleRepository scheduleRepository;

    public Flux<Row> getByZone(String zoneId, String ownerId) {
        return zoneRepository.findById(zoneId)
                .flatMapMany(zone -> greenhouseRepository.findById(zone.greenhouseId())
                        .filter(gh -> gh.ownerId().equals(ownerId))
                        .flatMapMany(gh -> rowRepository.findByZoneId(zoneId)));
    }

    public Mono<Row> getById(String id, String ownerId) {
        return rowRepository.findById(id)
                .flatMap(row -> greenhouseRepository.findById(row.greenhouseId())
                        .filter(gh -> gh.ownerId().equals(ownerId))
                        .map(gh -> row));
    }

    public Mono<Row> createRow(Row row, String ownerId) {
        return zoneRepository.findById(row.zoneId())
                .flatMap(zone -> greenhouseRepository.findById(zone.greenhouseId())
                        .filter(gh -> gh.ownerId().equals(ownerId))
                        .flatMap(gh -> rowRepository.findByZoneId(zone.id())
                                .map(Row::rowId)
                                .reduce(Math::max)
                                .defaultIfEmpty(0)
                                .map(maxId -> maxId + 1)
                                .flatMap(nextId -> {
                                    Row toSave = Row.builder()
                                            .rowId(nextId)
                                            .zoneId(row.zoneId())
                                            .greenhouseId(zone.greenhouseId())
                                            .name(row.name())
                                            .plantType(row.plantType())
                                            .currentMode(row.currentMode() != null ? row.currentMode() : "AUTO")
                                            .thresholdMin(row.thresholdMin())
                                            .thresholdMax(row.thresholdMax())
                                            .lastSoilMoisture(0.0)
                                            .pumpStatus("OFF")
                                            .build();
                                    return rowRepository.save(toSave);
                                })))
                .switchIfEmpty(Mono.error(new RuntimeException("Zone not found or access denied")));
    }

    public Mono<Row> updateRow(String id, Row row, String ownerId) {
        return getById(id, ownerId)
                .flatMap(existing -> {
                    Row updated = Row.builder()
                            .id(id)
                            .rowId(existing.rowId())
                            .zoneId(existing.zoneId())
                            .greenhouseId(existing.greenhouseId())
                            .name(row.name())
                            .plantType(row.plantType())
                            .currentMode(row.currentMode())
                            .thresholdMin(row.thresholdMin())
                            .thresholdMax(row.thresholdMax())
                            .lastSoilMoisture(existing.lastSoilMoisture())
                            .pumpStatus(existing.pumpStatus())
                            .build();
                    return rowRepository.save(updated);
                });
    }

    @Transactional
    public Mono<Void> deleteRow(String id, String ownerId) {
        return getById(id, ownerId)
                .flatMap(row -> {
                    // 1. Delete devices in this row
                    Mono<Void> deleteDevices = deviceRepository.deleteByRowId(id);
                    // 2. Delete schedules in this row
                    Mono<Void> deleteSchedules = scheduleRepository.deleteByRowId(id);
                    // 3. Delete row itself
                    return deleteDevices.then(deleteSchedules).then(rowRepository.delete(row));
                });
    }
}
