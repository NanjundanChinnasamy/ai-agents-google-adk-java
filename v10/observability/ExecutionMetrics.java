package com.google.adk.finance.v10.observability;

import com.google.adk.finance.v10.metrics.EvaluationMetrics;
import com.google.adk.finance.v10.metrics.LatencyMetrics;
import com.google.adk.finance.v10.metrics.TokenMetrics;
import com.google.adk.finance.v10.metrics.ToolMetrics;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Comprehensive aggregated metrics for an individual agent execution run.
 */
public record ExecutionMetrics(
        long totalExecutionDurationMs,
        String overallStatus,
        int modelCallCount,
        int toolCallCount,
        int guardrailEventCount,
        int evaluationFailureCount,
        TokenMetrics tokenMetrics,
        LatencyMetrics latencyMetrics,
        Map<String, ToolMetrics> toolMetricsMap,
        EvaluationMetrics evaluationMetrics
) {
    public ExecutionMetrics {
        overallStatus = overallStatus == null ? "SUCCESS" : overallStatus;
        tokenMetrics = tokenMetrics == null ? TokenMetrics.unavailable() : tokenMetrics;
        latencyMetrics = latencyMetrics == null ? LatencyMetrics.empty() : latencyMetrics;
        toolMetricsMap = toolMetricsMap == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(toolMetricsMap));
        evaluationMetrics = evaluationMetrics == null ? EvaluationMetrics.empty() : evaluationMetrics;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private long totalExecutionDurationMs;
        private String overallStatus = "SUCCESS";
        private int modelCallCount;
        private int toolCallCount;
        private int guardrailEventCount;
        private int evaluationFailureCount;
        private TokenMetrics tokenMetrics = TokenMetrics.unavailable();
        private LatencyMetrics latencyMetrics = LatencyMetrics.empty();
        private final Map<String, ToolMetrics> toolMetricsMap = new LinkedHashMap<>();
        private EvaluationMetrics evaluationMetrics = EvaluationMetrics.empty();

        public Builder totalExecutionDurationMs(long ms) {
            this.totalExecutionDurationMs = ms;
            return this;
        }

        public Builder overallStatus(String status) {
            this.overallStatus = status;
            return this;
        }

        public Builder modelCallCount(int count) {
            this.modelCallCount = count;
            return this;
        }

        public Builder toolCallCount(int count) {
            this.toolCallCount = count;
            return this;
        }

        public Builder guardrailEventCount(int count) {
            this.guardrailEventCount = count;
            return this;
        }

        public Builder evaluationFailureCount(int count) {
            this.evaluationFailureCount = count;
            return this;
        }

        public Builder tokenMetrics(TokenMetrics tokenMetrics) {
            this.tokenMetrics = tokenMetrics;
            return this;
        }

        public Builder latencyMetrics(LatencyMetrics latencyMetrics) {
            this.latencyMetrics = latencyMetrics;
            return this;
        }

        public Builder addToolMetric(ToolMetrics toolMetric) {
            if (toolMetric != null) {
                this.toolMetricsMap.put(toolMetric.toolName(), toolMetric);
            }
            return this;
        }

        public Builder evaluationMetrics(EvaluationMetrics evaluationMetrics) {
            this.evaluationMetrics = evaluationMetrics;
            return this;
        }

        public ExecutionMetrics build() {
            return new ExecutionMetrics(
                    totalExecutionDurationMs,
                    overallStatus,
                    modelCallCount,
                    toolCallCount,
                    guardrailEventCount,
                    evaluationFailureCount,
                    tokenMetrics,
                    latencyMetrics,
                    toolMetricsMap,
                    evaluationMetrics
            );
        }
    }
}
