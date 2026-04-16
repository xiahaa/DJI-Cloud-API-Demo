package com.dji.sdk.cloudapi.device;

/**
 * RC state: capability set blob ({@code capability_set}). Schema may add fields; extend nested types as the spec stabilizes.
 */
public class RcCapabilitySetState {

    private CapabilitySetBody capabilitySet;

    public RcCapabilitySetState() {
    }

    @Override
    public String toString() {
        return "RcCapabilitySetState{" +
                "capabilitySet=" + capabilitySet +
                '}';
    }

    public CapabilitySetBody getCapabilitySet() {
        return capabilitySet;
    }

    public RcCapabilitySetState setCapabilitySet(CapabilitySetBody capabilitySet) {
        this.capabilitySet = capabilitySet;
        return this;
    }

    public static class CapabilitySetBody {

        private CloudControlAuthCapability cloudControlAuth;

        public CapabilitySetBody() {
        }

        @Override
        public String toString() {
            return "CapabilitySetBody{" +
                    "cloudControlAuth=" + cloudControlAuth +
                    '}';
        }

        public CloudControlAuthCapability getCloudControlAuth() {
            return cloudControlAuth;
        }

        public CapabilitySetBody setCloudControlAuth(CloudControlAuthCapability cloudControlAuth) {
            this.cloudControlAuth = cloudControlAuth;
            return this;
        }
    }

    public static class CloudControlAuthCapability {

        private Boolean support;

        public CloudControlAuthCapability() {
        }

        @Override
        public String toString() {
            return "CloudControlAuthCapability{" +
                    "support=" + support +
                    '}';
        }

        public Boolean getSupport() {
            return support;
        }

        public CloudControlAuthCapability setSupport(Boolean support) {
            this.support = support;
            return this;
        }
    }
}
