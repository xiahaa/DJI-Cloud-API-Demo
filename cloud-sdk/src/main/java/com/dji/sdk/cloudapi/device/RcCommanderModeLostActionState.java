package com.dji.sdk.cloudapi.device;

/**
 * RC state: commander mode lost-link action ({@code commander_mode_lost_action}).
 */
public class RcCommanderModeLostActionState {

    private Integer commanderModeLostAction;

    public RcCommanderModeLostActionState() {
    }

    @Override
    public String toString() {
        return "RcCommanderModeLostActionState{" +
                "commanderModeLostAction=" + commanderModeLostAction +
                '}';
    }

    public Integer getCommanderModeLostAction() {
        return commanderModeLostAction;
    }

    public RcCommanderModeLostActionState setCommanderModeLostAction(Integer commanderModeLostAction) {
        this.commanderModeLostAction = commanderModeLostAction;
        return this;
    }
}
