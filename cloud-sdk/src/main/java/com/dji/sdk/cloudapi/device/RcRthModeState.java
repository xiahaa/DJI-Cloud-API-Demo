package com.dji.sdk.cloudapi.device;

/**
 * RC gateway state: return-to-home mode ({@code rth_mode}).
 */
public class RcRthModeState {

    private Integer rthMode;

    public RcRthModeState() {
    }

    @Override
    public String toString() {
        return "RcRthModeState{" +
                "rthMode=" + rthMode +
                '}';
    }

    public Integer getRthMode() {
        return rthMode;
    }

    public RcRthModeState setRthMode(Integer rthMode) {
        this.rthMode = rthMode;
        return this;
    }
}
