package com.thesis.irrigation.controller;

import com.thesis.irrigation.common.BaseResponse;
import com.thesis.irrigation.domain.model.Row;
import com.thesis.irrigation.domain.repository.GreenhouseRepository;
import com.thesis.irrigation.domain.repository.ZoneRepository;
import com.thesis.irrigation.service.RowService;
import com.thesis.irrigation.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/v1/rows")
@RequiredArgsConstructor
public class RowController {

    private final RowService rowService;
    private final JwtUtil jwtUtil;
    private final com.thesis.irrigation.domain.repository.ControlLogRepository controlLogRepository;
    private final com.thesis.irrigation.config.MqttGateway mqttGateway;
    private final ZoneRepository zoneRepository;
    private final GreenhouseRepository greenhouseRepository;

    private String getUserIdFromHeader(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (jwtUtil.validateToken(token)) {
                return jwtUtil.getUserIdFromToken(token);
            }
        }
        return null;
    }

    @GetMapping("/zone/{zoneId}")
    public Mono<ResponseEntity<BaseResponse<List<Row>>>> getByZone(@PathVariable String zoneId,
            @RequestHeader("Authorization") String authHeader) {
        String userId = getUserIdFromHeader(authHeader);
        if (userId == null)
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());

        return rowService.getByZone(zoneId, userId)
                .collectList()
                .map(list -> ResponseEntity.ok(BaseResponse.success("Success", list)));
    }

    @PostMapping
    public Mono<ResponseEntity<BaseResponse<Row>>> create(@RequestBody Row row,
            @RequestHeader("Authorization") String authHeader) {
        String userId = getUserIdFromHeader(authHeader);
        if (userId == null)
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());

        return rowService.createRow(row, userId)
                .map(saved -> ResponseEntity.status(HttpStatus.CREATED).body(BaseResponse.success("Created", saved)))
                .onErrorResume(e -> Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(BaseResponse.error(400, e.getMessage()))));
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<BaseResponse<Row>>> update(@PathVariable String id, @RequestBody Row row,
            @RequestHeader("Authorization") String authHeader) {
        String userId = getUserIdFromHeader(authHeader);
        if (userId == null)
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());

        return rowService.updateRow(id, row, userId)
                .map(updated -> ResponseEntity.ok(BaseResponse.success("Updated", updated)))
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<BaseResponse<Void>>> delete(@PathVariable String id,
            @RequestHeader("Authorization") String authHeader) {
        String userId = getUserIdFromHeader(authHeader);
        if (userId == null)
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());

        return rowService.deleteRow(id, userId)
                .then(Mono.just(ResponseEntity.ok(BaseResponse.<Void>success("Deleted", null))));
    }

    @PostMapping("/{id}/control")
    public Mono<ResponseEntity<Void>> controlPump(
            @PathVariable String id,
            @RequestBody com.thesis.irrigation.domain.dto.ControlRequest request,
            @RequestHeader("Authorization") String authHeader) {
        String userId = getUserIdFromHeader(authHeader);
        if (userId == null) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }

        return rowService.getById(id, userId)
                .switchIfEmpty(Mono.error(new org.springframework.security.access.AccessDeniedException(
                        "Row not found or access denied")))
                .flatMap(row -> zoneRepository.findById(row.zoneId())
                        .flatMap(zone -> greenhouseRepository.findById(zone.greenhouseId())
                                .flatMap(greenhouse -> {
                                    String topic = "user_" + userId
                                            + "/gh_" + greenhouse.greenhouseId()
                                            + "/z_" + zone.zoneId()
                                            + "/r_" + row.rowId()
                                            + "/pump";

                                    String action = request != null && request.action() != null
                                            ? request.action()
                                            : "TOGGLE";

                                    com.thesis.irrigation.domain.model.ControlLog log = com.thesis.irrigation.domain.model.ControlLog
                                            .builder()
                                            .deviceId(topic)
                                            .userId(userId)
                                            .greenhouseId(row.greenhouseId())
                                            .zoneId(row.zoneId())
                                            .rowId(row.id())
                                            .action(action)
                                            .source("USER")
                                            .timestamp(java.time.Instant.now())
                                            .build();

                                    return controlLogRepository.save(log)
                                            .flatMap(saved -> {
                                                mqttGateway.sendToMqtt(topic, action);
                                                return Mono.just(ResponseEntity.ok().<Void>build());
                                            });
                                })));
    }
}
