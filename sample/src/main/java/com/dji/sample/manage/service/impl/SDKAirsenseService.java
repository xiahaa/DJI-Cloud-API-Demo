package com.dji.sample.manage.service.impl;

import com.dji.sdk.cloudapi.airsense.AirsenseWarning;
import com.dji.sdk.cloudapi.airsense.api.AbstractAirsenseService;
import com.dji.sdk.mqtt.MqttReply;
import com.dji.sdk.mqtt.events.TopicEventsRequest;
import com.dji.sdk.mqtt.events.TopicEventsResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Handles AirSense ADS-B warning events from device MQTT {@code /events}.
 */
@Service
@Slf4j
public class SDKAirsenseService extends AbstractAirsenseService {

    @Override
    public TopicEventsResponse<MqttReply> airsenseWarning(TopicEventsRequest<List<AirsenseWarning>> request, MessageHeaders headers) {
        List<AirsenseWarning> data = request.getData();
        if (log.isDebugEnabled()) {
            log.debug("airsense_warning gateway={} count={}", request.getGateway(), data == null ? 0 : data.size());
        }
        return new TopicEventsResponse<MqttReply>().setData(MqttReply.success());
    }
}
