package com.thesis.irrigation.controller;

import com.thesis.irrigation.common.BaseResponse;
import com.thesis.irrigation.domain.model.Greenhouse;
import com.thesis.irrigation.domain.model.Row;
import com.thesis.irrigation.domain.model.Zone;
import com.thesis.irrigation.domain.repository.GreenhouseRepository;
import com.thesis.irrigation.domain.repository.RowRepository;
import com.thesis.irrigation.domain.repository.ZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.thesis.irrigation.utils.JwtUtil;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/monitoring")
@RequiredArgsConstructor
public class MonitoringController {

    private final GreenhouseRepository greenhouseRepository;
    private final ZoneRepository zoneRepository;
    private final RowRepository rowRepository;
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

    /**
     * Returns the full hierarchy of greenhouses, zones, and rows for the authenticated user.
     */
    @GetMapping("/tree")
    public Mono<org.springframework.http.ResponseEntity<BaseResponse<List<GreenhouseTree>>>> getMonitoringTree(@org.springframework.web.bind.annotation.RequestHeader("Authorization") String authHeader) {
        String userId = getUserIdFromHeader(authHeader);
        if (userId == null) return Mono.just(org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).build());

        return greenhouseRepository.findByOwnerId(userId)
                .flatMap(gh -> zoneRepository.findByGreenhouseId(gh.id())
                        .flatMap(zone -> rowRepository.findByZoneId(zone.id())
                                .collectList()
                                .map(rows -> new ZoneTree(zone, rows)))
                        .collectList()
                        .map(zones -> new GreenhouseTree(gh, zones)))
                .collectList()
                .map(list -> org.springframework.http.ResponseEntity.ok(BaseResponse.success("Fetched monitoring tree", list)));
    }

    // DTOs for the tree structure
    public record GreenhouseTree(Greenhouse info, List<ZoneTree> zones) {}
    public record ZoneTree(Zone info, List<Row> rows) {}
}
