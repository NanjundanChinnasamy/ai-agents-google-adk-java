package com.google.adk.finance.v10.observability.events;

import com.google.adk.finance.v10.observability.EventType;
import com.google.adk.finance.v10.observability.ExecutionEvent;

import java.util.Map;

/**
 * Event emitted when an agent execution finishes (successfully or with evaluation warnings).
 */
public final class AgentCompletedEvent {

    public static ExecutionEvent create(
            String executionId,
            String agentName,
            long durationMs,
            String status,
            String sanitizedResponseSummary) {

        return ExecutionEvent.builder(executionId, EventType.AGENT_COMPLETED)
                .agentName(agentName)
                .component("AGENT")
                .durationMs(durationMs)
                .status(status != null ? status : "SUCCESS")
                .safeSummary("Agent execution completed with status: " + status)
                .metadata(Map.of(
                        "durationMs", durationMs,
                        "status", status != null ? status : "SUCCESS",
                        "responsePreview", truncate(sanitizedResponseSummary, 120)
                ))
                .build();
    }

    private static String truncate(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text : text.substring(0, max) + "...";
    }
}
