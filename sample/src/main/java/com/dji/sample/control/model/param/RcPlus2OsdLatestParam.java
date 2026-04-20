package com.dji.sample.control.model.param;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

/**
 * Query parameter for latest DRC osd_info_push data.
 */
@Data
public class RcPlus2OsdLatestParam {

    @NotBlank
    private String clientId;

    @NotBlank
    private String rcSn;

    /**
     * Aircraft device sn. OSD topic is thing/product/{device_sn}/osd.
     */
    @NotBlank
    private String deviceSn;

    /**
     * Optional wait timeout (ms) for one-shot /osd message.
     */
    @Min(1000)
    @Max(30000)
    private Long waitTimeoutMs;
}
