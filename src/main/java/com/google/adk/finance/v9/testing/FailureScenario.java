package com.google.adk.finance.v9.testing;

import java.util.Map;
import java.util.Objects;

/**
 * Defines a deliberate failure or adversarial injection scenario for testing agent resilience.
 */
public record FailureScenario(
        String scenarioId,
        Category category,
        String description,
        String testInput,
        String expectedProtectionBehavior,
        Map<String, Object> simulationContext
) {
    public enum Category {
        /** Sensitive personal or financial identifiers in user prompt */
        PII_INPUT,

        /** Prompt injection probe or system override attempt */
        PROMPT_INJECTION,

        /** Malformed, illegal, or injection-laden ticker symbol */
        INVALID_TICKER,

        /** Unauthorized transaction attempt (e.g. buy/sell/transfer) */
        UNAUTHORIZED_OPERATION,

        /** Upstream MCP or Google Search downtime, timeout, or malformed payload */
        TOOL_FAILURE,

        /** Model output claiming numbers not backed by evidence (hallucination) */
        HALLUCINATED_FACT,

        /** Model output misrepresenting portfolio arithmetic */
        CALCULATION_FAILURE,

        /** Incomplete scenario missing stress test or baseline */
        INCOMPLETE_SCENARIO,

        /** Toxic, offensive, or leaked PII in generated output */
        OUTPUT_SAFETY
    }

    public FailureScenario {
        Objects.requireNonNull(scenarioId, "scenarioId must not be null");
        Objects.requireNonNull(category, "category must not be null");
        description = description == null ? "" : description;
        testInput = testInput == null ? "" : testInput;
        expectedProtectionBehavior = expectedProtectionBehavior == null ? "" : expectedProtectionBehavior;
        simulationContext = simulationContext == null ? Map.of() : Map.copyOf(simulationContext);
    }
}
