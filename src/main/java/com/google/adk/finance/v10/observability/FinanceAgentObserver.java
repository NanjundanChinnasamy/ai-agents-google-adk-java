package com.google.adk.finance.v10.observability;

import com.google.adk.finance.v10.observability.events.AgentCompletedEvent;
import com.google.adk.finance.v10.observability.events.AgentStartedEvent;
import com.google.adk.finance.v10.observability.events.ErrorEvent;
import com.google.adk.finance.v10.observability.events.EvaluationEvent;
import com.google.adk.finance.v10.observability.events.ModelCallEvent;
import com.google.adk.finance.v10.observability.events.ToolCallEvent;
import com.google.adk.finance.v10.observability.events.ToolResultEvent;
import com.google.adk.finance.v10.persistence.ExecutionRecord;
import com.google.adk.finance.v10.persistence.ExecutionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Concrete observer collecting structured execution events and orchestrating safe persistence.
 * <p>
 * Key Architectural Principle (Section 16):
 * <b>Distinguishes Business/Agent Failure from Observability/Persistence Failure.</b>
 * If telemetry persistence fails, the agent's synthesized response is preserved and not marked as failed.
 */
public class FinanceAgentObserver implements AgentObserver {
    private static final Logger logger = LoggerFactory.getLogger(FinanceAgentObserver.class);

    private final ObservabilityContext context;
    private final ExecutionRepository repository;
    private ExecutionTrace lastTrace;
    private boolean persistenceSuccessful = true;

    public FinanceAgentObserver(ObservabilityContext context, ExecutionRepository repository) {
        this.context = Objects.requireNonNull(context, "context must not be null");
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    public ObservabilityContext getContext() {
        return context;
    }

    public ExecutionRepository getRepository() {
        return repository;
    }

    public Optional<ExecutionTrace> getLastTrace() {
        return Optional.ofNullable(lastTrace);
    }

    public boolean isPersistenceSuccessful() {
        return persistenceSuccessful;
    }

    @Override
    public void onAgentStarted(String executionId, String userId, String sessionId, String prompt) {
        String cleanPrompt = context.sanitize(prompt);
        ExecutionEvent event = AgentStartedEvent.create(
                executionId,
                context.agentName(),
                userId,
                sessionId,
                cleanPrompt
        );
        context.recordEvent(event);
        logger.info("[Observability] Agent started: executionId={}", executionId);
    }

    @Override
    public void onAgentCompleted(String executionId, String status, String responseSummary, long durationMs) {
        String cleanResponse = context.sanitize(responseSummary);
        ExecutionEvent event = AgentCompletedEvent.create(
                executionId,
                context.agentName(),
                durationMs,
                status,
                cleanResponse
        );
        context.recordEvent(event);

        // Build completed trace
        this.lastTrace = context.complete(status);
        logger.info("[Observability] Agent completed: executionId={}, status={}, duration={}ms",
                executionId, status, durationMs);

        // Persist execution
        persistSafely(cleanResponse, null);
    }

    @Override
    public void onAgentFailed(String executionId, Throwable error, long durationMs) {
        String safeErrorMsg = error != null ? error.getMessage() : "Unknown failure";
        ExecutionEvent errEvent = ErrorEvent.create(
                executionId,
                context.agentName(),
                "AGENT",
                safeErrorMsg,
                error != null ? error.getClass().getSimpleName() : "Exception",
                true
        );
        context.recordEvent(errEvent);

        ExecutionEvent completedEvent = AgentCompletedEvent.create(
                executionId,
                context.agentName(),
                durationMs,
                "FAILED",
                "Terminated due to error: " + safeErrorMsg
        );
        context.recordEvent(completedEvent);

        this.lastTrace = context.complete("FAILED");
        logger.error("[Observability] Agent execution failed: executionId={}, error={}", executionId, safeErrorMsg);

        persistSafely("", safeErrorMsg);
    }

    @Override
    public void onModelCallStarted(String executionId, String modelName) {
        context.startTimer("model_" + modelName);
        ExecutionEvent event = ModelCallEvent.started(executionId, context.agentName(), modelName);
        context.recordEvent(event);
    }

    @Override
    public void onModelCallCompleted(String executionId, String modelName, long durationMs, String tokenUsage) {
        context.recordModelLatency(durationMs);
        ExecutionEvent event = ModelCallEvent.completed(executionId, context.agentName(), modelName, durationMs, tokenUsage);
        context.recordEvent(event);
    }

    @Override
    public void onModelCallFailed(String executionId, String modelName, Throwable error, long durationMs) {
        context.recordModelLatency(durationMs);
        String safeError = error != null ? error.getMessage() : "Model call error";
        ExecutionEvent event = ModelCallEvent.failed(executionId, context.agentName(), modelName, durationMs, safeError);
        context.recordEvent(event);
    }

    @Override
    public void onToolCallStarted(String executionId, String toolName, String toolSource, Map<String, Object> safeArgs) {
        context.startTimer("tool_" + toolName);
        ExecutionEvent event = ToolCallEvent.started(executionId, context.agentName(), toolName, toolSource, safeArgs);
        context.recordEvent(event);
    }

    @Override
    public void onToolCallCompleted(String executionId, String toolName, String toolSource, long durationMs, String safeSummary) {
        context.recordToolExecution(toolName, true, durationMs);
        ExecutionEvent event = ToolResultEvent.completed(executionId, context.agentName(), toolName, toolSource, durationMs, safeSummary);
        context.recordEvent(event);
    }

    @Override
    public void onToolCallFailed(String executionId, String toolName, String toolSource, Throwable error, long durationMs) {
        context.recordToolExecution(toolName, false, durationMs);
        String safeError = error != null ? error.getMessage() : "Tool failure";
        ExecutionEvent event = ToolResultEvent.failed(executionId, context.agentName(), toolName, toolSource, durationMs, safeError);
        context.recordEvent(event);
    }

    @Override
    public void onGuardrailEvent(ExecutionEvent guardrailEvent) {
        context.recordEvent(guardrailEvent);
    }

    @Override
    public void onEvaluationCompleted(String executionId, String criterion, String result, Double score, String explanation, long durationMs) {
        context.recordEvaluationLatency(durationMs);
        ExecutionEvent event = EvaluationEvent.completed(
                executionId,
                context.agentName(),
                "eval-" + criterion.toLowerCase().replace(" ", "-"),
                criterion,
                result,
                Optional.ofNullable(score),
                "",
                "",
                explanation,
                durationMs
        );
        context.recordEvent(event);
    }

    @Override
    public void onError(String executionId, String component, String errorMessage, Throwable error) {
        ExecutionEvent event = ErrorEvent.create(
                executionId,
                context.agentName(),
                component,
                errorMessage,
                error != null ? error.getClass().getSimpleName() : "Error",
                false
        );
        context.recordEvent(event);
    }

    private void persistSafely(String response, String error) {
        if (lastTrace == null) return;
        try {
            ExecutionRecord record = ExecutionRecord.fromTrace(
                    lastTrace,
                    context.userId(),
                    context.sessionId(),
                    context.events().isEmpty() ? "" : context.events().get(0).safeSummary(),
                    response,
                    error
            );
            repository.save(record);
            this.persistenceSuccessful = true;
            logger.info("[Observability] Successfully persisted execution: {}", lastTrace.executionId());
        } catch (Throwable t) {
            this.persistenceSuccessful = false;
            logger.error("[Observability] Failed to persist execution '{}': {}", lastTrace.executionId(), t.getMessage());
            context.recordEvent(ErrorEvent.persistenceFailed(lastTrace.executionId(), context.agentName(), t.getMessage()));
        }
    }
}
