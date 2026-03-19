package com.thesis.irrigation.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;

import java.util.Map;

/**
 * WebSocketConfig — Wires the Spring WebFlux WebSocket infrastructure.
 *
 * Architecture:
 *   React Client  ──WSS──►  /ws/telemetry  ──►  TelemetryWebSocketHandler
 *                                                 (subscribes to Redis Pub/Sub)
 *                                                 (streams JSON to client via session.send())
 *
 * NOTE: TelemetryWebSocketHandler is a placeholder bean registered here.
 *       The actual handler logic (Redis subscribe + session.send) will be
 *       implemented in controller/TelemetryWebSocketHandler.java.
 *
 * Uses Spring WebFlux's reactive WebSocket API (NOT the Servlet/Tomcat WebSocket API).
 * Package: org.springframework.web.reactive.socket.*
 */
@Slf4j
@Configuration
public class WebSocketConfig {

    /**
     * WebSocketHandlerAdapter — bridges Spring's DispatcherHandler to WebSocket handlers.
     * Required bean for WebFlux WebSocket to work.
     */
    @Bean
    public WebSocketHandlerAdapter webSocketHandlerAdapter() {
        log.info("[WebSocketConfig] WebSocketHandlerAdapter registered.");
        return new WebSocketHandlerAdapter();
    }

    /**
     * SimpleUrlHandlerMapping — maps URL paths to specific WebSocketHandler beans.
     *
     * Registered endpoints:
     *   /ws/telemetry  →  telemetryWebSocketHandler  (realtime sensor stream)
     *
     * Order = 1: Lower value = higher priority. Ensures WS mapping is checked before
     * generic request mappings.
     */
    @Bean
    public HandlerMapping webSocketHandlerMapping(WebSocketHandler telemetryWebSocketHandler) {
        Map<String, WebSocketHandler> urlMap = Map.of(
                "/ws/telemetry", telemetryWebSocketHandler
        );

        SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
        mapping.setUrlMap(urlMap);
        mapping.setOrder(1);

        log.info("[WebSocketConfig] WebSocket endpoint mapped: /ws/telemetry → TelemetryWebSocketHandler");

        return mapping;
    }
}
