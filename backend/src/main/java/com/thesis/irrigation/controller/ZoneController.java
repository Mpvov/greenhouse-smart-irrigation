package com.thesis.irrigation.controller;

import com.thesis.irrigation.common.BaseResponse;
import com.thesis.irrigation.domain.model.Zone;
import com.thesis.irrigation.service.ZoneService;
import com.thesis.irrigation.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/v1/zones")
@RequiredArgsConstructor
public class ZoneController {

    private final ZoneService zoneService;
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

    @GetMapping("/greenhouse/{greenhouseId}")
    public Mono<ResponseEntity<BaseResponse<List<Zone>>>> getByGreenhouse(@PathVariable String greenhouseId, @RequestHeader("Authorization") String authHeader) {
        String userId = getUserIdFromHeader(authHeader);
        if (userId == null) return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());

        return zoneService.getByGreenhouse(greenhouseId, userId)
                .collectList()
                .map(list -> ResponseEntity.ok(BaseResponse.success("Success", list)));
    }

    @PostMapping
    public Mono<ResponseEntity<BaseResponse<Zone>>> create(@RequestBody Zone zone, @RequestHeader("Authorization") String authHeader) {
        String userId = getUserIdFromHeader(authHeader);
        if (userId == null) return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());

        return zoneService.createZone(zone, userId)
                .map(saved -> ResponseEntity.status(HttpStatus.CREATED).body(BaseResponse.success("Created", saved)))
                .onErrorResume(e -> Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(BaseResponse.error(400, e.getMessage()))));
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<BaseResponse<Zone>>> update(@PathVariable String id, @RequestBody Zone zone, @RequestHeader("Authorization") String authHeader) {
        String userId = getUserIdFromHeader(authHeader);
        if (userId == null) return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());

        return zoneService.updateZone(id, zone, userId)
                .map(updated -> ResponseEntity.ok(BaseResponse.success("Updated", updated)))
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<BaseResponse<Void>>> delete(@PathVariable String id, @RequestHeader("Authorization") String authHeader) {
        String userId = getUserIdFromHeader(authHeader);
        if (userId == null) return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());

        return zoneService.deleteZone(id, userId)
                .then(Mono.just(ResponseEntity.ok(BaseResponse.<Void>success("Deleted", null))));
    }
}
