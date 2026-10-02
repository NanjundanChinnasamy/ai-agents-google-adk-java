package com.google.adk.finance.v10.observability.events;

import com.google.adk.finance.v10.observability.EventType;
import com.google.adk.finance.v10.observability.ExecutionEvent;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Event helper for system, execution, or persistence errors.
 */
public final class ErrorEvent {

    public static ExecutionEvent create(
            String executionId,
            String agentName,
            String component,
            String errorMessage,
            String exceptionClass,
            boolean isFatal) {

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("component", component != null ? component : "SYSTEM");
        meta.put("exceptionClass", exceptionClass != null ? exceptionClass : "Exception");
        meta.put("isFatal", isFatal);

        String safeError = errorMessage != null ? errorMessage : "Unknown error";

        return ExecutionEvent.builder(executionId, EventType.ERROR)
                .agentName(agentName)
                .component(component != null ? component : "SYSTEM")
                .status("FAILED")
                .errorInfo(safeError)
                .safeSummary(String.format("Error in [%s]: %s", component, safeError))
                .metadata(meta)
                .build();
    }

    public static ExecutionEvent persistenceFailed(
            String executionId,
            String agentName,
            String errorMessage) {

        return ExecutionEvent.builder(executionId, EventType.PERSISTENCE_FAILED)
                .agentName(agentName)
                .component("PERSISTENCE")
                .status("FAILED")
                .errorInfo(errorMessage)
                .safeSummary("Telemetry persistence failed: " + errorMessage)
                .metadata(Map.of("critical", false))
                .build();
    }
}
