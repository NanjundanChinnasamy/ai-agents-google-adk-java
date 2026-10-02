package com.google.adk.finance.v10.observability.events;

import com.google.adk.finance.v10.observability.EventType;
import com.google.adk.finance.v10.observability.ExecutionEvent;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Event helper for LLM model calls (start, complete, fail).
 * Tracks model identifier, latency, and token usage ("UNKNOWN" when unexposed by ADK).
 */
public final class ModelCallEvent {

    public static final String TOKEN_USAGE_UNKNOWN = "UNKNOWN";

    public static ExecutionEvent started(String executionId, String agentName, String modelName) {
        return ExecutionEvent.builder(executionId, EventType.MODEL_CALL_STARTED)
                .agentName(agentName)
                .component("MODEL")
                .status("STARTED")
                .safeSummary("Invoking LLM model: " + modelName)
                .metadata(Map.of("modelName", modelName != null ? modelName : "unknown"))
                .build();
    }

    public static ExecutionEvent completed(
            String executionId,
            String agentName,
            String modelName,
            long durationMs,
            String tokenUsage) {

        String tokens = tokenUsage != null && !tokenUsage.isBlank() ? tokenUsage : TOKEN_USAGE_UNKNOWN;
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("modelName", modelName != null ? modelName : "unknown");
        meta.put("durationMs", durationMs);
        meta.put("tokenUsage", tokens);

        return ExecutionEvent.builder(executionId, EventType.MODEL_CALL_COMPLETED)
                .agentName(agentName)
                .component("MODEL")
                .durationMs(durationMs)
                .status("SUCCESS")
                .safeSummary(String.format("Model '%s' generated response in %dms (tokens: %s)",
                        modelName != null ? modelName : "unknown", durationMs, tokens))
                .metadata(meta)
                .build();
    }

    public static ExecutionEvent failed(
            String executionId,
            String agentName,
            String modelName,
            long durationMs,
            String errorMessage) {

        return ExecutionEvent.builder(executionId, EventType.MODEL_CALL_FAILED)
                .agentName(agentName)
                .component("MODEL")
                .durationMs(durationMs)
                .status("FAILED")
                .errorInfo(errorMessage)
                .safeSummary(String.format("Model '%s' call failed after %dms: %s",
                        modelName != null ? modelName : "unknown", durationMs, errorMessage))
                .metadata(Map.of(
                        "modelName", modelName != null ? modelName : "unknown",
                        "durationMs", durationMs
                ))
                .build();
    }
}
