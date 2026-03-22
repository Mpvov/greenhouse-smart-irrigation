package com.thesis.irrigation.domain.model;

import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

/**
 * Irrigation Schedule for a specific Row.
 */
@Document(collection = "schedules")
@Builder
public record Schedule(
        @Id
        String id,
        String rowId,
        String startTime,   // e.g. "06:00"
        Integer duration,   // minutes
        Boolean isActive,
        Instant createdAt
) {}
