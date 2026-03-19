package com.thesis.irrigation.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * RedisConfig — Provides the primary ReactiveRedisTemplate bean.
 *
 * Uses StringRedisSerializer for both keys and values.
 * All WebSocket handlers will use this template to subscribe to Redis Pub/Sub
 * channels and receive telemetry in real-time without blocking the Netty event loop.
 *
 * Architecture:
 *   MqttInboundFlow  →  publish(channel, json)  →  Redis Pub/Sub
 *   WebSocketHandler ←  subscribe(channel)       ← Redis Pub/Sub
 */
@Slf4j
@Configuration
public class RedisConfig {

    /**
     * ReactiveRedisTemplate<String, String>
     *
     * Key   = Redis channel name (e.g., "telemetry:device-001")
     * Value = JSON string payload (serialized TelemetryDTO)
     *
     * CRITICAL — Non-blocking: This template's publish/subscribe operations
     * return Mono/Flux and NEVER block the calling thread.
     */
    @Primary
    @Bean
    public ReactiveRedisTemplate<String, String> reactiveRedisTemplate(
            ReactiveRedisConnectionFactory connectionFactory) {

        StringRedisSerializer serializer = new StringRedisSerializer();

        RedisSerializationContext<String, String> context =
                RedisSerializationContext.<String, String>newSerializationContext(serializer)
                        .key(serializer)
                        .value(serializer)
                        .hashKey(serializer)
                        .hashValue(serializer)
                        .build();

        log.info("[RedisConfig] ReactiveRedisTemplate<String, String> bean created. " +
                 "Serializer: StringRedisSerializer (key + value).");

        return new ReactiveRedisTemplate<>(connectionFactory, context);
    }
}
