package com.dji.sdk.cloudapi.device;

import com.dji.sdk.cloudapi.wayline.RthModeEnum;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;

/**
 * RC state: current RTH mode ({@code current_rth_mode}).
 */
public class RcCurrentRthModeState {

    @JsonProperty("current_rth_mode")
    @NotNull
    private RthModeEnum currentRthMode;

    public RcCurrentRthModeState() {
    }

    @Override
    public String toString() {
        return "RcCurrentRthModeState{" +
                "currentRthMode=" + currentRthMode +
                '}';
    }

    public RthModeEnum getCurrentRthMode() {
        return currentRthMode;
    }

    public RcCurrentRthModeState setCurrentRthMode(RthModeEnum currentRthMode) {
        this.currentRthMode = currentRthMode;
        return this;
    }
}
