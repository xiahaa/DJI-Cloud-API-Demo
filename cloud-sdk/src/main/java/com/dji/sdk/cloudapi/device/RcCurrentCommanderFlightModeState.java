package com.dji.sdk.cloudapi.device;

import com.dji.sdk.cloudapi.control.CommanderFlightModeEnum;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * RC state: current commander flight mode ({@code current_commander_flight_mode}).
 */
public class RcCurrentCommanderFlightModeState {

    @JsonProperty("current_commander_flight_mode")
    private CommanderFlightModeEnum currentCommanderFlightMode;

    public RcCurrentCommanderFlightModeState() {
    }

    @Override
    public String toString() {
        return "RcCurrentCommanderFlightModeState{" +
                "currentCommanderFlightMode=" + currentCommanderFlightMode +
                '}';
    }

    public CommanderFlightModeEnum getCurrentCommanderFlightMode() {
        return currentCommanderFlightMode;
    }

    public RcCurrentCommanderFlightModeState setCurrentCommanderFlightMode(CommanderFlightModeEnum currentCommanderFlightMode) {
        this.currentCommanderFlightMode = currentCommanderFlightMode;
        return this;
    }
}
