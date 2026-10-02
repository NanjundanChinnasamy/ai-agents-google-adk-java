package com.google.adk.finance.v10.metrics;

/**
 * Token usage metrics model.
 * <p>
 * Rule: Never fabricate or hallucinate token counts. If the underlying framework or LLM provider
 * does not expose token usage on the wire, values strictly evaluate to "UNKNOWN".
 */
public record TokenMetrics(
        String inputTokens,
        String outputTokens,
        String totalTokens,
        boolean isAvailable
) {
    public static final String UNKNOWN = "UNKNOWN";

    public static TokenMetrics unavailable() {
        return new TokenMetrics(UNKNOWN, UNKNOWN, UNKNOWN, false);
    }

    public static TokenMetrics of(long input, long output) {
        long total = input + output;
        return new TokenMetrics(String.valueOf(input), String.valueOf(output), String.valueOf(total), true);
    }
}
