package com.dji.sdk.cloudapi.control;

import com.dji.sdk.common.BaseModel;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * DRC stick-control command (RC Plus 2).
 *
 * <p>Stick channels map to A/E/T/R and use a neutral value of 1024.</p>
 */
public class StickControlRequest extends BaseModel {

    @NotNull
    @Min(364)
    @Max(1684)
    private Integer roll;

    @NotNull
    @Min(364)
    @Max(1684)
    private Integer pitch;

    @NotNull
    @Min(364)
    @Max(1684)
    private Integer throttle;

    @NotNull
    @Min(364)
    @Max(1684)
    private Integer yaw;

    public Integer getRoll() {
        return roll;
    }

    public StickControlRequest setRoll(Integer roll) {
        this.roll = roll;
        return this;
    }

    public Integer getPitch() {
        return pitch;
    }

    public StickControlRequest setPitch(Integer pitch) {
        this.pitch = pitch;
        return this;
    }

    public Integer getThrottle() {
        return throttle;
    }

    public StickControlRequest setThrottle(Integer throttle) {
        this.throttle = throttle;
        return this;
    }

    public Integer getYaw() {
        return yaw;
    }

    public StickControlRequest setYaw(Integer yaw) {
        this.yaw = yaw;
        return this;
    }

    @Override
    public String toString() {
        return "StickControlRequest{" +
                "roll=" + roll +
                ", pitch=" + pitch +
                ", throttle=" + throttle +
                ", yaw=" + yaw +
                '}';
    }
}
