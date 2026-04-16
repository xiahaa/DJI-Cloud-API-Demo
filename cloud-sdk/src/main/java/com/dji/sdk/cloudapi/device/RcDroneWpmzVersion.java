package com.dji.sdk.cloudapi.device;

/**
 * WPMZ template version reported on RC / direct-drone state ({@code wpmz_version}).
 */
public class RcDroneWpmzVersion {

    private String wpmzVersion;

    public RcDroneWpmzVersion() {
    }

    @Override
    public String toString() {
        return "RcDroneWpmzVersion{" +
                "wpmzVersion='" + wpmzVersion + '\'' +
                '}';
    }

    public String getWpmzVersion() {
        return wpmzVersion;
    }

    public RcDroneWpmzVersion setWpmzVersion(String wpmzVersion) {
        this.wpmzVersion = wpmzVersion;
        return this;
    }
}
