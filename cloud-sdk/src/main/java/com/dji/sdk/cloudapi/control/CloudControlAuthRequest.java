package com.dji.sdk.cloudapi.control;

import com.dji.sdk.common.BaseModel;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * Request body for method {@code cloud_control_auth_request}.
 */
public class CloudControlAuthRequest extends BaseModel {

    @NotBlank
    @JsonProperty("user_id")
    private String userId;

    @NotBlank
    @JsonProperty("user_callsign")
    private String userCallsign;

    @NotEmpty
    @Size(min = 1, max = 1)
    @JsonProperty("control_keys")
    private List<String> controlKeys;

    public CloudControlAuthRequest() {
    }

    @Override
    public String toString() {
        return "CloudControlAuthRequest{" +
                "userId='" + userId + '\'' +
                ", userCallsign='" + userCallsign + '\'' +
                ", controlKeys=" + controlKeys +
                '}';
    }

    public String getUserId() {
        return userId;
    }

    public CloudControlAuthRequest setUserId(String userId) {
        this.userId = userId;
        return this;
    }

    public String getUserCallsign() {
        return userCallsign;
    }

    public CloudControlAuthRequest setUserCallsign(String userCallsign) {
        this.userCallsign = userCallsign;
        return this;
    }

    public List<String> getControlKeys() {
        return controlKeys;
    }

    public CloudControlAuthRequest setControlKeys(List<String> controlKeys) {
        this.controlKeys = controlKeys;
        return this;
    }
}
