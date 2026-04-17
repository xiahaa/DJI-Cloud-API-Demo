package com.dji.sample.control.model.param;

import com.dji.sample.component.redis.RedisConst;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

/**
 * RC Plus 2 DRC mqtt connection parameter.
 */
@Data
public class RcPlus2DrcConnectParam {

    /**
     * Optional client id. If absent, server generates one.
     */
    private String clientId;

    /**
     * Session token expiry in seconds.
     */
    @Range(min = 1800, max = 86400)
    private long expireSec = RedisConst.DRC_MODE_ALIVE_SECOND;
}
