package com.thesis.irrigation.controller;

import com.thesis.irrigation.common.BaseResponse;
import com.thesis.irrigation.domain.model.Greenhouse;
import com.thesis.irrigation.service.GreenhouseService;
import com.thesis.irrigation.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/v1/greenhouses")
@RequiredArgsConstructor
@Slf4j
public class GreenhouseController {

    private final GreenhouseService greenhouseService;
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
    public Mono<ResponseEntity<BaseResponse<List<Greenhouse>>>> getAll(@RequestHeader("Authorization") String authHeader) {
        String userId = getUserIdFromHeader(authHeader);
        if (userId == null) return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());

        return greenhouseService.getAllByOwner(userId)
                .collectList()
                .map(list -> ResponseEntity.ok(BaseResponse.success("Success", list)));
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<BaseResponse<Greenhouse>>> getById(@PathVariable String id, @RequestHeader("Authorization") String authHeader) {
        String userId = getUserIdFromHeader(authHeader);
        if (userId == null) return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());

        return greenhouseService.getById(id, userId)
                .map(gh -> ResponseEntity.ok(BaseResponse.success("Success", gh)))
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PostMapping
    public Mono<ResponseEntity<BaseResponse<Greenhouse>>> create(@RequestBody Greenhouse greenhouse, @RequestHeader("Authorization") String authHeader) {
        String userId = getUserIdFromHeader(authHeader);
        if (userId == null) return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());

        Greenhouse toCreate = Greenhouse.builder()
                .id(greenhouse.name())
                .ownerId(userId)
                .name(greenhouse.name())
                .location(greenhouse.location())
                .build();

        return greenhouseService.createGreenhouse(toCreate)
                .map(saved -> ResponseEntity.status(HttpStatus.CREATED).body(BaseResponse.success("Created", saved)));
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<BaseResponse<Greenhouse>>> update(@PathVariable String id, @RequestBody Greenhouse greenhouse, @RequestHeader("Authorization") String authHeader) {
        String userId = getUserIdFromHeader(authHeader);
        if (userId == null) return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());

        return greenhouseService.updateGreenhouse(id, greenhouse, userId)
                .map(updated -> ResponseEntity.ok(BaseResponse.success("Updated", updated)))
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<BaseResponse<Void>>> delete(@PathVariable String id, @RequestHeader("Authorization") String authHeader) {
        String userId = getUserIdFromHeader(authHeader);
        if (userId == null) return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());

        return greenhouseService.deleteGreenhouse(id, userId)
                .then(Mono.just(ResponseEntity.ok(BaseResponse.<Void>success("Deleted", null))))
                .onErrorResume(e -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(BaseResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage()))));
    }
}
