package com.thesis.irrigation.integration;

import com.thesis.irrigation.domain.repository.GreenhouseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.annotation.Order;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.stereotype.Component;

/**
 * MqttSubscriptionManager — Reads all Greenhouses from MongoDB,
 * forms dynamic MQTT topics ({ownerId}/{ghId}/#), and subscribes
 * the adapter at runtime.
 *
 * Must run AFTER DatabaseInitializer (@Order(2) vs @Order(1)).
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(2)
public class MqttSubscriptionManager implements ApplicationListener<ApplicationReadyEvent> {

    private final MqttPahoMessageDrivenChannelAdapter mqttInboundAdapter;
    private final GreenhouseRepository greenhouseRepository;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("[MqttSubscriptionManager] Querying DB for Greenhouses to build dynamic MQTT topics...");

        greenhouseRepository.findAll()
                .doOnNext(gh -> {
                    String topic = gh.ownerId() + "/" + gh.id() + "/#";
                    try {
                        mqttInboundAdapter.addTopic(topic, 1);
                        log.info("[MqttSubscriptionManager] ✅ Subscribed to topic: {}", topic);
                    } catch (Exception e) {
                        log.error("[MqttSubscriptionManager] ❌ Failed to subscribe to {}: {}", topic, e.getMessage());
                    }
                })
                .doOnComplete(() -> log.info("[MqttSubscriptionManager] ✅ Dynamic MQTT subscription complete."))
                .doOnError(err -> log.error("[MqttSubscriptionManager] Error querying greenhouses: {}", err.getMessage()))
                .blockLast();
    }
}
