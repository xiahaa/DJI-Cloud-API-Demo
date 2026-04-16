package com.dji.sdk.cloudapi.device;

/**
 * RC state: camera watermark OSD settings ({@code camera_watermark_settings}).
 */
public class RcCameraWatermarkSettingsState {

    private Integer datetimeEnable;
    private Integer droneSnEnable;
    private Integer droneTypeEnable;
    private Integer globalEnable;
    private Integer gpsEnable;
    private Integer layout;
    private String userCustomString;
    private Integer userCustomStringEnable;

    public RcCameraWatermarkSettingsState() {
    }

    @Override
    public String toString() {
        return "RcCameraWatermarkSettingsState{" +
                "datetimeEnable=" + datetimeEnable +
                ", droneSnEnable=" + droneSnEnable +
                ", droneTypeEnable=" + droneTypeEnable +
                ", globalEnable=" + globalEnable +
                ", gpsEnable=" + gpsEnable +
                ", layout=" + layout +
                ", userCustomString='" + userCustomString + '\'' +
                ", userCustomStringEnable=" + userCustomStringEnable +
                '}';
    }

    public Integer getDatetimeEnable() {
        return datetimeEnable;
    }

    public RcCameraWatermarkSettingsState setDatetimeEnable(Integer datetimeEnable) {
        this.datetimeEnable = datetimeEnable;
        return this;
    }

    public Integer getDroneSnEnable() {
        return droneSnEnable;
    }

    public RcCameraWatermarkSettingsState setDroneSnEnable(Integer droneSnEnable) {
        this.droneSnEnable = droneSnEnable;
        return this;
    }

    public Integer getDroneTypeEnable() {
        return droneTypeEnable;
    }

    public RcCameraWatermarkSettingsState setDroneTypeEnable(Integer droneTypeEnable) {
        this.droneTypeEnable = droneTypeEnable;
        return this;
    }

    public Integer getGlobalEnable() {
        return globalEnable;
    }

    public RcCameraWatermarkSettingsState setGlobalEnable(Integer globalEnable) {
        this.globalEnable = globalEnable;
        return this;
    }

    public Integer getGpsEnable() {
        return gpsEnable;
    }

    public RcCameraWatermarkSettingsState setGpsEnable(Integer gpsEnable) {
        this.gpsEnable = gpsEnable;
        return this;
    }

    public Integer getLayout() {
        return layout;
    }

    public RcCameraWatermarkSettingsState setLayout(Integer layout) {
        this.layout = layout;
        return this;
    }

    public String getUserCustomString() {
        return userCustomString;
    }

    public RcCameraWatermarkSettingsState setUserCustomString(String userCustomString) {
        this.userCustomString = userCustomString;
        return this;
    }

    public Integer getUserCustomStringEnable() {
        return userCustomStringEnable;
    }

    public RcCameraWatermarkSettingsState setUserCustomStringEnable(Integer userCustomStringEnable) {
        this.userCustomStringEnable = userCustomStringEnable;
        return this;
    }
}
