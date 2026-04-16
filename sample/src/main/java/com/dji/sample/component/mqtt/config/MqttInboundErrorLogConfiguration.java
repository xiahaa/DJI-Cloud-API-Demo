package com.dji.sample.component.mqtt.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Logs MQTT inbound routing/deserialization exceptions from Spring Integration errorChannel.
 */
@Slf4j
@Configuration
public class MqttInboundErrorLogConfiguration {

    private static final int MAX_PAYLOAD_LOG_BYTES = 1024;

    @Bean
    public IntegrationFlow mqttInboundErrorLogger() {
        return IntegrationFlows.from("errorChannel")
                .handle(message -> {
                    if (!(message.getPayload() instanceof Throwable)) {
                        return;
                    }

                    Throwable throwable = message.getPayload() instanceof Throwable
                            ? (Throwable) message.getPayload()
                            : null;
                    MessagingException messagingException = throwable instanceof MessagingException
                            ? (MessagingException) throwable
                            : null;
                    if (messagingException == null) {
                        return;
                    }

                    Message<?> failed = messagingException.getFailedMessage();
                    if (failed == null) {
                        log.error("[MQTT][inbound-error] no failed message on MessagingException.", messagingException);
                        return;
                    }

                    Object topicHeader = failed.getHeaders().get(MqttHeaders.RECEIVED_TOPIC);
                    if (topicHeader == null) {
                        return;
                    }

                    String topic = String.valueOf(topicHeader);
                    String payloadPreview = previewPayload(failed.getPayload());
                    Throwable root = rootCause(messagingException);
                    log.error("[MQTT][inbound-error] handling failed. topic={}, errorType={}, error={}, payloadPreview={}",
                            topic, root.getClass().getName(), root.getMessage(), payloadPreview, messagingException);
                })
                .get();
    }

    private String previewPayload(Object payload) {
        if (payload == null) {
            return "null";
        }
        if (payload instanceof byte[]) {
            byte[] bytes = (byte[]) payload;
            int len = Math.min(bytes.length, MAX_PAYLOAD_LOG_BYTES);
            String preview = new String(bytes, 0, len, StandardCharsets.UTF_8);
            return bytes.length > MAX_PAYLOAD_LOG_BYTES ? preview + "...(truncated)" : preview;
        }
        String value = String.valueOf(payload);
        return value.length() > MAX_PAYLOAD_LOG_BYTES ? value.substring(0, MAX_PAYLOAD_LOG_BYTES) + "...(truncated)" : value;
    }

    private Throwable rootCause(Throwable t) {
        Throwable cursor = t;
        while (Objects.nonNull(cursor.getCause()) && cursor.getCause() != cursor) {
            cursor = cursor.getCause();
        }
        return cursor;
    }
}
