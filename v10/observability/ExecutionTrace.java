package com.google.adk.finance.v10.observability;

import com.google.adk.finance.v9.evaluation.EvaluationResult;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable execution trace collecting the complete chronological narrative of an agent run.
 * Provides the canonical human-readable ASCII trace view.
 */
public record ExecutionTrace(
        String executionId,
        Instant startedAt,
        Instant completedAt,
        String status,
        String agentName,
        List<ExecutionEvent> events,
        ExecutionMetrics metrics,
        List<EvaluationResult> evaluations
) {
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_INSTANT;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
            .withZone(ZoneId.of("UTC"));

    public ExecutionTrace {
        Objects.requireNonNull(executionId, "executionId must not be null");
        startedAt = startedAt == null ? Instant.now() : startedAt;
        completedAt = completedAt == null ? startedAt : completedAt;
        status = status == null ? "SUCCESS" : status;
        agentName = agentName == null ? "finance_advisor_v10" : agentName;
        events = events == null ? List.of() : List.copyOf(events);
        metrics = metrics == null ? ExecutionMetrics.builder().build() : metrics;
        evaluations = evaluations == null ? List.of() : List.copyOf(evaluations);
    }

    public boolean isSuccess() {
        return "SUCCESS".equalsIgnoreCase(status);
    }

    public boolean isFailed() {
        return "FAILED".equalsIgnoreCase(status);
    }

    public boolean isBlocked() {
        return "BLOCKED".equalsIgnoreCase(status);
    }

    public boolean hasGuardrailEvents() {
        return events.stream().anyMatch(e -> e.eventType().isGuardrailEvent());
    }

    public boolean hasEvaluationFailures() {
        return evaluations.stream().anyMatch(EvaluationResult::isFail);
    }

    public boolean hasErrors() {
        return events.stream().anyMatch(e -> e.eventType().isFailure());
    }

    /**
     * Formats the trace into the standard human-readable ASCII layout.
     */
    public String toFormattedTimeline() {
        StringBuilder sb = new StringBuilder();
        sb.append("==================================================\n");
        sb.append("FINANCE ADVISOR V10 TRACE\n");
        sb.append("==================================================\n\n");

        sb.append("Execution ID:\n").append(executionId).append("\n\n");
        sb.append("Started:\n").append(ISO_FORMATTER.format(startedAt)).append("\n\n");
        sb.append("Status:\n").append(status).append("\n\n");

        sb.append("--------------------------------------------------\n");
        sb.append("EVENT TIMELINE\n");
        sb.append("--------------------------------------------------\n");
        if (events.isEmpty()) {
            sb.append("(No recorded events)\n");
        } else {
            for (ExecutionEvent event : events) {
                String timeStr = TIME_FORMATTER.format(event.timestamp());
                sb.append(String.format("%-12s %s\n", timeStr, event.eventType().name()));
            }
        }
        sb.append("\n");

        sb.append("--------------------------------------------------\n");
        sb.append("METRICS\n");
        sb.append("--------------------------------------------------\n");
        double seconds = metrics.totalExecutionDurationMs() / 1000.0;
        sb.append(String.format("Total duration: %.2fs\n", seconds));
        sb.append("Model calls: ").append(metrics.modelCallCount()).append("\n");
        sb.append("Tool calls: ").append(metrics.toolCallCount()).append("\n");
        sb.append("Guardrail events: ").append(metrics.guardrailEventCount()).append("\n");
        sb.append("Evaluation failures: ").append(metrics.evaluationFailureCount()).append("\n");

        if (!metrics.toolMetricsMap().isEmpty()) {
            sb.append("\nTool Latency & Invocation Breakdown:\n");
            metrics.toolMetricsMap().values().forEach(tm -> {
                sb.append(String.format("- %s: %d calls, %d success, %d failure, avg latency %dms\n",
                        tm.toolName(), tm.invocationCount(), tm.successCount(), tm.failureCount(), tm.averageDurationMs()));
            });
        }
        sb.append("\n");

        if (!evaluations.isEmpty()) {
            sb.append("--------------------------------------------------\n");
            sb.append("EVALUATION\n");
            sb.append("--------------------------------------------------\n");
            for (EvaluationResult eval : evaluations) {
                sb.append(String.format("%s: %s\n", eval.criterion().displayName(), eval.status().name()));
            }
            sb.append("\n");
        }

        sb.append("==================================================");
        return sb.toString();
    }
}
