package com.google.adk.finance.v9.evaluation;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Standardized, structured evaluation result for all V9 evaluation checks.
 * <p>
 * Does not hide diagnostic failures behind a single arbitrary aggregate score.
 * Provides granular expected vs actual comparisons, evidence references, and actionable explanations.
 */
public record EvaluationResult(
        String testCaseId,
        EvaluationCriteria criterion,
        Status status,
        Optional<Double> score,
        String expected,
        String actual,
        String explanation,
        List<String> evidenceReferences,
        Optional<String> failureReason,
        Map<String, Object> metadata
) {
    public enum Status {
        PASS,
        FAIL,
        WARN,
        ERROR
    }

    public EvaluationResult {
        testCaseId = testCaseId == null ? "CASE-DEFAULT" : testCaseId;
        Objects.requireNonNull(criterion, "criterion must not be null");
        Objects.requireNonNull(status, "status must not be null");
        score = score == null ? Optional.empty() : score;
        expected = expected == null ? "" : expected;
        actual = actual == null ? "" : actual;
        explanation = explanation == null ? "" : explanation;
        evidenceReferences = evidenceReferences == null ? List.of() : List.copyOf(evidenceReferences);
        failureReason = failureReason == null ? Optional.empty() : failureReason;
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public boolean isPass() {
        return status == Status.PASS;
    }

    public boolean isFail() {
        return status == Status.FAIL;
    }

    public static EvaluationResult pass(String testCaseId, EvaluationCriteria criterion, String explanation) {
        return new EvaluationResult(
                testCaseId,
                criterion,
                Status.PASS,
                Optional.of(1.0),
                "Criteria satisfied",
                "Verified compliant",
                explanation,
                List.of(),
                Optional.empty(),
                Map.of()
        );
    }

    public static EvaluationResult fail(String testCaseId, EvaluationCriteria criterion, String expected, String actual, String failureReason) {
        return new EvaluationResult(
                testCaseId,
                criterion,
                Status.FAIL,
                Optional.of(0.0),
                expected,
                actual,
                failureReason,
                List.of(),
                Optional.of(failureReason),
                Map.of()
        );
    }

    public static EvaluationResult warn(String testCaseId, EvaluationCriteria criterion, String explanation) {
        return new EvaluationResult(
                testCaseId,
                criterion,
                Status.WARN,
                Optional.of(0.5),
                "Acceptable with warnings",
                "Non-critical variance",
                explanation,
                List.of(),
                Optional.empty(),
                Map.of()
        );
    }

    public static EvaluationResult error(String testCaseId, EvaluationCriteria criterion, String errorMessage) {
        return new EvaluationResult(
                testCaseId,
                criterion,
                Status.ERROR,
                Optional.of(0.0),
                "Successful execution",
                "Execution error: " + errorMessage,
                errorMessage,
                List.of(),
                Optional.of(errorMessage),
                Map.of()
        );
    }
}
