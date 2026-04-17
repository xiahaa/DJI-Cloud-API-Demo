package com.dji.sdk.cloudapi.wayline;

import com.dji.sdk.common.BaseModel;

/**
 * Request payload for method {@code in_flight_task_request}.
 */
public class InFlightTaskRequest extends BaseModel {

    private String droneSn;

    private String trackId;

    public InFlightTaskRequest() {
    }

    @Override
    public String toString() {
        return "InFlightTaskRequest{" +
                "droneSn='" + droneSn + '\'' +
                ", trackId='" + trackId + '\'' +
                '}';
    }

    public String getDroneSn() {
        return droneSn;
    }

    public InFlightTaskRequest setDroneSn(String droneSn) {
        this.droneSn = droneSn;
        return this;
    }

    public String getTrackId() {
        return trackId;
    }

    public InFlightTaskRequest setTrackId(String trackId) {
        this.trackId = trackId;
        return this;
    }
}
