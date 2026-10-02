package com.google.adk.finance.v10.observability;

import com.google.adk.finance.v10.metrics.EvaluationMetrics;
import com.google.adk.finance.v10.metrics.LatencyMetrics;
import com.google.adk.finance.v10.metrics.TokenMetrics;
import com.google.adk.finance.v10.metrics.ToolMetrics;
import com.google.adk.finance.v8.guardrails.input.PiiSanitizer;
import com.google.adk.finance.v9.evaluation.EvaluationResult;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe execution context for collecting structured events, timing latencies,
 * and aggregating metrics across a single Finance Advisor invocation.
 */
public class ObservabilityContext {
    private static final DateTimeFormatter DATE_PREFIX_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final String executionId;
    private final String userId;
    private final String sessionId;
    private final String agentName;
    private final Instant startedAt;
    private Instant completedAt;
    private String status = "IN_PROGRESS";

    private final List<ExecutionEvent> events = Collections.synchronizedList(new ArrayList<>());
    private final List<EvaluationResult> evaluations = Collections.synchronizedList(new ArrayList<>());
    private final Map<String, Instant> timerStarts = new ConcurrentHashMap<>();
    private final Map<String, ToolMetrics> toolMetrics = new ConcurrentHashMap<>();

    private long totalModelLatencyMs = 0;
    private long totalToolLatencyMs = 0;
    private long totalEvaluationLatencyMs = 0;
    private long totalGuardrailLatencyMs = 0;
    private int modelCallCount = 0;
    private int toolCallCount = 0;
    private int guardrailEventCount = 0;

    private final PiiSanitizer piiSanitizer;

    public ObservabilityContext(String userId, String sessionId, String agentName) {
        this(generateExecutionId(), userId, sessionId, agentName, new PiiSanitizer());
    }

    public ObservabilityContext(String executionId, String userId, String sessionId, String agentName) {
        this(executionId, userId, sessionId, agentName, new PiiSanitizer());
    }

    public ObservabilityContext(String executionId, String userId, String sessionId, String agentName, PiiSanitizer piiSanitizer) {
        this.executionId = Objects.requireNonNull(executionId, "executionId must not be null");
        this.userId = userId != null ? userId : "anonymous";
        this.sessionId = sessionId != null ? sessionId : "session-" + UUID.randomUUID().toString().substring(0, 8);
        this.agentName = agentName != null ? agentName : "finance_advisor_v10";
        this.startedAt = Instant.now();
        this.piiSanitizer = Objects.requireNonNull(piiSanitizer, "piiSanitizer must not be null");
    }

    /**
     * Generates a collision-safe, date-prefixed execution ID.
     * Example: "exec-20261001-a1b2c3d4"
     */
    public static String generateExecutionId() {
        String datePart = LocalDate.now().format(DATE_PREFIX_FORMATTER);
        String uuidPart = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return "exec-" + datePart + "-" + uuidPart;
    }

    public String executionId() {
        return executionId;
    }

    public String userId() {
        return userId;
    }

    public String sessionId() {
        return sessionId;
    }

    public String agentName() {
        return agentName;
    }

    public Instant startedAt() {
        return startedAt;
    }

    public Instant completedAt() {
        return completedAt != null ? completedAt : Instant.now();
    }

    public String status() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String sanitize(String input) {
        return piiSanitizer.sanitize(input);
    }

    public void recordEvent(ExecutionEvent event) {
        if (event == null) return;
        events.add(event);

        if (event.eventType().isGuardrailEvent()) {
            guardrailEventCount++;
        } else if (event.eventType().isModelEvent()) {
            if (event.eventType() == EventType.MODEL_CALL_STARTED) {
                modelCallCount++;
            }
        } else if (event.eventType().isToolEvent()) {
            if (event.eventType() == EventType.TOOL_CALL_STARTED) {
                toolCallCount++;
            }
        }
    }

    public void startTimer(String key) {
        timerStarts.put(key, Instant.now());
    }

    public long stopTimer(String key) {
        Instant start = timerStarts.remove(key);
        if (start == null) return 0;
        return Math.max(0, java.time.Duration.between(start, Instant.now()).toMillis());
    }

    public void recordModelLatency(long latencyMs) {
        totalModelLatencyMs += latencyMs;
    }

    public void recordToolExecution(String toolName, boolean success, long durationMs) {
        totalToolLatencyMs += durationMs;
        toolMetrics.compute(toolName, (k, v) -> {
            ToolMetrics current = v != null ? v : ToolMetrics.initial(toolName);
            return current.recordInvocation(success, durationMs);
        });
    }

    public void recordEvaluationLatency(long latencyMs) {
        totalEvaluationLatencyMs += latencyMs;
    }

    public void recordGuardrailLatency(long latencyMs) {
        totalGuardrailLatencyMs += latencyMs;
    }

    public void addEvaluationResult(EvaluationResult result) {
        if (result != null) {
            evaluations.add(result);
        }
    }

    public List<ExecutionEvent> events() {
        synchronized (events) {
            return new ArrayList<>(events);
        }
    }

    public List<EvaluationResult> evaluations() {
        synchronized (evaluations) {
            return new ArrayList<>(evaluations);
        }
    }

    public ExecutionTrace complete(String finalStatus) {
        this.completedAt = Instant.now();
        this.status = finalStatus != null ? finalStatus : "SUCCESS";

        long totalDurationMs = Math.max(0, java.time.Duration.between(startedAt, completedAt).toMillis());

        // Count evaluation failures
        int evalFailures = (int) evaluations.stream().filter(EvaluationResult::isFail).count();
        int evalPasses = (int) evaluations.stream().filter(EvaluationResult::isPass).count();
        int evalWarns = (int) evaluations.stream().filter(e -> e.status() == EvaluationResult.Status.WARN).count();

        Map<String, Double> scores = new LinkedHashMap<>();
        evaluations.forEach(e -> e.score().ifPresent(s -> scores.put(e.criterion().displayName(), s)));

        EvaluationMetrics evalMetrics = new EvaluationMetrics(
                evaluations.size(),
                evalPasses,
                evalFailures,
                evalWarns,
                scores
        );

        LatencyMetrics latencyMetrics = new LatencyMetrics(
                totalDurationMs,
                totalModelLatencyMs,
                totalToolLatencyMs,
                totalEvaluationLatencyMs,
                totalGuardrailLatencyMs
        );

        ExecutionMetrics metrics = ExecutionMetrics.builder()
                .totalExecutionDurationMs(totalDurationMs)
                .overallStatus(status)
                .modelCallCount(modelCallCount)
                .toolCallCount(toolCallCount)
                .guardrailEventCount(guardrailEventCount)
                .evaluationFailureCount(evalFailures)
                .tokenMetrics(TokenMetrics.unavailable())
                .latencyMetrics(latencyMetrics)
                .evaluationMetrics(evalMetrics)
                .build();

        // Add tool metrics
        ExecutionMetrics.Builder metricsBuilder = ExecutionMetrics.builder()
                .totalExecutionDurationMs(totalDurationMs)
                .overallStatus(status)
                .modelCallCount(modelCallCount)
                .toolCallCount(toolCallCount)
                .guardrailEventCount(guardrailEventCount)
                .evaluationFailureCount(evalFailures)
                .tokenMetrics(TokenMetrics.unavailable())
                .latencyMetrics(latencyMetrics)
                .evaluationMetrics(evalMetrics);
        toolMetrics.values().forEach(metricsBuilder::addToolMetric);

        return new ExecutionTrace(
                executionId,
                startedAt,
                completedAt,
                status,
                agentName,
                events(),
                metricsBuilder.build(),
                evaluations()
        );
    }
}
