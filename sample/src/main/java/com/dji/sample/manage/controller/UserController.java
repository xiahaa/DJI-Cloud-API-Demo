package com.dji.sample.manage.controller;

import com.dji.sample.common.model.CustomClaim;
import com.dji.sample.manage.model.dto.UserDTO;
import com.dji.sample.manage.model.dto.UserListDTO;
import com.dji.sample.manage.service.IUserService;
import com.dji.sdk.common.HttpResultResponse;
import com.dji.sdk.common.PaginationData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

import static com.dji.sample.component.AuthInterceptor.TOKEN_CLAIM;


@Slf4j
@RestController
@RequestMapping("${url.manage.prefix}${url.manage.version}/users")
public class UserController {

    @Autowired
    private IUserService userService;

    /**
     * Query the information of the current user.
     * @param request
     * @return
     */
    @GetMapping("/current")
    public HttpResultResponse getCurrentUserInfo(HttpServletRequest request) {
        CustomClaim customClaim = (CustomClaim)request.getAttribute(TOKEN_CLAIM);
        HttpResultResponse response = userService.getUserByUsername(customClaim.getUsername(), customClaim.getWorkspaceId());
        if (response.getCode() == HttpResultResponse.CODE_SUCCESS && response.getData() instanceof UserDTO) {
            UserDTO u = (UserDTO) response.getData();
            log.info("[link][http] GET /users/current user={} workspaceId={} mqtt_addr={} mqtt_username={} mqtt_password_configured={}",
                    u.getUsername(),
                    u.getWorkspaceId(),
                    u.getMqttAddr(),
                    u.getMqttUsername(),
                    u.getMqttPassword() != null && !u.getMqttPassword().isEmpty());
        } else {
            log.warn("[link][http] GET /users/current failed code={} message={}", response.getCode(), response.getMessage());
        }
        return response;
    }

    /**
     * Paging to query all users in a workspace.
     * @param page      current page
     * @param pageSize
     * @param workspaceId
     * @return
     */
    @GetMapping("/{workspace_id}/users")
    public HttpResultResponse<PaginationData<UserListDTO>> getUsers(@RequestParam(defaultValue = "1") Long page,
                                                                    @RequestParam(value = "page_size", defaultValue = "50") Long pageSize,
                                                                    @PathVariable("workspace_id") String workspaceId) {
        PaginationData<UserListDTO> paginationData = userService.getUsersByWorkspaceId(page, pageSize, workspaceId);
        return HttpResultResponse.success(paginationData);
    }

    /**
     * Modify user information. Only mqtt account information is included, nothing else can be modified.
     * @param user
     * @param workspaceId
     * @param userId
     * @return
     */
    @PutMapping("/{workspace_id}/users/{user_id}")
    public HttpResultResponse updateUser(@RequestBody UserListDTO user,
                                         @PathVariable("workspace_id") String workspaceId,
                                         @PathVariable("user_id") String userId) {

        userService.updateUser(workspaceId, userId, user);
        return HttpResultResponse.success();
    }
}
