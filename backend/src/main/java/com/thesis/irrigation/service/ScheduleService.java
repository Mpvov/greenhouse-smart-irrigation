package com.thesis.irrigation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thesis.irrigation.config.MqttGateway;
import com.thesis.irrigation.domain.model.Greenhouse;
import com.thesis.irrigation.domain.model.Row;
import com.thesis.irrigation.domain.model.Schedule;
import com.thesis.irrigation.domain.repository.GreenhouseRepository;
import com.thesis.irrigation.domain.repository.RowRepository;
import com.thesis.irrigation.domain.repository.ScheduleRepository;
import com.thesis.irrigation.domain.repository.ZoneRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduleService {

        private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

        private final ScheduleRepository scheduleRepository;
        private final RowRepository rowRepository;
        private final GreenhouseRepository greenhouseRepository;
        private final ZoneRepository zoneRepository;
        private final MqttGateway mqttGateway;
        private final ObjectMapper objectMapper;

        public Flux<Schedule> getByRow(String rowId, String ownerId) {
                return rowRepository.findById(rowId)
                                .flatMapMany(row -> greenhouseRepository.findById(row.greenhouseId())
                                                .filter(gh -> gh.ownerId().equals(ownerId))
                                                .flatMapMany(gh -> scheduleRepository.findByRowId(rowId)));
        }

        public Mono<Schedule> createSchedule(Schedule schedule, String ownerId) {
                return validateInput(schedule)
                                .then(loadOwnedRowContext(schedule.rowId(), ownerId))
                                .flatMap(ctx -> scheduleRepository.findByRowIdAndIsActiveTrue(ctx.row().id())
                                                .collectList()
                                                .flatMap(activeSchedules -> {
                                                        validateNoOverlap(schedule.startTime(), schedule.duration(),
                                                                        activeSchedules);

                                                        Schedule toSave = Schedule.builder()
                                                                        .id(null)
                                                                        .rowId(ctx.row().id())
                                                                        .startTime(schedule.startTime())
                                                                        .duration(schedule.duration())
                                                                        .isActive(true)
                                                                        .createdAt(Instant.now())
                                                                        .build();

                                                        return scheduleRepository.save(toSave)
                                                                        .flatMap(saved -> publishActiveSchedules(ctx,
                                                                                        ownerId).thenReturn(saved));
                                                }))
                                .switchIfEmpty(Mono.error(new RuntimeException("Row not found or access denied")));
        }

        public Mono<Void> deleteSchedule(String id, String ownerId) {
                return scheduleRepository.findById(id)
                                .flatMap(schedule -> loadOwnedRowContext(schedule.rowId(), ownerId)
                                                .flatMap(ctx -> scheduleRepository.delete(schedule)
                                                                .then(publishActiveSchedules(ctx, ownerId))))
                                .then();
        }

        private Mono<Void> validateInput(Schedule schedule) {
                if (schedule == null || schedule.rowId() == null || schedule.rowId().isBlank()) {
                        return Mono.error(new RuntimeException("rowId is required"));
                }
                if (schedule.startTime() == null || schedule.startTime().isBlank()) {
                        return Mono.error(new RuntimeException("startTime is required"));
                }
                if (schedule.duration() == null || schedule.duration() <= 0) {
                        return Mono.error(new RuntimeException("duration must be greater than 0"));
                }

                int startMinutes = toMinutes(schedule.startTime());
                int endMinutes = startMinutes + schedule.duration();
                if (endMinutes > 24 * 60) {
                        return Mono.error(new RuntimeException("schedule must end within the same day"));
                }

                return Mono.empty();
        }

        private Mono<RowContext> loadOwnedRowContext(String rowId, String ownerId) {
                return rowRepository.findById(rowId)
                                .flatMap(row -> greenhouseRepository.findById(row.greenhouseId())
                                                .filter(gh -> gh.ownerId().equals(ownerId))
                                                .map(gh -> new RowContext(row, gh)));
        }

        private void validateNoOverlap(String startTime, Integer duration, List<Schedule> activeSchedules) {
                int newStart = toMinutes(startTime);
                int newEnd = newStart + duration;

                for (Schedule existing : activeSchedules) {
                        if (existing.startTime() == null || existing.duration() == null || existing.duration() <= 0) {
                                continue;
                        }

                        int existingStart;
                        try {
                                existingStart = toMinutes(existing.startTime());
                        } catch (RuntimeException ignored) {
                                continue;
                        }

                        int existingEnd = existingStart + existing.duration();
                        if (newStart < existingEnd && existingStart < newEnd) {
                                throw new RuntimeException("Schedule overlaps with existing active schedule");
                        }
                }
        }

        private Mono<Void> publishActiveSchedules(RowContext ctx, String ownerId) {
                return scheduleRepository.findByRowIdAndIsActiveTrue(ctx.row().id())
                                .sort(Comparator.comparing(Schedule::startTime))
                                .collectList()
                                .flatMap(activeSchedules -> zoneRepository.findById(ctx.row().zoneId())
                                                .switchIfEmpty(Mono.error(new RuntimeException("Zone not found")))
                                                .flatMap(zone -> {
                                                        String topic = String.format(
                                                                        "user_%s/gh_%s/z_%d/r_%d/config/schedule",
                                                                        ownerId,
                                                                        ctx.greenhouse().greenhouseId(),
                                                                        zone.zoneId(),
                                                                        ctx.row().rowId());

                                                        ScheduleConfigPayload payload = new ScheduleConfigPayload(
                                                                        "SCHEDULES_SYNC",
                                                                        activeSchedules.stream()
                                                                                        .map(s -> new ActiveSchedulePayload(
                                                                                                        s.id(),
                                                                                                        s.startTime(),
                                                                                                        s.duration(),
                                                                                                        Boolean.TRUE.equals(
                                                                                                                        s.isActive())))
                                                                                        .toList());

                                                        try {
                                                                mqttGateway.sendToMqtt(topic, 1, objectMapper
                                                                                .writeValueAsString(payload));
                                                                return Mono.empty();
                                                        } catch (JsonProcessingException e) {
                                                                return Mono.error(new RuntimeException(
                                                                                "Failed to serialize active schedules",
                                                                                e));
                                                        }
                                                }));
        }

        private int toMinutes(String hhmm) {
                try {
                        LocalTime t = LocalTime.parse(hhmm, TIME_FORMATTER);
                        return t.getHour() * 60 + t.getMinute();
                } catch (DateTimeParseException ex) {
                        throw new RuntimeException("startTime must be in HH:mm format");
                }
        }

        private record RowContext(Row row, Greenhouse greenhouse) {
        }

        private record ActiveSchedulePayload(String id, String start_time, Integer duration_mins, boolean is_active) {
        }

        private record ScheduleConfigPayload(String type, List<ActiveSchedulePayload> schedules) {
        }
}
