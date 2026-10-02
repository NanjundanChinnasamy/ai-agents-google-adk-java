package com.google.adk.finance.v10.observability.events;

import com.google.adk.finance.v10.observability.EventType;
import com.google.adk.finance.v10.observability.ExecutionEvent;

import java.time.Instant;
import java.util.Map;

/**
 * Event emitted when an agent execution begins.
 * Contains sanitized user request summary without PII or credentials.
 */
public final class AgentStartedEvent {

    public static ExecutionEvent create(
            String executionId,
            String agentName,
            String userId,
            String sessionId,
            String sanitizedPromptSummary) {

        return ExecutionEvent.builder(executionId, EventType.AGENT_STARTED)
                .agentName(agentName)
                .component("AGENT")
                .status("STARTED")
                .safeSummary("Agent execution started: \"" + truncate(sanitizedPromptSummary, 80) + "\"")
                .metadata(Map.of(
                        "userId", userId != null ? userId : "anonymous",
                        "sessionId", sessionId != null ? sessionId : "unknown",
                        "promptPreview", truncate(sanitizedPromptSummary, 120)
                ))
                .build();
    }

    private static String truncate(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text : text.substring(0, max) + "...";
    }
}
