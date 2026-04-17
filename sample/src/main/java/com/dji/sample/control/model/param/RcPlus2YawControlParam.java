package com.dji.sample.control.model.param;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * RC Plus 2 yaw-control command parameter based on DRC stick_control.
 */
@Data
public class RcPlus2YawControlParam {

    /**
     * DRC MQTT client id that currently owns control.
     */
    @NotBlank
    private String clientId;

    /**
     * RC gateway SN.
     */
    @NotBlank
    private String rcSn;

    /**
     * Sequence number used by DRC command stream.
     */
    private Long seq;

    /**
     * Roll stick channel (A), range [364, 1684], neutral is 1024.
     */
    @Min(364)
    @Max(1684)
    private Integer roll = 1024;

    /**
     * Pitch stick channel (E), range [364, 1684], neutral is 1024.
     */
    @Min(364)
    @Max(1684)
    private Integer pitch = 1024;

    /**
     * Throttle stick channel (T), range [364, 1684], neutral is 1024.
     */
    @Min(364)
    @Max(1684)
    private Integer throttle = 1024;

    /**
     * Yaw stick channel (R), range [364, 1684], neutral is 1024.
     */
    @NotNull
    @Min(364)
    @Max(1684)
    private Integer yaw;
}
