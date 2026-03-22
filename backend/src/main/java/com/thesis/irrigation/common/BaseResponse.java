package com.thesis.irrigation.common;

import java.time.Instant;

/**
 * Standard API response wrapper for all REST endpoints.
 * Using Java Record for immutability and conciseness.
 *
 * @param <T> Type of the response data payload
 */
public record BaseResponse<T>(
        Instant timestamp,
        int status,
        String message,
        T data
) {

    /**
     * Factory method for a successful response with data.
     */
    public static <T> BaseResponse<T> success(String message, T data) {
        return new BaseResponse<>(Instant.now(), 200, message, data);
    }

    /**
     * Factory method for a successful response without data (e.g., 204 No Content).
     */
    public static <T> BaseResponse<T> success(String message) {
        return new BaseResponse<>(Instant.now(), 200, message, null);
    }

    /**
     * Factory method for an error response.
     */
    public static <T> BaseResponse<T> error(int status, String message) {
        return new BaseResponse<>(Instant.now(), status, message, null);
    }
}
