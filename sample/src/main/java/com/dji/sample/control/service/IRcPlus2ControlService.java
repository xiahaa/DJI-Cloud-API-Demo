package com.dji.sample.control.service;

import com.dji.sample.control.model.dto.JwtAclDTO;
import com.dji.sample.control.model.param.RcPlus2DrcConnectParam;
import com.dji.sample.control.model.param.RcPlus2DrcEnterParam;
import com.dji.sample.control.model.param.RcPlus2FlyToPointActionsParam;
import com.dji.sample.control.model.param.RcPlus2FlyToPointParam;
import com.dji.sample.control.model.param.RcPlus2OsdLatestParam;
import com.dji.sample.control.model.param.RcPlus2YawControlParam;
import com.dji.sdk.cloudapi.control.DrcModeMqttBroker;
import com.dji.sdk.common.HttpResultResponse;

/**
 * RC Plus 2 realtime flight control service.
 *
 * <p>This service wraps Pilot-to-Cloud DRC control flows for RC gateways.</p>
 */
public interface IRcPlus2ControlService {

    /**
     * Build DRC MQTT broker credentials for client-side DRC connection.
     *
     * @param workspaceId workspace id from request path
     * @param userId current user id
     * @param username current username
     * @param param connection parameter
     * @return drc mqtt broker info with token credentials
     */
    DrcModeMqttBroker drcConnect(String workspaceId, String userId, String username, RcPlus2DrcConnectParam param);

    /**
     * Enter DRC mode and grant topic ACL for the specified RC gateway.
     *
     * @param workspaceId workspace id from request path
     * @param param enter parameter
     * @return granted ACL topics
     */
    JwtAclDTO drcEnter(String workspaceId, RcPlus2DrcEnterParam param);

    /**
     * Exit DRC mode and cleanup session ACL.
     *
     * @param workspaceId workspace id from request path
     * @param param exit parameter
     */
    void drcExit(String workspaceId, RcPlus2DrcEnterParam param);

    /**
     * Trigger fly-to-point mission by RC gateway.
     *
     * @param workspaceId workspace id from request path
     * @param param fly-to-point parameter
     * @return unified http response
     */
    HttpResultResponse flyToPoint(String workspaceId, RcPlus2FlyToPointParam param);

    /**
     * Convert action list to target point and trigger fly_to_point.
     *
     * @param workspaceId workspace id from request path
     * @param param actions transform parameter
     * @return unified http response
     */
    HttpResultResponse flyToPointByActions(String workspaceId, RcPlus2FlyToPointActionsParam param);

    /**
     * Send DRC joystick command for yaw adjustment.
     *
     * @param workspaceId workspace id from request path
     * @param param yaw control parameter
     * @return unified http response
     */
    HttpResultResponse yawControl(String workspaceId, RcPlus2YawControlParam param);

    /**
     * Get latest one-shot /osd payload from target aircraft device_sn.
     *
     * @param workspaceId workspace id from request path
     * @param param query parameter
     * @return latest osd data
     */
    HttpResultResponse latestOsd(String workspaceId, RcPlus2OsdLatestParam param);
}
