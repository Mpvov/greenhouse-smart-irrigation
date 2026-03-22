package com.thesis.irrigation.domain.exception;

import com.thesis.irrigation.common.BaseResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.resource.NoResourceFoundException;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

/**
 * Global exception handler for all REST controllers.
 * Catches common exceptions and returns a standardized BaseResponse.
 *
 * NOTE: Must return Mono<ResponseEntity<...>> to stay non-blocking in the reactive pipeline.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles Spring's ResponseStatusException (e.g., 404 Not Found, 400 Bad Request).
     */
    @ExceptionHandler(ResponseStatusException.class)
    public Mono<ResponseEntity<BaseResponse<?>>> handleResponseStatusException(
            ResponseStatusException ex) {

        int statusCode = ex.getStatusCode().value();
        BaseResponse<?> body = BaseResponse.error(statusCode, ex.getReason() != null
                ? ex.getReason()
                : ex.getMessage());

        return Mono.just(ResponseEntity.status(statusCode).body(body));
    }

    /**
     * Handles 404 when a static resource or route is not found.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public Mono<ResponseEntity<BaseResponse<?>>> handleNoResourceFound(
            NoResourceFoundException ex) {

        BaseResponse<?> body = BaseResponse.error(
                HttpStatus.NOT_FOUND.value(),
                "Resource not found: " + (ex.getReason() != null ? ex.getReason() : ex.getMessage()));

        return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body(body));
    }

    /**
     * Catch-all handler for any unhandled exceptions.
     * Avoids leaking internal stack traces to the client.
     */
    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<BaseResponse<?>>> handleGenericException(Exception ex) {

        BaseResponse<?> body = BaseResponse.error(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "An unexpected internal server error occurred.");

        return Mono.just(ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(body));
    }
}
