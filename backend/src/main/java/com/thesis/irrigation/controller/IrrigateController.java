package com.thesis.irrigation.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;

import com.thesis.irrigation.domain.dto.ThresholdConfigResponse;
import com.thesis.irrigation.domain.dto.ThresholdRequest;
import com.thesis.irrigation.service.IrrigationService;
import com.thesis.irrigation.utils.JwtUtil;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/devices/irrigate")
@RequiredArgsConstructor
public class IrrigateController {
    private final IrrigationService irrigationService;
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

    @PostMapping("/{id}/control")
    public Mono<ResponseEntity<Void>> controlPump(
            @PathVariable String id,
            @RequestBody com.thesis.irrigation.domain.dto.ControlRequest request,
            @RequestHeader("Authorization") String authHeader) {
        String userId = getUserIdFromHeader(authHeader);
        if (userId == null) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }

        return irrigationService.controlPump(id, userId, request)
                .thenReturn(ResponseEntity.ok().<Void>build())
                .onErrorResume(org.springframework.security.access.AccessDeniedException.class,
                        e -> Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).build()))
                .onErrorResume(IllegalStateException.class,
                        e -> Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).build()));
    }

    @PutMapping("/{id}/threshold")
    public Mono<ResponseEntity<ThresholdConfigResponse>> updateThreshold(
            @PathVariable String id,
            @RequestBody ThresholdRequest request,
            @RequestHeader("Authorization") String authHeader) {
        String userId = getUserIdFromHeader(authHeader);
        if (userId == null) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }

        return irrigationService.updateThreshold(id, userId,
                request != null ? request.thresholdMin() : null,
                request != null ? request.thresholdMax() : null)
                .map(ResponseEntity::ok)
                .onErrorResume(org.springframework.security.access.AccessDeniedException.class,
                        e -> Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).build()))
                .onErrorResume(IllegalStateException.class,
                        e -> Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).build()))
                .onErrorResume(IllegalArgumentException.class,
                        e -> Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).build()));
    }

}
