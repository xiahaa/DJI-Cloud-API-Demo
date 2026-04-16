package com.dji.sdk.cloudapi.device;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * RC state: PSDK widget values (payload key {@code psdk_widget_values}).
 * Structure may vary by payload; items are {@link JsonNode} until fully specified in the Cloud API.
 */
public class RcPsdkWidgetValuesState {

    private List<JsonNode> psdkWidgetValues;

    public RcPsdkWidgetValuesState() {
    }

    @Override
    public String toString() {
        return "RcPsdkWidgetValuesState{" +
                "psdkWidgetValues=" + psdkWidgetValues +
                '}';
    }

    public List<JsonNode> getPsdkWidgetValues() {
        return psdkWidgetValues;
    }

    public RcPsdkWidgetValuesState setPsdkWidgetValues(List<JsonNode> psdkWidgetValues) {
        this.psdkWidgetValues = psdkWidgetValues;
        return this;
    }
}
