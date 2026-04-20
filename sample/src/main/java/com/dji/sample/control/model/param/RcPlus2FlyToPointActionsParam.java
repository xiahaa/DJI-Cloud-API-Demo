package com.dji.sample.control.model.param;

import lombok.Data;
import org.hibernate.validator.constraints.Range;

import javax.validation.Valid;
import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * RC Plus 2 fly_to_point variant: transform actions to one target point.
 */
@Data
public class RcPlus2FlyToPointActionsParam {

    @NotBlank
    private String clientId;

    @NotBlank
    private String rcSn;

    /**
     * Aircraft device sn used to fetch latest /osd message.
     */
    @NotBlank
    private String deviceSn;

    @Range(min = 1, max = 15)
    @NotNull
    private Integer maxSpeed;

    /**
     * Delay before fetching current osd (ms). 0 means no delay.
     */
    @Min(0)
    @Max(60000)
    private Long delayMs = 0L;

    /**
     * Wait timeout for one-shot /osd message (ms).
     */
    @Min(1000)
    @Max(30000)
    private Long waitTimeoutMs = 10000L;

    /**
     * fly_to_point target height. Defaults to 145 when omitted.
     */
    @DecimalMin("20.0")
    @DecimalMax("500.0")
    private Float height = 145F;

    @NotEmpty
    private List<@Valid Action> actions;

    @Data
    public static class Action {

        /**
         * 1: forward(m), 2: turn-left(deg), 3: turn-right(deg)
         */
        @NotNull
        @Range(min = 1, max = 3)
        private Integer operator;

        @NotNull
        @Min(0)
        private Float value;
    }
}
