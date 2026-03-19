package com.thesis.irrigation.domain.model;

import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Level 1: Greenhouse — belongs to a User (via ownerId).
 */
@Document(collection = "greenhouses")
@Builder
public record Greenhouse(
        @Id
        String id,
        String ownerId,   // FK → User.id (used to build MQTT topic: {ownerId}/{id}/#)
        String name,
        String location
) {}
