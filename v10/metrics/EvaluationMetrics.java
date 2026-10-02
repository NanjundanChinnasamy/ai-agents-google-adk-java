package com.google.adk.finance.v10.metrics;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Metric summary of evaluation checks associated with an execution.
 */
public record EvaluationMetrics(
        int totalEvaluations,
        int passedEvaluations,
        int failedEvaluations,
        int warningEvaluations,
        Map<String, Double> scores
) {
    public EvaluationMetrics {
        scores = scores == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(scores));
    }

    public static EvaluationMetrics empty() {
        return new EvaluationMetrics(0, 0, 0, 0, Map.of());
    }

    public boolean hasFailures() {
        return failedEvaluations > 0;
    }

    public double passRate() {
        if (totalEvaluations == 0) return 1.0;
        return (double) passedEvaluations / totalEvaluations;
    }
}
