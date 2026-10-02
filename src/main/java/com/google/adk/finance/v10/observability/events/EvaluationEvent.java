package com.google.adk.finance.v10.observability.events;

import com.google.adk.finance.v10.observability.EventType;
import com.google.adk.finance.v10.observability.ExecutionEvent;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Event helper for V9 evaluation benchmark criteria.
 * Connects evaluation checks directly into the execution trace.
 */
public final class EvaluationEvent {

    public static ExecutionEvent started(String executionId, String agentName, String testCaseId) {
        return ExecutionEvent.builder(executionId, EventType.EVALUATION_STARTED)
                .agentName(agentName)
                .component("EVALUATOR")
                .status("STARTED")
                .safeSummary("Evaluation benchmark started for case: " + testCaseId)
                .metadata(Map.of("testCaseId", testCaseId != null ? testCaseId : "CASE-DEFAULT"))
                .build();
    }

    public static ExecutionEvent completed(
            String executionId,
            String agentName,
            String evaluationId,
            String criterion,
            String result,
            Optional<Double> score,
            String expectedSafe,
            String actualSafe,
            String explanation,
            long durationMs) {

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("evaluationId", evaluationId != null ? evaluationId : "eval-default");
        meta.put("criterion", criterion != null ? criterion : "UNKNOWN_CRITERION");
        meta.put("result", result != null ? result : "PASS");
        score.ifPresent(s -> meta.put("score", s));
        if (expectedSafe != null && !expectedSafe.isBlank()) meta.put("expected", expectedSafe);
        if (actualSafe != null && !actualSafe.isBlank()) meta.put("actual", actualSafe);
        if (explanation != null && !explanation.isBlank()) meta.put("explanation", explanation);
        meta.put("durationMs", durationMs);

        String scoreStr = score.map(s -> String.format(" (%.2f)", s)).orElse("");
        String summary = String.format("Evaluation [%s]: %s%s - %s",
                criterion, result, scoreStr, truncate(explanation, 80));

        EventType eventType = "FAIL".equalsIgnoreCase(result)
                ? EventType.EVALUATION_FAILED
                : EventType.EVALUATION_COMPLETED;

        return ExecutionEvent.builder(executionId, eventType)
                .agentName(agentName)
                .component("EVALUATOR")
                .durationMs(durationMs)
                .status(result)
                .safeSummary(summary)
                .metadata(meta)
                .build();
    }

    private static String truncate(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text : text.substring(0, max) + "...";
    }
}
