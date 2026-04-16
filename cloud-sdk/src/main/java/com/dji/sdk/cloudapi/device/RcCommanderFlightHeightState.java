package com.dji.sdk.cloudapi.device;

/**
 * RC state: commander flight height ({@code commander_flight_height}).
 */
public class RcCommanderFlightHeightState {

    private Integer commanderFlightHeight;

    public RcCommanderFlightHeightState() {
    }

    @Override
    public String toString() {
        return "RcCommanderFlightHeightState{" +
                "commanderFlightHeight=" + commanderFlightHeight +
                '}';
    }

    public Integer getCommanderFlightHeight() {
        return commanderFlightHeight;
    }

    public RcCommanderFlightHeightState setCommanderFlightHeight(Integer commanderFlightHeight) {
        this.commanderFlightHeight = commanderFlightHeight;
        return this;
    }
}
