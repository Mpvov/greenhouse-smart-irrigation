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

    /**
     * Returns the full hierarchy of greenhouses, zones, and rows.
     * In a production system, this might be filtered by user, but for now we return all.
     */
    @GetMapping("/tree")
    public Mono<BaseResponse<List<GreenhouseTree>>> getMonitoringTree() {
        return greenhouseRepository.findAll()
                .flatMap(gh -> zoneRepository.findByGreenhouseId(gh.id())
                        .flatMap(zone -> rowRepository.findByZoneId(zone.id())
                                .collectList()
                                .map(rows -> new ZoneTree(zone, rows)))
                        .collectList()
                        .map(zones -> new GreenhouseTree(gh, zones)))
                .collectList()
                .map(list -> BaseResponse.success("Fetched monitoring tree", list));
    }

    // DTOs for the tree structure
    public record GreenhouseTree(Greenhouse info, List<ZoneTree> zones) {}
    public record ZoneTree(Zone info, List<Row> rows) {}
}
