package com.dji.sdk.cloudapi.device;

import java.util.List;

/**
 * RC state: per-camera reporting ({@code cameras}).
 */
public class RcCamerasState {

    private List<RcCamerasData> cameras;

    public RcCamerasState() {
    }

    @Override
    public String toString() {
        return "RcCamerasState{" +
                "cameras=" + cameras +
                '}';
    }

    public List<RcCamerasData> getCameras() {
        return cameras;
    }

    public RcCamerasState setCameras(List<RcCamerasData> cameras) {
        this.cameras = cameras;
        return this;
    }
}
