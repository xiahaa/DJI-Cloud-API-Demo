package com.dji.sdk.cloudapi.device;

/**
 * RC / aircraft state: BeiDou-related firmware flag ({@code is_beidou_version}).
 */
public class RcIsBeidouVersionState {

    private Boolean isBeidouVersion;

    public RcIsBeidouVersionState() {
    }

    @Override
    public String toString() {
        return "RcIsBeidouVersionState{" +
                "isBeidouVersion=" + isBeidouVersion +
                '}';
    }

    public Boolean getIsBeidouVersion() {
        return isBeidouVersion;
    }

    public RcIsBeidouVersionState setIsBeidouVersion(Boolean isBeidouVersion) {
        this.isBeidouVersion = isBeidouVersion;
        return this;
    }
}
