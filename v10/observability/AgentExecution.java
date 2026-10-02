package com.google.adk.finance.v10.observability;

import com.google.adk.finance.v10.persistence.ExecutionRepository;
import com.google.adk.finance.v9.evaluation.EvaluationResult;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Result model representing an executed agent turn, connecting output to trace, metrics, and persistence.
 */
public record AgentExecution(
        String executionId,
        String userId,
        String sessionId,
        String prompt,
        String response,
        String status,
        ExecutionTrace trace,
        ExecutionRepository repository
) {
    public AgentExecution {
        Objects.requireNonNull(executionId, "executionId must not be null");
        userId = userId == null ? "anonymous" : userId;
        sessionId = sessionId == null ? "session-default" : sessionId;
        prompt = prompt == null ? "" : prompt;
        response = response == null ? "" : response;
        status = status == null ? "SUCCESS" : status;
        Objects.requireNonNull(trace, "trace must not be null");
    }

    public boolean isSuccess() {
        return "SUCCESS".equalsIgnoreCase(status);
    }

    public boolean isBlocked() {
        return "BLOCKED".equalsIgnoreCase(status);
    }

    public boolean isFailed() {
        return "FAILED".equalsIgnoreCase(status);
    }

    public ExecutionMetrics metrics() {
        return trace.metrics();
    }

    public List<ExecutionEvent> events() {
        return trace.events();
    }

    public List<EvaluationResult> evaluations() {
        return trace.evaluations();
    }

    public String renderTrace() {
        return trace.toFormattedTimeline();
    }

    public String finalResponse() {
        return response;
    }
}
