package com.dji.sdk.cloudapi.device;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * RC state: cloud control authorization list (payload key {@code cloud_control_auth}).
 * Element schema may evolve; items are kept as {@link JsonNode} until typed in the Cloud API spec.
 */
public class RcCloudControlAuthState {

    private List<JsonNode> cloudControlAuth;

    public RcCloudControlAuthState() {
    }

    @Override
    public String toString() {
        return "RcCloudControlAuthState{" +
                "cloudControlAuth=" + cloudControlAuth +
                '}';
    }

    public List<JsonNode> getCloudControlAuth() {
        return cloudControlAuth;
    }

    public RcCloudControlAuthState setCloudControlAuth(List<JsonNode> cloudControlAuth) {
        this.cloudControlAuth = cloudControlAuth;
        return this;
    }
}
