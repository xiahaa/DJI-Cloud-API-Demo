package com.dji.sdk.cloudapi.device;

/**
 * RC state: commander (point-to-point) flight mode ({@code commander_flight_mode}).
 */
public class RcCommanderFlightModeState {

    private Integer commanderFlightMode;

    public RcCommanderFlightModeState() {
    }

    @Override
    public String toString() {
        return "RcCommanderFlightModeState{" +
                "commanderFlightMode=" + commanderFlightMode +
                '}';
    }

    public Integer getCommanderFlightMode() {
        return commanderFlightMode;
    }

    public RcCommanderFlightModeState setCommanderFlightMode(Integer commanderFlightMode) {
        this.commanderFlightMode = commanderFlightMode;
        return this;
    }
}
