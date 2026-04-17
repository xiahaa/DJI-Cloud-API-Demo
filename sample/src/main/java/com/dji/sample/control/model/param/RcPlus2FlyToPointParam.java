package com.dji.sample.control.model.param;

import com.dji.sdk.cloudapi.control.Point;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * RC Plus 2 fly-to-point business parameter.
 */
@Data
public class RcPlus2FlyToPointParam {

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
     * Max speed in m/s.
     */
    @Range(min = 1, max = 15)
    @NotNull
    private Integer maxSpeed;

    /**
     * Target points (lat/lon/height). For M30-series only one point is supported.
     */
    @Size(min = 1)
    @NotNull
    private List<@Valid Point> points;
}
