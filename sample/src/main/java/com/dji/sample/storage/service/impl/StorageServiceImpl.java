package com.dji.sample.storage.service.impl;

import com.dji.sample.component.oss.model.OssConfiguration;
import com.dji.sample.component.oss.service.impl.OssServiceContext;
import com.dji.sample.storage.service.IStorageService;
import com.dji.sdk.cloudapi.media.StorageConfigGet;
import com.dji.sdk.cloudapi.media.api.AbstractMediaService;
import com.dji.sdk.cloudapi.storage.StsCredentialsResponse;
import com.dji.sdk.mqtt.MqttReply;
import com.dji.sdk.mqtt.requests.TopicRequestsRequest;
import com.dji.sdk.mqtt.requests.TopicRequestsResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Service;

/**
 * @author sean
 * @version 0.3
 * @date 2022/3/9
 */
@Service
@Slf4j
public class StorageServiceImpl extends AbstractMediaService implements IStorageService {

    @Autowired
    private OssServiceContext ossService;

    @Override
    public StsCredentialsResponse getSTSCredentials() {
        if (!OssConfiguration.enable) {
            throw new IllegalStateException("OSS is disabled (oss.enable=false); enable and configure OSS to obtain STS credentials.");
        }
        var credentials = ossService.getCredentials();
        if (credentials == null) {
            throw new IllegalStateException("OSS temporary credentials unavailable (provider returned null).");
        }
        return new StsCredentialsResponse()
                .setEndpoint(OssConfiguration.endpoint)
                .setBucket(OssConfiguration.bucket)
                .setCredentials(credentials)
                .setProvider(OssConfiguration.provider)
                .setObjectKeyPrefix(OssConfiguration.objectDirPrefix)
                .setRegion(OssConfiguration.region);
    }

    @Override
    public TopicRequestsResponse<MqttReply<StsCredentialsResponse>> storageConfigGet(TopicRequestsRequest<StorageConfigGet> response, MessageHeaders headers) {
        try {
            TopicRequestsResponse<MqttReply<StsCredentialsResponse>> out = new TopicRequestsResponse<MqttReply<StsCredentialsResponse>>()
                    .setData(MqttReply.success(getSTSCredentials()));
            log.info("[MQTT][link] storage_config_get success (reply issued)");
            return out;
        } catch (Exception e) {
            log.error("[MQTT][link] storage_config_get failed (reply=MqttReply.error; device may retry)", e);
            return new TopicRequestsResponse<MqttReply<StsCredentialsResponse>>()
                    .setData(MqttReply.error("storage_config_get failed: " + e.getMessage()));
        }
    }
}
