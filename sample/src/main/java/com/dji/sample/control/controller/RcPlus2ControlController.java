package com.dji.sample.control.controller;

import com.dji.sample.common.model.CustomClaim;
import com.dji.sample.control.model.dto.JwtAclDTO;
import com.dji.sample.control.model.param.RcPlus2DrcConnectParam;
import com.dji.sample.control.model.param.RcPlus2DrcEnterParam;
import com.dji.sample.control.model.param.RcPlus2FlyToPointActionsParam;
import com.dji.sample.control.model.param.RcPlus2FlyToPointParam;
import com.dji.sample.control.model.param.RcPlus2OsdLatestParam;
import com.dji.sample.control.model.param.RcPlus2YawControlParam;
import com.dji.sample.control.service.IRcPlus2ControlService;
import com.dji.sdk.cloudapi.control.DrcModeMqttBroker;
import com.dji.sdk.common.HttpResultResponse;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import static com.dji.sample.component.AuthInterceptor.TOKEN_CLAIM;

/**
 * RC Plus 2 realtime flight-control API.
 *
 * <p>This controller follows Pilot-to-Cloud DRC flows and provides only two business
 * controls: fly-to-point and yaw adjustment. DRC auth/enter/exit is provided as pre-steps.</p>
 */
@RestController
@RequestMapping("${url.control.prefix}${url.control.version}/pilot/rc-plus-2")
public class RcPlus2ControlController {

    private final IRcPlus2ControlService rcPlus2ControlService;

    public RcPlus2ControlController(IRcPlus2ControlService rcPlus2ControlService) {
        this.rcPlus2ControlService = rcPlus2ControlService;
    }

    /**
     * Build MQTT credentials for DRC client connection.
     */
    @PostMapping("/workspaces/{workspace_id}/drc/connect")
    public HttpResultResponse<DrcModeMqttBroker> drcConnect(@PathVariable("workspace_id") String workspaceId,
                                                             HttpServletRequest request,
                                                             @Valid @RequestBody RcPlus2DrcConnectParam param) {
        CustomClaim claims = (CustomClaim) request.getAttribute(TOKEN_CLAIM);
        DrcModeMqttBroker broker = rcPlus2ControlService.drcConnect(workspaceId, claims.getId(), claims.getUsername(), param);
        return HttpResultResponse.success(broker);
    }

    /**
     * Enter DRC mode for target RC and grant pub/sub ACL.
     */
    @PostMapping("/workspaces/{workspace_id}/drc/enter")
    public HttpResultResponse<JwtAclDTO> drcEnter(@PathVariable("workspace_id") String workspaceId,
                                                   @Valid @RequestBody RcPlus2DrcEnterParam param) {
        JwtAclDTO acl = rcPlus2ControlService.drcEnter(workspaceId, param);
        return HttpResultResponse.success(acl);
    }

    /**
     * Exit DRC mode and revoke corresponding ACL/session.
     */
    @PostMapping("/workspaces/{workspace_id}/drc/exit")
    public HttpResultResponse drcExit(@PathVariable("workspace_id") String workspaceId,
                                      @Valid @RequestBody RcPlus2DrcEnterParam param) {
        rcPlus2ControlService.drcExit(workspaceId, param);
        return HttpResultResponse.success();
    }

    /**
     * Fly aircraft to target point(s) through RC gateway.
     */
    @PostMapping("/workspaces/{workspace_id}/flight/fly-to-point")
    public HttpResultResponse flyToPoint(@PathVariable("workspace_id") String workspaceId,
                                         @Valid @RequestBody RcPlus2FlyToPointParam param) {
        return rcPlus2ControlService.flyToPoint(workspaceId, param);
    }

    /**
     * Transform actions to target point and trigger fly_to_point.
     */
    @PostMapping("/workspaces/{workspace_id}/flight/fly-to-point-by-actions")
    public HttpResultResponse flyToPointByActions(@PathVariable("workspace_id") String workspaceId,
                                                  @Valid @RequestBody RcPlus2FlyToPointActionsParam param) {
        return rcPlus2ControlService.flyToPointByActions(workspaceId, param);
    }

    /**
     * Adjust aircraft yaw through DRC joystick command.
     */
    @PostMapping("/workspaces/{workspace_id}/flight/yaw")
    public HttpResultResponse yawControl(@PathVariable("workspace_id") String workspaceId,
                                         @Valid @RequestBody RcPlus2YawControlParam param) {
        return rcPlus2ControlService.yawControl(workspaceId, param);
    }

    /**
     * Get latest aircraft /osd data by device_sn.
     */
    @PostMapping("/workspaces/{workspace_id}/flight/osd/latest")
    public HttpResultResponse latestOsd(@PathVariable("workspace_id") String workspaceId,
                                        @Valid @RequestBody RcPlus2OsdLatestParam param) {
        return rcPlus2ControlService.latestOsd(workspaceId, param);
    }
}
