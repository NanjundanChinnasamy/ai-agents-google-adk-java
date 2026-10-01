package com.google.adk.finance.v8.guardrails.input;

import com.google.adk.finance.v8.guardrails.GuardrailResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Deterministic PII sanitization engine that redacts sensitive personal and financial identifiers
 * into privacy-preserving placeholder tokens before external tool or LLM ingestion.
 * <p>
 * Example:
 * <pre>
 * Input:  "My name is John Smith and my account number is 1234567890. What is the latest Infosys news?"
 * Output: "My name is [REDACTED_NAME] and my account number is [REDACTED_ACCOUNT]. What is the latest Infosys news?"
 * </pre>
 * <p>
 * <i>Security Invariant: The raw unredacted text is never retained in telemetry or audit logs.</i>
 */
public class PiiSanitizer {
    private static final Logger logger = LoggerFactory.getLogger(PiiSanitizer.class);

    private final PiiDetector detector;

    public PiiSanitizer() {
        this(new PiiDetector());
    }

    public PiiSanitizer(PiiDetector detector) {
        this.detector = Objects.requireNonNull(detector, "detector must not be null");
    }

    /**
     * Sanitizes the input string by replacing all detected PII occurrences with standard redaction tokens.
     *
     * @param input raw text
     * @return sanitized string with all sensitive identifiers redacted
     */
    public String sanitize(String input) {
        if (input == null || input.isBlank()) {
            return input;
        }

        List<PiiDetector.PiiMatch> matches = detector.detectMatches(input);
        if (matches.isEmpty()) {
            return input;
        }

        // Sort matches in descending order of start position so replacements do not alter preceding indices
        List<PiiDetector.PiiMatch> sorted = matches.stream()
                .sorted(Comparator.comparingInt(PiiDetector.PiiMatch::start).reversed())
                .toList();

        StringBuilder sb = new StringBuilder(input);
        for (PiiDetector.PiiMatch match : sorted) {
            // Protect against overlapping match bounds
            if (match.start() >= 0 && match.end() <= sb.length() && match.start() <= match.end()) {
                // If it's a name introduction like "My name is John Smith", replace only the captured name portion if desired,
                // or if match covers "My name is John Smith", check regex group
                sb.replace(match.start(), match.end(), resolveReplacement(input, match));
            }
        }

        logger.info("[PII Sanitizer] Redacted {} PII occurrence(s) successfully.", matches.size());
        return sb.toString();
    }

    /**
     * Evaluates and sanitizes the input, returning a structured {@link GuardrailResult}.
     *
     * @param input the raw text
     * @return {@link GuardrailResult#allow} if clean, or {@link GuardrailResult#sanitize} if redacted
     */
    public GuardrailResult evaluateAndSanitize(String input) {
        if (input == null || input.isBlank()) {
            return GuardrailResult.allow("PiiSanitizer", "Empty or blank input.");
        }

        List<PiiDetector.PiiMatch> matches = detector.detectMatches(input);
        if (matches.isEmpty()) {
            return GuardrailResult.allow("PiiSanitizer", "No PII found; input allowed as-is.");
        }

        String sanitizedText = sanitize(input);
        return GuardrailResult.sanitize(
                "PiiSanitizer",
                sanitizedText,
                "Sanitized " + matches.size() + " sensitive identifier(s).",
                Map.of(
                        "redactedCount", matches.size(),
                        "categories", matches.stream().map(PiiDetector.PiiMatch::type).distinct().toList()
                )
        );
    }

    private String resolveReplacement(String original, PiiDetector.PiiMatch match) {
        if ("NAME".equals(match.type())) {
            // If the matched string starts with "my name is " or "i am ", keep the greeting and replace the name
            String matchedSegment = original.substring(match.start(), match.end());
            if (matchedSegment.toLowerCase().startsWith("my name is ")) {
                return "My name is " + match.redactionToken();
            } else if (matchedSegment.toLowerCase().startsWith("i am ")) {
                return "I am " + match.redactionToken();
            }
        }
        if ("BANK_ACCOUNT".equals(match.type())) {
            String matchedSegment = original.substring(match.start(), match.end());
            // If it matched prefix "account number is 1234567890", keep prefix and replace digits
            if (matchedSegment.toLowerCase().contains("account") || matchedSegment.toLowerCase().contains("a/c")) {
                String replaced = matchedSegment.replaceAll("[0-9][0-9\\-\\s]{6,20}[0-9]", match.redactionToken());
                if (!replaced.equals(matchedSegment)) {
                    return replaced;
                }
            }
            return match.redactionToken();
        }
        if ("CUSTOMER_ID".equals(match.type())) {
            String matchedSegment = original.substring(match.start(), match.end());
            String replaced = matchedSegment.replaceAll("[0-9]{4,10}", match.redactionToken());
            if (!replaced.equals(matchedSegment)) {
                return replaced;
            }
            return match.redactionToken();
        }
        return match.redactionToken();
    }
}
