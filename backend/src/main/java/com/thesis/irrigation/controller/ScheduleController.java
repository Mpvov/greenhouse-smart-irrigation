package com.thesis.irrigation.controller;

import com.thesis.irrigation.common.BaseResponse;
import com.thesis.irrigation.domain.model.Schedule;
import com.thesis.irrigation.service.ScheduleService;
import com.thesis.irrigation.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/v1/schedules")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;
    private final JwtUtil jwtUtil;

    private String getUserIdFromHeader(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (jwtUtil.validateToken(token)) {
                return jwtUtil.getUserIdFromToken(token);
            }
        }
        return null;
    }

    @GetMapping("/row/{rowId}")
    public Mono<ResponseEntity<BaseResponse<List<Schedule>>>> getByRow(@PathVariable String rowId, @RequestHeader("Authorization") String authHeader) {
        String userId = getUserIdFromHeader(authHeader);
        if (userId == null) return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());

        return scheduleService.getByRow(rowId, userId)
                .collectList()
                .map(list -> ResponseEntity.ok(BaseResponse.success("Success", list)));
    }

    @PostMapping
    public Mono<ResponseEntity<BaseResponse<Schedule>>> create(@RequestBody Schedule schedule, @RequestHeader("Authorization") String authHeader) {
        String userId = getUserIdFromHeader(authHeader);
        if (userId == null) return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());

        return scheduleService.createSchedule(schedule, userId)
                .map(saved -> ResponseEntity.status(HttpStatus.CREATED).body(BaseResponse.success("Created", saved)))
                .onErrorResume(e -> Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(BaseResponse.error(400, e.getMessage()))));
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<BaseResponse<Void>>> delete(@PathVariable String id, @RequestHeader("Authorization") String authHeader) {
        String userId = getUserIdFromHeader(authHeader);
        if (userId == null) return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());

        return scheduleService.deleteSchedule(id, userId)
                .then(Mono.just(ResponseEntity.ok(BaseResponse.<Void>success("Deleted", null))));
    }
}
