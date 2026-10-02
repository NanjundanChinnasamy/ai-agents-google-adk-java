package com.google.adk.finance.v10.metrics;

/**
 * Metric summary for a single tool across an execution.
 */
public record ToolMetrics(
        String toolName,
        int invocationCount,
        int successCount,
        int failureCount,
        long totalDurationMs
) {
    public long averageDurationMs() {
        if (invocationCount == 0) return 0;
        return totalDurationMs / invocationCount;
    }

    public ToolMetrics recordInvocation(boolean success, long durationMs) {
        return new ToolMetrics(
                toolName,
                invocationCount + 1,
                success ? successCount + 1 : successCount,
                success ? failureCount : failureCount + 1,
                totalDurationMs + durationMs
        );
    }

    public static ToolMetrics initial(String toolName) {
        return new ToolMetrics(toolName, 0, 0, 0, 0);
    }
}
