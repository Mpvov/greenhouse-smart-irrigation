package com.thesis.irrigation.domain.model;

import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Represents a greenhouse owner or system administrator.
 * Maps to the 'users' MongoDB collection.
 */
@Document(collection = "users")
@Builder
public record User(
        @Id
        String id,

        @Indexed(unique = true)
        String email,

        String passwordHash,

        String role  // ADMIN | OWNER
) {}
