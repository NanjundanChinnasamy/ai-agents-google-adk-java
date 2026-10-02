package com.google.adk.finance.v10.metrics;

/**
 * Latency metrics capturing granular component timing breakdowns.
 */
public record LatencyMetrics(
        long totalExecutionMs,
        long modelLatencyMs,
        long toolLatencyMs,
        long evaluationLatencyMs,
        long guardrailLatencyMs
) {
    public static LatencyMetrics empty() {
        return new LatencyMetrics(0, 0, 0, 0, 0);
    }

    public double totalExecutionSeconds() {
        return totalExecutionMs / 1000.0;
    }

    public double modelLatencySeconds() {
        return modelLatencyMs / 1000.0;
    }

    public double toolLatencySeconds() {
        return toolLatencyMs / 1000.0;
    }

    public double evaluationLatencySeconds() {
        return evaluationLatencyMs / 1000.0;
    }
}
