package com.google.adk.finance.v10.persistence;

import com.google.adk.finance.v10.observability.ExecutionEvent;
import com.google.adk.finance.v10.observability.ExecutionMetrics;
import com.google.adk.finance.v10.observability.ExecutionTrace;
import com.google.adk.finance.v9.evaluation.EvaluationResult;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable Data Transfer Record representing an agent execution persisted to disk or relational storage.
 * <p>
 * Contains complete execution history, audit timeline, performance metrics, and evaluation outcomes.
 */
public record ExecutionRecord(
        String executionId,
        String userId,
        String sessionId,
        String startedAt,
        String completedAt,
        String status,
        String agentName,
        String sanitizedPrompt,
        String sanitizedResponse,
        List<ExecutionEvent> events,
        ExecutionMetrics metrics,
        List<EvaluationResult> evaluations,
        Optional<String> error
) {
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_INSTANT;

    public ExecutionRecord {
        Objects.requireNonNull(executionId, "executionId must not be null");
        userId = userId == null ? "anonymous" : userId;
        sessionId = sessionId == null ? "session-default" : sessionId;
        startedAt = startedAt == null ? ISO.format(Instant.now()) : startedAt;
        completedAt = completedAt == null ? startedAt : completedAt;
        status = status == null ? "SUCCESS" : status;
        agentName = agentName == null ? "finance_advisor_v10" : agentName;
        sanitizedPrompt = sanitizedPrompt == null ? "" : sanitizedPrompt;
        sanitizedResponse = sanitizedResponse == null ? "" : sanitizedResponse;
        events = events == null ? List.of() : List.copyOf(events);
        evaluations = evaluations == null ? List.of() : List.copyOf(evaluations);
        error = error == null ? Optional.empty() : error;
    }

    public static ExecutionRecord fromTrace(
            ExecutionTrace trace,
            String userId,
            String sessionId,
            String sanitizedPrompt,
            String sanitizedResponse,
            String error) {

        return new ExecutionRecord(
                trace.executionId(),
                userId,
                sessionId,
                ISO.format(trace.startedAt()),
                ISO.format(trace.completedAt()),
                trace.status(),
                trace.agentName(),
                sanitizedPrompt,
                sanitizedResponse,
                trace.events(),
                trace.metrics(),
                trace.evaluations(),
                Optional.ofNullable(error)
        );
    }

    public boolean isSuccess() {
        return "SUCCESS".equalsIgnoreCase(status);
    }

    public boolean isFailed() {
        return "FAILED".equalsIgnoreCase(status);
    }

    public boolean hasEvaluationFailures() {
        return evaluations.stream().anyMatch(EvaluationResult::isFail);
    }

    public boolean hasGuardrailEvents() {
        return events.stream().anyMatch(e -> e.eventType().isGuardrailEvent());
    }
}
