package com.dji.sdk.cloudapi.device;

/**
 * Single camera entry inside {@link RcCamerasState#getCameras()}.
 */
public class RcCamerasData {

    private Integer cameraMode;
    private String payloadIndex;
    private Integer photoState;
    private Long recordTime;
    private Integer recordingState;
    private Integer remainPhotoNum;
    private Long remainRecordDuration;
    private Integer wideExposureMode;
    private Integer wideExposureValue;
    private Integer wideIso;
    private Integer wideShutterSpeed;
    private Integer zoomCalibrateFarthestFocusValue;
    private Integer zoomCalibrateNearestFocusValue;
    private Integer zoomExposureMode;
    private Integer zoomExposureValue;
    private Integer zoomFocusMode;
    private Integer zoomFocusState;
    private Integer zoomFocusValue;
    private Integer zoomMaxFocusValue;
    private Integer zoomMinFocusValue;
    private Integer zoomShutterSpeed;

    public RcCamerasData() {
    }

    @Override
    public String toString() {
        return "RcCamerasData{" +
                "cameraMode=" + cameraMode +
                ", payloadIndex='" + payloadIndex + '\'' +
                ", photoState=" + photoState +
                ", recordTime=" + recordTime +
                ", recordingState=" + recordingState +
                ", remainPhotoNum=" + remainPhotoNum +
                ", remainRecordDuration=" + remainRecordDuration +
                ", wideExposureMode=" + wideExposureMode +
                ", wideExposureValue=" + wideExposureValue +
                ", wideIso=" + wideIso +
                ", wideShutterSpeed=" + wideShutterSpeed +
                ", zoomCalibrateFarthestFocusValue=" + zoomCalibrateFarthestFocusValue +
                ", zoomCalibrateNearestFocusValue=" + zoomCalibrateNearestFocusValue +
                ", zoomExposureMode=" + zoomExposureMode +
                ", zoomExposureValue=" + zoomExposureValue +
                ", zoomFocusMode=" + zoomFocusMode +
                ", zoomFocusState=" + zoomFocusState +
                ", zoomFocusValue=" + zoomFocusValue +
                ", zoomMaxFocusValue=" + zoomMaxFocusValue +
                ", zoomMinFocusValue=" + zoomMinFocusValue +
                ", zoomShutterSpeed=" + zoomShutterSpeed +
                '}';
    }

    public Integer getCameraMode() {
        return cameraMode;
    }

    public RcCamerasData setCameraMode(Integer cameraMode) {
        this.cameraMode = cameraMode;
        return this;
    }

    public String getPayloadIndex() {
        return payloadIndex;
    }

    public RcCamerasData setPayloadIndex(String payloadIndex) {
        this.payloadIndex = payloadIndex;
        return this;
    }

    public Integer getPhotoState() {
        return photoState;
    }

    public RcCamerasData setPhotoState(Integer photoState) {
        this.photoState = photoState;
        return this;
    }

    public Long getRecordTime() {
        return recordTime;
    }

    public RcCamerasData setRecordTime(Long recordTime) {
        this.recordTime = recordTime;
        return this;
    }

    public Integer getRecordingState() {
        return recordingState;
    }

    public RcCamerasData setRecordingState(Integer recordingState) {
        this.recordingState = recordingState;
        return this;
    }

    public Integer getRemainPhotoNum() {
        return remainPhotoNum;
    }

    public RcCamerasData setRemainPhotoNum(Integer remainPhotoNum) {
        this.remainPhotoNum = remainPhotoNum;
        return this;
    }

    public Long getRemainRecordDuration() {
        return remainRecordDuration;
    }

    public RcCamerasData setRemainRecordDuration(Long remainRecordDuration) {
        this.remainRecordDuration = remainRecordDuration;
        return this;
    }

    public Integer getWideExposureMode() {
        return wideExposureMode;
    }

    public RcCamerasData setWideExposureMode(Integer wideExposureMode) {
        this.wideExposureMode = wideExposureMode;
        return this;
    }

    public Integer getWideExposureValue() {
        return wideExposureValue;
    }

    public RcCamerasData setWideExposureValue(Integer wideExposureValue) {
        this.wideExposureValue = wideExposureValue;
        return this;
    }

    public Integer getWideIso() {
        return wideIso;
    }

    public RcCamerasData setWideIso(Integer wideIso) {
        this.wideIso = wideIso;
        return this;
    }

    public Integer getWideShutterSpeed() {
        return wideShutterSpeed;
    }

    public RcCamerasData setWideShutterSpeed(Integer wideShutterSpeed) {
        this.wideShutterSpeed = wideShutterSpeed;
        return this;
    }

    public Integer getZoomCalibrateFarthestFocusValue() {
        return zoomCalibrateFarthestFocusValue;
    }

    public RcCamerasData setZoomCalibrateFarthestFocusValue(Integer zoomCalibrateFarthestFocusValue) {
        this.zoomCalibrateFarthestFocusValue = zoomCalibrateFarthestFocusValue;
        return this;
    }

    public Integer getZoomCalibrateNearestFocusValue() {
        return zoomCalibrateNearestFocusValue;
    }

    public RcCamerasData setZoomCalibrateNearestFocusValue(Integer zoomCalibrateNearestFocusValue) {
        this.zoomCalibrateNearestFocusValue = zoomCalibrateNearestFocusValue;
        return this;
    }

    public Integer getZoomExposureMode() {
        return zoomExposureMode;
    }

    public RcCamerasData setZoomExposureMode(Integer zoomExposureMode) {
        this.zoomExposureMode = zoomExposureMode;
        return this;
    }

    public Integer getZoomExposureValue() {
        return zoomExposureValue;
    }

    public RcCamerasData setZoomExposureValue(Integer zoomExposureValue) {
        this.zoomExposureValue = zoomExposureValue;
        return this;
    }

    public Integer getZoomFocusMode() {
        return zoomFocusMode;
    }

    public RcCamerasData setZoomFocusMode(Integer zoomFocusMode) {
        this.zoomFocusMode = zoomFocusMode;
        return this;
    }

    public Integer getZoomFocusState() {
        return zoomFocusState;
    }

    public RcCamerasData setZoomFocusState(Integer zoomFocusState) {
        this.zoomFocusState = zoomFocusState;
        return this;
    }

    public Integer getZoomFocusValue() {
        return zoomFocusValue;
    }

    public RcCamerasData setZoomFocusValue(Integer zoomFocusValue) {
        this.zoomFocusValue = zoomFocusValue;
        return this;
    }

    public Integer getZoomMaxFocusValue() {
        return zoomMaxFocusValue;
    }

    public RcCamerasData setZoomMaxFocusValue(Integer zoomMaxFocusValue) {
        this.zoomMaxFocusValue = zoomMaxFocusValue;
        return this;
    }

    public Integer getZoomMinFocusValue() {
        return zoomMinFocusValue;
    }

    public RcCamerasData setZoomMinFocusValue(Integer zoomMinFocusValue) {
        this.zoomMinFocusValue = zoomMinFocusValue;
        return this;
    }

    public Integer getZoomShutterSpeed() {
        return zoomShutterSpeed;
    }

    public RcCamerasData setZoomShutterSpeed(Integer zoomShutterSpeed) {
        this.zoomShutterSpeed = zoomShutterSpeed;
        return this;
    }
}
