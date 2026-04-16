package com.dji.sdk.cloudapi.device;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * RC / aircraft state: AI detection boxes ({@code ai_boxes}). Element schema may evolve; kept as {@link JsonNode} until typed.
 */
public class RcAiBoxesState {

    private List<JsonNode> aiBoxes;

    public RcAiBoxesState() {
    }

    @Override
    public String toString() {
        return "RcAiBoxesState{" +
                "aiBoxes=" + aiBoxes +
                '}';
    }

    public List<JsonNode> getAiBoxes() {
        return aiBoxes;
    }

    public RcAiBoxesState setAiBoxes(List<JsonNode> aiBoxes) {
        this.aiBoxes = aiBoxes;
        return this;
    }
}
