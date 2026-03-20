package com.thesis.irrigation.controller;

import com.thesis.irrigation.common.BaseResponse;
import com.thesis.irrigation.domain.model.Alert;
import com.thesis.irrigation.domain.repository.AlertRepository;
import com.thesis.irrigation.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertRepository alertRepository;
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

    @GetMapping
    public Mono<ResponseEntity<BaseResponse<List<Alert>>>> getAll(@RequestHeader("Authorization") String authHeader) {
        String userId = getUserIdFromHeader(authHeader);
        if (userId == null) return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());

        return alertRepository.findByUserIdOrderByTimestampDesc(userId)
                .collectList()
                .map(list -> ResponseEntity.ok(BaseResponse.success("Success", list)));
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<BaseResponse<Void>>> delete(@PathVariable String id, @RequestHeader("Authorization") String authHeader) {
        String userId = getUserIdFromHeader(authHeader);
        if (userId == null) return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());

        return alertRepository.findById(id)
                .filter(a -> a.userId().equals(userId))
                .flatMap(alertRepository::delete)
                .then(Mono.just(ResponseEntity.ok(BaseResponse.<Void>success("Alert dismissed", null))));
    }
}
