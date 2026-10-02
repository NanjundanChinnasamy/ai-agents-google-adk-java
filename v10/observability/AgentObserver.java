package com.google.adk.finance.v10.observability;

import java.util.Map;

/**
 * Observer interface decoupling telemetry and event collection from agent business logic.
 */
public interface AgentObserver {

    void onAgentStarted(String executionId, String userId, String sessionId, String prompt);

    void onAgentCompleted(String executionId, String status, String responseSummary, long durationMs);

    void onAgentFailed(String executionId, Throwable error, long durationMs);

    void onModelCallStarted(String executionId, String modelName);

    void onModelCallCompleted(String executionId, String modelName, long durationMs, String tokenUsage);

    void onModelCallFailed(String executionId, String modelName, Throwable error, long durationMs);

    void onToolCallStarted(String executionId, String toolName, String toolSource, Map<String, Object> safeArgs);

    void onToolCallCompleted(String executionId, String toolName, String toolSource, long durationMs, String safeSummary);

    void onToolCallFailed(String executionId, String toolName, String toolSource, Throwable error, long durationMs);

    void onGuardrailEvent(ExecutionEvent guardrailEvent);

    void onEvaluationCompleted(String executionId, String criterion, String result, Double score, String explanation, long durationMs);

    void onError(String executionId, String component, String errorMessage, Throwable error);
}
