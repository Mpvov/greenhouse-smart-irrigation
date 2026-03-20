package com.thesis.irrigation.service;

import com.thesis.irrigation.domain.model.Schedule;
import com.thesis.irrigation.domain.repository.GreenhouseRepository;
import com.thesis.irrigation.domain.repository.RowRepository;
import com.thesis.irrigation.domain.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final RowRepository rowRepository;
    private final GreenhouseRepository greenhouseRepository;

    public Flux<Schedule> getByRow(String rowId, String ownerId) {
        return rowRepository.findById(rowId)
                .flatMapMany(row -> greenhouseRepository.findById(row.greenhouseId())
                        .filter(gh -> gh.ownerId().equals(ownerId))
                        .flatMapMany(gh -> scheduleRepository.findByRowId(rowId)));
    }

    public Mono<Schedule> createSchedule(Schedule schedule, String ownerId) {
        return rowRepository.findById(schedule.rowId())
                .flatMap(row -> greenhouseRepository.findById(row.greenhouseId())
                        .filter(gh -> gh.ownerId().equals(ownerId))
                        .flatMap(gh -> {
                            Schedule toSave = Schedule.builder()
                                    .id(null)
                                    .rowId(schedule.rowId())
                                    .startTime(schedule.startTime())
                                    .duration(schedule.duration())
                                    .isActive(true)
                                    .createdAt(Instant.now())
                                    .build();
                            return scheduleRepository.save(toSave);
                        }))
                .switchIfEmpty(Mono.error(new RuntimeException("Row not found or access denied")));
    }

    public Mono<Void> deleteSchedule(String id, String ownerId) {
        return scheduleRepository.findById(id)
                .flatMap(schedule -> rowRepository.findById(schedule.rowId())
                        .flatMap(row -> greenhouseRepository.findById(row.greenhouseId())
                                .filter(gh -> gh.ownerId().equals(ownerId))
                                .flatMap(gh -> scheduleRepository.delete(schedule))))
                .then();
    }
}
