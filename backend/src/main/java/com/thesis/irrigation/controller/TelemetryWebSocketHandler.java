package com.thesis.irrigation.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thesis.irrigation.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

import java.net.URI;

/**
 * TelemetryWebSocketHandler — Streams real-time telemetry to authenticated clients.
 *
 * Data Isolation: Only forwards Redis messages where message.userId == session.userId.
 *
 * Connection: ws://host/ws/telemetry?token=JWT_TOKEN
 *          or ws://host/ws/telemetry?userId=1  (dev mode)
 */
@Slf4j
@Component("telemetryWebSocketHandler")
@RequiredArgsConstructor
public class TelemetryWebSocketHandler implements WebSocketHandler {

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        String sessionId = session.getId();
        URI uri = session.getHandshakeInfo().getUri();
        String query = uri.getQuery();

        // ── Extract userId from JWT or ?userId= param (dev fallback) ──
        String userId = extractUserId(query);

        if (userId == null) {
            log.warn("[WebSocket] No valid userId found. Closing session: {}", sessionId);
            return session.close();
        }

        log.info("[WebSocket] Client connected. userId={}, sessionId={}", userId, sessionId);

        // ── Receive: keep-alive / ping frames ──
        Mono<Void> receiveMono = session.receive()
                .doOnNext(msg -> log.debug("[WebSocket] Received: {}", msg.getPayloadAsText()))
                .then();

        // ── Send: Subscribe to Redis, FILTER by userId ──
        Mono<Void> sendMono = session.send(
                redisTemplate.listenToChannel("iot.telemetry.realtime")
                        .filter(message -> {
                            try {
                                JsonNode node = objectMapper.readTree(message.getMessage());
                                String msgUserId = node.path("userId").asText("");
                                return userId.equals(msgUserId);
                            } catch (Exception e) {
                                return false; // Drop unparseable messages
                            }
                        })
                        .map(message -> {
                            // Forward only the "data" portion (strip the userId wrapper)
                            try {
                                JsonNode node = objectMapper.readTree(message.getMessage());
                                JsonNode data = node.path("data");
                                return session.textMessage(data.toString());
                            } catch (Exception e) {
                                return session.textMessage(message.getMessage());
                            }
                        })
                        .doOnNext(msg -> log.debug("[WebSocket] Sending to userId={}, session={}", userId, sessionId))
        );

        return Mono.zip(receiveMono, sendMono)
                .doFinally(signal -> log.info("[WebSocket] Client disconnected. userId={}, signal={}", userId, signal))
                .then();
    }

    /**
     * Extracts userId from query params.
     * Priority: 1) JWT token → extract userId  2) ?userId= param (dev mode)
     */
    private String extractUserId(String query) {
        if (query == null) return null;

        String token = null;
        String userIdParam = null;

        for (String param : query.split("&")) {
            if (param.startsWith("token=")) {
                token = param.substring(6);
            }
            if (param.startsWith("userId=")) {
                userIdParam = param.substring(7);
            }
        }

        // Try JWT first
        if (token != null && jwtUtil.validateToken(token)) {
            String claimId = jwtUtil.getAllClaimsFromToken(token).get("userId", String.class);
            if (claimId != null) return claimId;
            
            // Fallback for old tokens
            String email = jwtUtil.getEmailFromToken(token);
            if ("admin@bk.hcm".equals(email)) return "1";
            return email;
        }

        // Dev fallback: ?userId=1
        return userIdParam;
    }
}
