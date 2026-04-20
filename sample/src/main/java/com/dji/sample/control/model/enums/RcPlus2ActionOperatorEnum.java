package com.dji.sample.control.model.enums;

import java.util.Arrays;

/**
 * Action operator for RC Plus 2 fly_to_point transform API.
 */
public enum RcPlus2ActionOperatorEnum {

    FORWARD(1),
    TURN_LEFT(2),
    TURN_RIGHT(3),
    UNKNOWN(-1);

    private final int code;

    RcPlus2ActionOperatorEnum(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static RcPlus2ActionOperatorEnum find(int code) {
        return Arrays.stream(values()).filter(item -> item.code == code).findFirst().orElse(UNKNOWN);
    }
}
