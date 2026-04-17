package com.dji.sample.control.model.param;

import com.dji.sample.component.redis.RedisConst;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.NotBlank;

/**
 * RC Plus 2 DRC enter/exit parameter.
 */
@Data
public class RcPlus2DrcEnterParam {

    /**
     * DRC MQTT client id returned by connect api.
     */
    @NotBlank
    private String clientId;

    /**
     * RC gateway SN (DJI RC Plus 2 SN).
     */
    @NotBlank
    private String rcSn;

    /**
     * Session token expiry in seconds.
     */
    @Range(min = 1800, max = 86400)
    private long expireSec = RedisConst.DRC_MODE_ALIVE_SECOND;
}
