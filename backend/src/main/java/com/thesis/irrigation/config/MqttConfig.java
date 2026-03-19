package com.thesis.irrigation.config;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

/**
 * MqttConfig — Spring Integration wiring for MQTT Inbound and Outbound flows.
 *
 * IMPORTANT: The inbound adapter starts with a dummy topic ($SYS/#).
 * Real topics ({ownerId}/{ghId}/#) are added dynamically at runtime
 * by MqttSubscriptionManager after DatabaseInitializer seeds the DB.
 */
@Slf4j
@Configuration
public class MqttConfig {

    @Value("${mqtt.broker.url}")
    private String brokerUrl;

    @Value("${mqtt.client.id:irrigation-backend}")
    private String clientId;

    @Value("${mqtt.topic.publish:command/#}")
    private String publishTopic;

    // ─── Shared Client Factory ────────────────────────────────────────────────

    @Bean
    public MqttPahoClientFactory mqttClientFactory() {
        log.info("[MqttConfig] Initializing MQTT client factory. Broker: {}", brokerUrl);

        MqttConnectOptions options = new MqttConnectOptions();
        options.setServerURIs(new String[]{brokerUrl});
        options.setCleanSession(true);
        options.setAutomaticReconnect(true);
        options.setConnectionTimeout(10);
        options.setKeepAliveInterval(30);

        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        factory.setConnectionOptions(options);
        return factory;
    }

    // ─── Inbound Channel (Sensor Data from Edge) ─────────────────────────────

    @Bean
    public MessageChannel mqttInboundChannel() {
        return new DirectChannel();
    }

    /**
     * Starts with dummy topic $SYS/#. Real topics added by MqttSubscriptionManager.
     */
    @Bean
    public MqttPahoMessageDrivenChannelAdapter mqttInboundAdapter(
            MqttPahoClientFactory mqttClientFactory,
            MessageChannel mqttInboundChannel) {

        String inboundClientId = clientId + "-inbound";

        // Start with $SYS/# (a non-data system topic) so adapter can initialize
        MqttPahoMessageDrivenChannelAdapter adapter =
                new MqttPahoMessageDrivenChannelAdapter(
                        inboundClientId,
                        mqttClientFactory,
                        "$SYS/#");

        adapter.setCompletionTimeout(5000);
        adapter.setConverter(new DefaultPahoMessageConverter());
        adapter.setQos(1);
        adapter.setOutputChannel(mqttInboundChannel);

        log.info("[MqttConfig] MQTT Inbound adapter initialized with dummy topic $SYS/#. ClientId: '{}'",
                inboundClientId);

        return adapter;
    }

    // ─── Outbound Channel (Commands to Edge) ─────────────────────────────────

    @Bean
    public MessageChannel mqttOutboundChannel() {
        return new DirectChannel();
    }

    @Bean
    @ServiceActivator(inputChannel = "mqttOutboundChannel")
    public MessageHandler mqttOutboundHandler(MqttPahoClientFactory mqttClientFactory) {
        String outboundClientId = clientId + "-outbound";

        MqttPahoMessageHandler handler = new MqttPahoMessageHandler(
                outboundClientId,
                mqttClientFactory);

        handler.setAsync(true);
        handler.setDefaultTopic(publishTopic);
        handler.setDefaultQos(1);

        log.info("[MqttConfig] MQTT Outbound handler configured. ClientId: '{}', Default topic: '{}'",
                outboundClientId, publishTopic);

        return handler;
    }
}
