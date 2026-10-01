package com.google.adk.finance.v9.testing;

import java.util.Objects;
import java.util.Optional;

/**
 * Result model for a deliberate failure testing scenario.
 */
public record FailureTestResult(
        String scenarioId,
        FailureScenario.Category category,
        Status status,
        String expectedBehavior,
        String actualBehavior,
        boolean failureProperlyCaught,
        String explanation,
        Optional<String> interceptedBy
) {
    public enum Status {
        PASS,
        FAIL,
        ERROR
    }

    public FailureTestResult {
        Objects.requireNonNull(scenarioId, "scenarioId must not be null");
        Objects.requireNonNull(category, "category must not be null");
        Objects.requireNonNull(status, "status must not be null");
        expectedBehavior = expectedBehavior == null ? "" : expectedBehavior;
        actualBehavior = actualBehavior == null ? "" : actualBehavior;
        explanation = explanation == null ? "" : explanation;
        interceptedBy = interceptedBy == null ? Optional.empty() : interceptedBy;
    }

    public boolean isPass() {
        return status == Status.PASS;
    }

    public static FailureTestResult pass(String id, FailureScenario.Category cat, String expected, String actual, String interceptedBy, String explanation) {
        return new FailureTestResult(id, cat, Status.PASS, expected, actual, true, explanation, Optional.ofNullable(interceptedBy));
    }

    public static FailureTestResult fail(String id, FailureScenario.Category cat, String expected, String actual, String failureReason) {
        return new FailureTestResult(id, cat, Status.FAIL, expected, actual, false, failureReason, Optional.empty());
    }
}
