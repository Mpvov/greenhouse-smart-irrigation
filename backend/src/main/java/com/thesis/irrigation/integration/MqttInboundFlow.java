package com.thesis.irrigation.integration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.stereotype.Component;

/**
 * MqttInboundFlow — Processes MQTT messages arriving on mqttInboundChannel.
 *
 * Current state: SKELETON — logs the raw payload.
 *
 * Next iteration will implement the Lambda Architecture dual path:
 *
 *   HOT PATH  (Realtime): Parse payload → publish to Redis Pub/Sub channel
 *             → WebSocketHandler picks up → pushes to React UI via WSS
 *
 *   COLD PATH (Storage):  Parse payload → save DataRecord via ReactiveMongoRepository
 *             → available for history/chart queries
 *
 * The @ServiceActivator annotation wires this handler to the Spring Integration
 * channel named "mqttInboundChannel" (declared as a bean in MqttConfig).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MqttInboundFlow {

    private final com.thesis.irrigation.service.TelemetryService telemetryService;

    /**
     * mqttMessageHandler — activated whenever a message arrives on mqttInboundChannel.
     */
    @Bean
    @ServiceActivator(inputChannel = "mqttInboundChannel")
    public MessageHandler mqttMessageHandler() {
        return message -> {
            String topic = (String) message.getHeaders()
                    .getOrDefault("mqtt_receivedTopic", "unknown");
            Object payload = message.getPayload();

            log.info("[MqttInboundFlow] Received message. Topic: {}, Payload: {}", topic, payload);

            String payloadStr = payload instanceof byte[] ? new String((byte[]) payload) : payload.toString();

            telemetryService.processMessage(topic, payloadStr)
                    .subscribe(
                            null,
                            error -> log.error("[MqttInboundFlow] Error processing telemetry: {}", error.getMessage())
                    );
        };
    }
}
