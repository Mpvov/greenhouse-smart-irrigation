package com.thesis.irrigation.controller;

import com.thesis.irrigation.common.BaseResponse;
import com.thesis.irrigation.domain.dto.HistoryResponse;
import com.thesis.irrigation.service.HistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/monitoring")
@RequiredArgsConstructor
public class HistoryController {

    private final HistoryService historyService;

    @GetMapping("/history/{rowId}")
    public Mono<ResponseEntity<BaseResponse<HistoryResponse>>> getHistory(
            @PathVariable String rowId,
            @RequestParam(required = false, defaultValue = "24") Integer hours) {

        return historyService.getRowHistory(rowId, hours)
                .map(response -> ResponseEntity.ok(BaseResponse.success("Success", response)));
    }
}
