package com.dji.sample.control.service.impl;

import com.dji.sample.component.mqtt.config.MqttPropertyConfiguration;
import com.dji.sample.component.mqtt.model.MapKeyConst;
import com.dji.sample.component.redis.RedisConst;
import com.dji.sample.component.redis.RedisOpsUtils;
import com.dji.sample.control.model.dto.JwtAclDTO;
import com.dji.sample.control.model.enums.MqttAclAccessEnum;
import com.dji.sample.control.model.param.RcPlus2DrcConnectParam;
import com.dji.sample.control.model.param.RcPlus2DrcEnterParam;
import com.dji.sample.control.model.enums.DroneAuthorityEnum;
import com.dji.sample.control.model.param.RcPlus2FlyToPointActionsParam;
import com.dji.sample.control.model.param.RcPlus2FlyToPointParam;
import com.dji.sample.control.model.param.RcPlus2OsdLatestParam;
import com.dji.sample.control.model.param.RcPlus2YawControlParam;
import com.dji.sample.control.service.IControlService;
import com.dji.sample.control.service.IRcPlus2ControlService;
import com.dji.sample.control.service.utils.RcPlus2FlyToPointActionUtils;
import com.dji.sdk.cloudapi.device.OsdRcDrone;
import com.dji.sample.manage.model.dto.DeviceDTO;
import com.dji.sample.manage.service.IDeviceRedisService;
import com.dji.sdk.cloudapi.control.ControlMethodEnum;
import com.dji.sdk.cloudapi.control.CloudControlAuthRequest;
import com.dji.sdk.cloudapi.control.DrcModeEnterRequest;
import com.dji.sdk.cloudapi.control.DrcModeMqttBroker;
import com.dji.sdk.cloudapi.control.FlyToPointRequest;
import com.dji.sdk.cloudapi.control.OsdInfoPush;
import com.dji.sdk.cloudapi.control.Point;
import com.dji.sdk.cloudapi.control.StickControlRequest;
import com.dji.sdk.cloudapi.control.api.AbstractControlService;
import com.dji.sdk.cloudapi.device.DeviceDomainEnum;
import com.dji.sdk.common.HttpResultResponse;
import com.dji.sdk.common.SDKManager;
import com.dji.sdk.mqtt.TopicConst;
import com.dji.sdk.mqtt.drc.DrcDownPublish;
import com.dji.sdk.mqtt.drc.TopicDrcRequest;
import com.dji.sdk.mqtt.services.ServicesReplyData;
import com.dji.sdk.mqtt.services.TopicServicesResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeoutException;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * RC Plus 2 realtime flight control implementation.
 *
 * <p>The implementation keeps the same API->MQTT pattern used by sample dock flows,
 * but targets Pilot/RC gateways for DRC and realtime control commands.</p>
 *
 * <p>Official Pilot-to-Cloud topics use {@code thing/product/{gateway_sn}/...}; here
 * {@code gateway_sn} is the RC gateway serial — the same value as HTTP body {@code rc_sn}.</p>
 * @see <a href="https://developer.dji.com/doc/cloud-api-tutorial/cn/api-reference/pilot-to-cloud/mqtt/dji-rc-plus-2/drc.html">DJI 上云 API - RC Plus 2 - DRC</a>
 * @see <a href="https://developer.dji.com/doc/cloud-api-tutorial/cn/api-reference/pilot-to-cloud/mqtt/dji-rc-plus-2/remote-control.html">DJI 上云 API - RC Plus 2 - remote control</a>
 * @author Saly C
 */
@Service
@Slf4j
public class RcPlus2ControlServiceImpl implements IRcPlus2ControlService {

    /**
     * RC fly_to_point may need user confirmation on Pilot; default SDK wait (3s per attempt) is often too short.
     */
    private static final long RC_FLY_TO_POINT_REPLY_TIMEOUT_MS = 15_000L;
    private static final long RC_OSD_WAIT_TIMEOUT_MS = 10_000L;
    private static final int DRC_LINK_REFUSED_CODE = 514304;
    private static final float DEFAULT_ACTIONS_TARGET_HEIGHT = 145F;

    private final ObjectMapper objectMapper;
    private final AbstractControlService abstractControlService;
    private final DrcDownPublish drcDownPublish;
    private final IDeviceRedisService deviceRedisService;
    private final IControlService controlService;
    private final RcPlus2DrcOsdWaitService rcPlus2DrcOsdWaitService;

    public RcPlus2ControlServiceImpl(ObjectMapper objectMapper,
                                     AbstractControlService abstractControlService,
                                     DrcDownPublish drcDownPublish,
                                     IDeviceRedisService deviceRedisService,
                                     IControlService controlService,
                                     RcPlus2DrcOsdWaitService rcPlus2DrcOsdWaitService) {
        this.objectMapper = objectMapper;
        this.abstractControlService = abstractControlService;
        this.drcDownPublish = drcDownPublish;
        this.deviceRedisService = deviceRedisService;
        this.controlService = controlService;
        this.rcPlus2DrcOsdWaitService = rcPlus2DrcOsdWaitService;
    }

    /**
     * Build DRC MQTT auth options for RC Plus 2 control terminal.
     */
    @Override
    public DrcModeMqttBroker drcConnect(String workspaceId, String userId, String username, RcPlus2DrcConnectParam param) {
        String clientId = param.getClientId();
        if (!StringUtils.hasText(clientId) || !RedisOpsUtils.checkExist(RedisConst.MQTT_ACL_PREFIX + clientId)) {
            clientId = userId + "-" + System.currentTimeMillis();
            RedisOpsUtils.hashSet(RedisConst.MQTT_ACL_PREFIX + clientId, "", MqttAclAccessEnum.ALL.getValue());
        }

        String key = RedisConst.MQTT_ACL_PREFIX + clientId;
        try {
            RedisOpsUtils.expireKey(key, RedisConst.DRC_MODE_ALIVE_SECOND);
            return MqttPropertyConfiguration.getMqttBrokerWithDrc(clientId, username, param.getExpireSec(), Map.of());
        } catch (RuntimeException e) {
            RedisOpsUtils.del(key);
            throw e;
        }
    }

    /**
     * Enter DRC mode for RC Plus 2 and grant per-device topic ACL.
     */
    @Override
    public JwtAclDTO drcEnter(String workspaceId, RcPlus2DrcEnterParam param) {
        log.info("[RC-DRC][enter] start workspaceId={}, rcSn={}, clientId={}, expireSec={}",
                workspaceId, param.getRcSn(), param.getClientId(), param.getExpireSec());
        DeviceDTO rc = requireOnlineRc(param.getRcSn());
        String gatewaySn = gatewaySnOrThrow(rc, param.getRcSn());
        String topic = TopicConst.THING_MODEL_PRE + TopicConst.PRODUCT + gatewaySn + TopicConst.DRC;
        String pubTopic = topic + TopicConst.DOWN;
        String subTopic = topic + TopicConst.UP;
        String servicesTopic = TopicConst.THING_MODEL_PRE + TopicConst.PRODUCT + gatewaySn + TopicConst.SERVICES_SUF;
        String servicesReplyTopic = servicesTopic + TopicConst._REPLY_SUF;
        log.info("[RC-DRC][enter] gateway_sn={}, servicesTopic={}, servicesReplyTopic={}, drcDownTopic={}, drcUpTopic={}",
                gatewaySn, servicesTopic, servicesReplyTopic, pubTopic, subTopic);

        if (param.getClientId().equals(getDrcClient(gatewaySn))) {
            log.info("[RC-DRC][enter] existing session owner detected, refresh ACL directly. gateway_sn={}, clientId={}",
                    gatewaySn, param.getClientId());
            refreshAcl(gatewaySn, param.getClientId(), pubTopic, subTopic);
            return JwtAclDTO.builder().sub(List.of(subTopic)).pub(List.of(pubTopic)).build();
        }

        requestCloudControlAuth(gatewaySn, servicesTopic, servicesReplyTopic, workspaceId, param.getClientId());

        DrcModeMqttBroker mqttBroker = MqttPropertyConfiguration.getMqttBrokerWithDrc(
                gatewaySn + "-" + System.currentTimeMillis(),
                gatewaySn,
                param.getExpireSec(),
                Map.of(MapKeyConst.ACL, objectMapper.convertValue(
                        JwtAclDTO.builder().pub(List.of(subTopic)).sub(List.of(pubTopic)).build(),
                        new TypeReference<Map<String, ?>>() {})));
        log.info("[RC-DRC][enter] publish method={} topic={} broker={{address={}, clientId={}, username={}, expireTime={}, enableTls={}}}, hsiFrequency={}, osdFrequency={}",
                ControlMethodEnum.DRC_MODE_ENTER.getMethod(),
                servicesTopic,
                mqttBroker.getAddress(),
                mqttBroker.getClientId(),
                mqttBroker.getUsername(),
                mqttBroker.getExpireTime(),
                mqttBroker.getEnableTls(),
                1,
                10);

        TopicServicesResponse<ServicesReplyData> reply = abstractControlService.drcModeEnter(
                SDKManager.getDeviceSDK(gatewaySn),
                new DrcModeEnterRequest()
                        .setMqttBroker(mqttBroker)
                        .setHsiFrequency(1)
                        .setOsdFrequency(10));

        ServicesReplyData replyData = reply.getData();
        log.info("[RC-DRC][enter] receive reply topic={} method={} gateway_sn={} result={}",
                servicesReplyTopic,
                ControlMethodEnum.DRC_MODE_ENTER.getMethod(),
                gatewaySn,
                replyData == null ? "empty response" : replyData.getResult());
        if (Objects.isNull(replyData) || !replyData.getResult().isSuccess()) {
            throw new RuntimeException("Failed to enter RC DRC mode. " + buildDrcEnterFailureMessage(gatewaySn, replyData));
        }

        refreshAcl(gatewaySn, param.getClientId(), pubTopic, subTopic);
        log.info("[RC-DRC][enter] success gateway_sn={}, ownerClientId={}, drcDownTopic={}, drcUpTopic={}",
                gatewaySn, param.getClientId(), pubTopic, subTopic);
        return JwtAclDTO.builder().sub(List.of(subTopic)).pub(List.of(pubTopic)).build();
    }

    /**
     * Exit DRC mode and release redis session for RC Plus 2.
     */
    @Override
    public void drcExit(String workspaceId, RcPlus2DrcEnterParam param) {
        log.info("[RC-DRC][exit] start workspaceId={}, rcSn={}, clientId={}",
                workspaceId, param.getRcSn(), param.getClientId());
        DeviceDTO rc = requireOnlineRc(param.getRcSn());
        String gatewaySn = gatewaySnOrThrow(rc, param.getRcSn());
        String servicesTopic = TopicConst.THING_MODEL_PRE + TopicConst.PRODUCT + gatewaySn + TopicConst.SERVICES_SUF;
        String servicesReplyTopic = servicesTopic + TopicConst._REPLY_SUF;
        log.info("[RC-DRC][exit] publish method={} topic={} gateway_sn={}",
                ControlMethodEnum.DRC_MODE_EXIT.getMethod(), servicesTopic, gatewaySn);
        TopicServicesResponse<ServicesReplyData> reply = abstractControlService.drcModeExit(SDKManager.getDeviceSDK(gatewaySn));
        ServicesReplyData replyData = reply.getData();
        log.info("[RC-DRC][exit] receive reply topic={} method={} gateway_sn={} result={}",
                servicesReplyTopic,
                ControlMethodEnum.DRC_MODE_EXIT.getMethod(),
                gatewaySn,
                replyData == null ? "empty response" : replyData.getResult());
        if (Objects.isNull(replyData) || !replyData.getResult().isSuccess()) {
            throw new RuntimeException("Failed to exit RC DRC mode. " + (replyData == null ? "empty response" : replyData.getResult()));
        }

        RedisOpsUtils.del(RedisConst.DRC_PREFIX + gatewaySn);
        RedisOpsUtils.del(RedisConst.MQTT_ACL_PREFIX + param.getClientId());
        log.info("[RC-DRC][exit] success gateway_sn={}, clearedOwnerKey={}, clearedAclKey={}",
                gatewaySn, RedisConst.DRC_PREFIX + gatewaySn, RedisConst.MQTT_ACL_PREFIX + param.getClientId());
    }

    /**
     * Fly to one or more target points after DRC owner validation and authority grab.
     */
    @Override
    public HttpResultResponse flyToPoint(String workspaceId, RcPlus2FlyToPointParam param) {
        log.info("[RC-CTRL][fly_to_point] start workspaceId={}, rcSn={}, clientId={}, maxSpeed={}, pointsCount={}",
                workspaceId,
                param.getRcSn(),
                param.getClientId(),
                param.getMaxSpeed(),
                param.getPoints() == null ? 0 : param.getPoints().size());
        DeviceDTO rc = validateDrcOwnership(param.getRcSn(), param.getClientId());
        String gatewaySn = gatewaySnOrThrow(rc, param.getRcSn());
        if (!StringUtils.hasText(rc.getChildDeviceSn())) {
            throw new RuntimeException(
                    "No aircraft linked to this RC. Connect the aircraft before fly_to_point (avoids services_reply timeout / 211001).");
        }
        ensureFlightAuthority(gatewaySn);

        FlyToPointRequest request = new FlyToPointRequest()
                .setFlyToId(UUID.randomUUID().toString())
                .setMaxSpeed(param.getMaxSpeed())
                .setPoints(param.getPoints());
        String servicesTopic = TopicConst.THING_MODEL_PRE + TopicConst.PRODUCT + gatewaySn + TopicConst.SERVICES_SUF;
        String servicesReplyTopic = servicesTopic + TopicConst._REPLY_SUF;
        log.info("[RC-CTRL][fly_to_point] publish method={} topic={} gateway_sn={}, flyToId={}, timeoutMs={}",
                ControlMethodEnum.FLY_TO_POINT.getMethod(),
                servicesTopic,
                gatewaySn,
                request.getFlyToId(),
                RC_FLY_TO_POINT_REPLY_TIMEOUT_MS);

        TopicServicesResponse<ServicesReplyData> response =
                abstractControlService.flyToPoint(
                        SDKManager.getDeviceSDK(gatewaySn), request, RC_FLY_TO_POINT_REPLY_TIMEOUT_MS);
        ServicesReplyData replyData = response.getData();
        log.info("[RC-CTRL][fly_to_point] receive reply topic={} method={} gateway_sn={} result={}",
                servicesReplyTopic,
                ControlMethodEnum.FLY_TO_POINT.getMethod(),
                gatewaySn,
                replyData == null ? "empty response" : replyData.getResult());
        if (Objects.isNull(replyData) || !replyData.getResult().isSuccess()) {
            return HttpResultResponse.error("RC fly_to_point failed. " + (replyData == null ? "empty response" : replyData.getResult()));
        }
        log.info("[RC-CTRL][fly_to_point] success gateway_sn={}, flyToId={}", gatewaySn, request.getFlyToId());
        return HttpResultResponse.success();
    }

    @Override
    public HttpResultResponse flyToPointByActions(String workspaceId, RcPlus2FlyToPointActionsParam param) {
        log.info("[RC-CTRL][fly_to_point_actions] start workspaceId={}, rcSn={}, deviceSn={}, clientId={}, maxSpeed={}, actionsCount={}, delayMs={}, waitTimeoutMs={}, targetHeight={}",
                workspaceId,
                param.getRcSn(),
                param.getDeviceSn(),
                param.getClientId(),
                param.getMaxSpeed(),
                param.getActions() == null ? 0 : param.getActions().size(),
                param.getDelayMs(),
                param.getWaitTimeoutMs(),
                Objects.requireNonNullElse(param.getHeight(), DEFAULT_ACTIONS_TARGET_HEIGHT));
        DeviceDTO rc = validateDrcOwnership(param.getRcSn(), param.getClientId());
        String gatewaySn = gatewaySnOrThrow(rc, param.getRcSn());
        verifyRequestedDeviceSn(rc, param.getDeviceSn());
        ensureFlightAuthority(gatewaySn);

        OsdRcDrone currentOsd = fetchCurrentOsdAfterDelay(param.getActions(), param.getDeviceSn(), param.getDelayMs(), param.getWaitTimeoutMs());
        float normalizedHead = normalizeAttitudeHeadTo360(
                Objects.requireNonNull(currentOsd.getAttitudeHead(), "Missing attitude_head from /osd data."));
        Point point = RcPlus2FlyToPointActionUtils.calculatePointByActions(
                param.getActions(),
                normalizedHead,
                Objects.requireNonNull(currentOsd.getLatitude(), "Missing latitude from /osd data."),
                Objects.requireNonNull(currentOsd.getLongitude(), "Missing longitude from /osd data."),
                Objects.requireNonNull(currentOsd.getHeight(), "Missing height from /osd data."));
        float targetHeight = Objects.requireNonNullElse(param.getHeight(), DEFAULT_ACTIONS_TARGET_HEIGHT);
        point.setHeight(targetHeight);
        log.info("[RC-CTRL][fly_to_point_actions] computed target gateway_sn={}, device_sn={}, point={}",
                gatewaySn, param.getDeviceSn(), toJsonForLog(point));

        RcPlus2FlyToPointParam flyToPointParam = new RcPlus2FlyToPointParam();
        flyToPointParam.setClientId(param.getClientId());
        flyToPointParam.setRcSn(param.getRcSn());
        flyToPointParam.setMaxSpeed(param.getMaxSpeed());
        flyToPointParam.setPoints(List.of(point));
        return flyToPoint(workspaceId, flyToPointParam);
    }

    /**
     * Publish DRC stick_control command and use the yaw channel to adjust heading.
     */
    @Override
    public HttpResultResponse yawControl(String workspaceId, RcPlus2YawControlParam param) {
        log.info("[RC-CTRL][stick_control] start workspaceId={}, rcSn={}, clientId={}, seq={}",
                workspaceId, param.getRcSn(), param.getClientId(), param.getSeq());
        DeviceDTO rc = validateDrcOwnership(param.getRcSn(), param.getClientId());
        String gatewaySn = gatewaySnOrThrow(rc, param.getRcSn());
        ensureFlightAuthority(gatewaySn);

        long seq = Objects.requireNonNullElse(param.getSeq(), System.currentTimeMillis());
        StickControlRequest request = new StickControlRequest()
                .setRoll(param.getRoll())
                .setPitch(param.getPitch())
                .setThrottle(param.getThrottle())
                .setYaw(param.getYaw());
        String drcDownTopic = TopicConst.THING_MODEL_PRE + TopicConst.PRODUCT + gatewaySn + TopicConst.DRC + TopicConst.DOWN;
        log.info("[RC-CTRL][stick_control] publish method={} topic={} gateway_sn={}, seq={}, roll={}, pitch={}, throttle={}, yaw={}",
                ControlMethodEnum.STICK_CONTROL.getMethod(),
                drcDownTopic,
                gatewaySn,
                seq,
                request.getRoll(),
                request.getPitch(),
                request.getThrottle(),
                request.getYaw());
        TopicDrcRequest<StickControlRequest> drcPayload = new TopicDrcRequest<StickControlRequest>()
                .setMethod(ControlMethodEnum.STICK_CONTROL.getMethod())
                .setSeq(seq)
                .setData(request);
        log.info("[RC-CTRL][stick_control] payload={}", toJsonForLog(drcPayload));

        drcDownPublish.publish(gatewaySn, ControlMethodEnum.STICK_CONTROL.getMethod(), request, seq);
        log.info("[RC-CTRL][stick_control] dispatched gateway_sn={}, seq={}", gatewaySn, seq);
        return HttpResultResponse.success();
    }

    @Override
    public HttpResultResponse latestOsd(String workspaceId, RcPlus2OsdLatestParam param) {
        log.info("[RC-CTRL][osd_latest] start workspaceId={}, rcSn={}, deviceSn={}, clientId={}",
                workspaceId, param.getRcSn(), param.getDeviceSn(), param.getClientId());
        DeviceDTO rc = validateDrcOwnership(param.getRcSn(), param.getClientId());
        gatewaySnOrThrow(rc, param.getRcSn());
        verifyRequestedDeviceSn(rc, param.getDeviceSn());
        String osdTopic = TopicConst.THING_MODEL_PRE + TopicConst.PRODUCT + param.getDeviceSn() + TopicConst.OSD_SUF;
        long waitTimeoutMs = Objects.requireNonNullElse(param.getWaitTimeoutMs(), RC_OSD_WAIT_TIMEOUT_MS);
        log.info("[RC-CTRL][osd_latest] wait next topic={} device_sn={} timeoutMs={}",
                osdTopic, param.getDeviceSn(), waitTimeoutMs);
        Object osdData;
        try {
            osdData = rcPlus2DrcOsdWaitService.awaitNext(param.getDeviceSn(), waitTimeoutMs);
        } catch (TimeoutException ex) {
            log.warn("[RC-CTRL][osd_latest] timeout waiting /osd. device_sn={}, topic={}, timeoutMs={}",
                    param.getDeviceSn(), osdTopic, waitTimeoutMs);
            return HttpResultResponse.error("No /osd message received within " + waitTimeoutMs + "ms for device_sn=" + param.getDeviceSn());
        }
        log.info("[RC-CTRL][osd_latest] success device_sn={}, topic={}, data={}",
                param.getDeviceSn(), osdTopic, toJsonForLog(osdData));
        return HttpResultResponse.success(osdData);
    }

    private OsdRcDrone fetchCurrentOsdAfterDelay(List<RcPlus2FlyToPointActionsParam.Action> actions,
                                                  String deviceSn,
                                                  Long delayMs,
                                                  Long waitTimeoutMs) {
        long actualDelayMs = Objects.requireNonNullElse(delayMs, 0L);
        long actualWaitTimeoutMs = Objects.requireNonNullElse(waitTimeoutMs, RC_OSD_WAIT_TIMEOUT_MS);
        if (actualDelayMs > 0) {
            log.info("[RC-CTRL][fly_to_point_actions] delay before /osd wait device_sn={}, delayMs={}", deviceSn, actualDelayMs);
            try {
                TimeUnit.MILLISECONDS.sleep(actualDelayMs);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted before waiting /osd message.", ex);
            }
        }
        String osdTopic = TopicConst.THING_MODEL_PRE + TopicConst.PRODUCT + deviceSn + TopicConst.OSD_SUF;
        log.info("[RC-CTRL][fly_to_point_actions] wait next /osd device_sn={}, topic={}, timeoutMs={}, actions={}",
                deviceSn, osdTopic, actualWaitTimeoutMs, toJsonForLog(actions));
        Object osdData;
        try {
            osdData = rcPlus2DrcOsdWaitService.awaitNext(deviceSn, actualWaitTimeoutMs);
        } catch (TimeoutException ex) {
            throw new RuntimeException("No /osd message received within " + actualWaitTimeoutMs + "ms for device_sn=" + deviceSn);
        }
        OsdRcDrone currentOsd = osdData instanceof OsdRcDrone
                ? (OsdRcDrone) osdData
                : objectMapper.convertValue(osdData, OsdRcDrone.class);
        log.info("[RC-CTRL][fly_to_point_actions] got /osd device_sn={}, data={}",
                deviceSn, toJsonForLog(currentOsd));
        return currentOsd;
    }

    /**
     * Validate that RC exists online and belongs to remote-controller domain.
     */
    private DeviceDTO requireOnlineRc(String rcSn) {
        Optional<DeviceDTO> rcOpt = deviceRedisService.getDeviceOnline(rcSn);
        if (rcOpt.isEmpty()) {
            throw new RuntimeException("RC gateway is offline or unknown: " + rcSn);
        }
        DeviceDTO rc = rcOpt.get();
        if (rc.getDomain() != DeviceDomainEnum.REMOTER_CONTROL) {
            throw new RuntimeException("Device is not a remote controller gateway: " + rcSn);
        }
        return rc;
    }

    /**
     * {@code gateway_sn} in DJI Pilot-to-Cloud MQTT paths equals this API's {@code rc_sn} (RC gateway serial).
     * Aircraft serial is {@link DeviceDTO#getChildDeviceSn()} and must not be used in {@code thing/product/{...}/...}.
     */
    private static String gatewaySnOrThrow(DeviceDTO rc, String rcSn) {
        if (!Objects.equals(rc.getDeviceSn(), rcSn)) {
            throw new IllegalStateException(
                    "Gateway SN mismatch: online device_sn=" + rc.getDeviceSn() + ", request rc_sn=" + rcSn);
        }
        return rcSn;
    }

    /**
     * Ensure DRC session owner matches current request client id.
     */
    private DeviceDTO validateDrcOwnership(String rcSn, String clientId) {
        DeviceDTO rc = requireOnlineRc(rcSn);
        String ownerClientId = getDrcClient(rcSn);
        if (!StringUtils.hasText(ownerClientId)) {
            throw new RuntimeException("RC is not in DRC mode. Please call drc/enter first.");
        }
        if (!ownerClientId.equals(clientId)) {
            throw new RuntimeException("DRC session is occupied by another client.");
        }
        return rc;
    }

    private void verifyRequestedDeviceSn(DeviceDTO rc, String requestDeviceSn) {
        String linkedDeviceSn = rc.getChildDeviceSn();
        if (!StringUtils.hasText(linkedDeviceSn)) {
            throw new RuntimeException("No aircraft linked to this RC. Connect aircraft before reading /osd.");
        }
        if (!Objects.equals(linkedDeviceSn, requestDeviceSn)) {
            throw new RuntimeException("device_sn does not match RC linked aircraft. linked=" + linkedDeviceSn + ", request=" + requestDeviceSn);
        }
    }

    private static float normalizeAttitudeHeadTo360(float attitudeHead) {
        float normalized = attitudeHead % 360F;
        if (normalized < 0F) {
            normalized += 360F;
        }
        return normalized;
    }

    /**
     * Grab flight authority before sending realtime control commands.
     *
     * <p>Delegates to {@link IControlService#seizeAuthority} so RC behaves like dock flows:
     * if Redis already marks {@code control_source == A} for this gateway, MQTT
     * {@code flight_authority_grab} is skipped (avoids unnecessary 211001 timeouts).</p>
     */
    private void ensureFlightAuthority(String rcSn) {
        String servicesTopic = TopicConst.THING_MODEL_PRE + TopicConst.PRODUCT + rcSn + TopicConst.SERVICES_SUF;
        String servicesReplyTopic = servicesTopic + TopicConst._REPLY_SUF;
        log.info("[RC-CTRL][authority] request seizeAuthority gateway_sn={}, method={}, topic={}, replyTopic={}",
                rcSn,
                ControlMethodEnum.FLIGHT_AUTHORITY_GRAB.getMethod(),
                servicesTopic,
                servicesReplyTopic);
        HttpResultResponse result = controlService.seizeAuthority(rcSn, DroneAuthorityEnum.FLIGHT, null);
        log.info("[RC-CTRL][authority] seizeAuthority result gateway_sn={}, code={}, message={}",
                rcSn, result.getCode(), result.getMessage());
        if (HttpResultResponse.CODE_SUCCESS != result.getCode()) {
            throw new RuntimeException("Failed to grab flight authority. " + result.getMessage());
        }
    }

    /**
     * RC Plus 2 DRC prerequisite: request cloud control authorization first.
     */
    private void requestCloudControlAuth(String gatewaySn,
                                         String servicesTopic,
                                         String servicesReplyTopic,
                                         String workspaceId,
                                         String clientId) {
        String userId = StringUtils.hasText(workspaceId) ? workspaceId : clientId;
        String userCallsign = clientId;
        CloudControlAuthRequest request = new CloudControlAuthRequest()
                .setUserId(userId)
                .setUserCallsign(userCallsign)
                .setControlKeys(List.of("flight"));
        log.info("[RC-DRC][auth] publish method={} topic={} gateway_sn={}, data={}",
                ControlMethodEnum.CLOUD_CONTROL_AUTH_REQUEST.getMethod(),
                servicesTopic,
                gatewaySn,
                toJsonForLog(request));
        TopicServicesResponse<ServicesReplyData> authReply =
                abstractControlService.cloudControlAuthRequest(SDKManager.getDeviceSDK(gatewaySn), request);
        ServicesReplyData authReplyData = authReply.getData();
        log.info("[RC-DRC][auth] receive reply topic={} method={} gateway_sn={} result={}",
                servicesReplyTopic,
                ControlMethodEnum.CLOUD_CONTROL_AUTH_REQUEST.getMethod(),
                gatewaySn,
                authReplyData == null ? "empty response" : authReplyData.getResult());
        if (Objects.isNull(authReplyData) || !authReplyData.getResult().isSuccess()) {
            throw new RuntimeException(
                    "cloud_control_auth_request failed before drc_mode_enter. "
                            + (authReplyData == null ? "empty response" : authReplyData.getResult()));
        }
    }

    private String toJsonForLog(Object data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception ex) {
            return String.valueOf(data);
        }
    }

    private String buildDrcEnterFailureMessage(String gatewaySn, ServicesReplyData replyData) {
        if (replyData == null || replyData.getResult() == null) {
            return "empty response";
        }
        if (DRC_LINK_REFUSED_CODE == replyData.getResult().getCode()) {
            return replyData.getResult()
                    + "; gateway_sn=" + gatewaySn
                    + ". Pilot likely refused DRC link. Confirm cloud control is authorized on RC, "
                    + "and verify drc MQTT broker address/username/password/expire_time/enable_tls.";
        }
        return replyData.getResult().toString();
    }

    /**
     * Persist ACL permissions for the given DRC session.
     */
    private void refreshAcl(String rcSn, String clientId, String pubTopic, String subTopic) {
        RedisOpsUtils.setWithExpire(RedisConst.DRC_PREFIX + rcSn, clientId, RedisConst.DRC_MODE_ALIVE_SECOND);
        String key = RedisConst.MQTT_ACL_PREFIX + clientId;
        RedisOpsUtils.hashSet(key, pubTopic, MqttAclAccessEnum.PUB.getValue());
        RedisOpsUtils.hashSet(key, subTopic, MqttAclAccessEnum.SUB.getValue());
        RedisOpsUtils.expireKey(key, RedisConst.DRC_MODE_ALIVE_SECOND);
        log.info("[RC-DRC][acl] refreshed redisKey={}, ownerKey={}, pubTopic={}, subTopic={}, ttlSec={}",
                key, RedisConst.DRC_PREFIX + rcSn, pubTopic, subTopic, RedisConst.DRC_MODE_ALIVE_SECOND);
    }

    /**
     * Query current DRC owner client id from redis.
     */
    private String getDrcClient(String rcSn) {
        return (String) RedisOpsUtils.get(RedisConst.DRC_PREFIX + rcSn);
    }
}
