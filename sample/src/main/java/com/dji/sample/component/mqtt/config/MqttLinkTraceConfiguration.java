package com.dji.sample.component.mqtt.config;

import com.dji.sample.component.mqtt.model.MqttClientOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.integration.mqtt.event.MqttConnectionFailedEvent;
import org.springframework.integration.mqtt.event.MqttSubscribedEvent;

/**
 * 链路日志：Java 侧 MQTT 与 Pilot 侧原生 MQTT 无关，但可对照「服务端是否仍能连上同一 Broker」。
 */
@Slf4j
@Configuration
public class MqttLinkTraceConfiguration {

    @Bean
    public ApplicationRunner mqttLinkStartupSummary(Environment environment) {
        return (ApplicationArguments args) -> {
            String inbound = environment.getProperty("cloud-sdk.mqtt.inbound-topic", "(unset)");
            MqttClientOptions basic = MqttPropertyConfiguration.getBasicClientOptions();
            String broker = MqttPropertyConfiguration.getBasicMqttAddress();
            log.info("[MQTT][link][startup] Java Paho brokerUri={} username={} passwordConfigured={} inboundTopics={}",
                    broker,
                    basic.getUsername(),
                    basic.getPassword() != null && !basic.getPassword().isEmpty(),
                    inbound);
        };
    }

    @EventListener
    public void onMqttConnectionFailed(MqttConnectionFailedEvent event) {
        Throwable cause = event.getCause();
        log.error("[MQTT][link][event] connectionFailed source={} message={}",
                event.getSource() != null ? event.getSource().getClass().getSimpleName() : "null",
                cause != null ? cause.getMessage() : event.toString(),
                cause);
    }

    @EventListener
    public void onMqttSubscribed(MqttSubscribedEvent event) {
        log.info("[MQTT][link][event] subscribed {}", event.getMessage());
    }
}
