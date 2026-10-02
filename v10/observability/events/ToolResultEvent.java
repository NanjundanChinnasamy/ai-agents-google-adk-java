package com.google.adk.finance.v10.observability.events;

import com.google.adk.finance.v10.observability.EventType;
import com.google.adk.finance.v10.observability.ExecutionEvent;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Event helper for tool execution results (completed successfully or failed).
 */
public final class ToolResultEvent {

    public static ExecutionEvent completed(
            String executionId,
            String agentName,
            String toolName,
            String toolSource,
            long durationMs,
            String safeResultSummary) {

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("toolName", toolName != null ? toolName : "unknown_tool");
        meta.put("toolSource", toolSource != null ? toolSource : ToolCallEvent.resolveSource(toolName));
        meta.put("durationMs", durationMs);
        meta.put("status", "SUCCESS");

        String summary = String.format("Tool '%s' completed in %dms (status=SUCCESS)",
                toolName, durationMs);
        if (safeResultSummary != null && !safeResultSummary.isBlank()) {
            summary += ": " + truncate(safeResultSummary, 80);
        }

        return ExecutionEvent.builder(executionId, EventType.TOOL_CALL_COMPLETED)
                .agentName(agentName)
                .component("TOOL")
                .durationMs(durationMs)
                .status("SUCCESS")
                .safeSummary(summary)
                .metadata(meta)
                .build();
    }

    public static ExecutionEvent failed(
            String executionId,
            String agentName,
            String toolName,
            String toolSource,
            long durationMs,
            String errorMessage) {

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("toolName", toolName != null ? toolName : "unknown_tool");
        meta.put("toolSource", toolSource != null ? toolSource : ToolCallEvent.resolveSource(toolName));
        meta.put("durationMs", durationMs);
        meta.put("status", "FAILED");

        String safeError = errorMessage != null ? errorMessage : "Tool execution failed";

        return ExecutionEvent.builder(executionId, EventType.TOOL_CALL_FAILED)
                .agentName(agentName)
                .component("TOOL")
                .durationMs(durationMs)
                .status("FAILED")
                .errorInfo(safeError)
                .safeSummary(String.format("Tool '%s' FAILED after %dms: %s",
                        toolName, durationMs, safeError))
                .metadata(meta)
                .build();
    }

    private static String truncate(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text : text.substring(0, max) + "...";
    }
}
