package com.google.adk.finance.v8.guardrails;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Result abstraction for all Finance Advisor V8 guardrails.
 * <p>
 * Supported actions:
 * <ul>
 *   <li><b>ALLOW</b>: The input, tool call, or model output passed validation and execution may continue.</li>
 *   <li><b>BLOCK</b>: A security, compliance, or safety violation occurred; execution must halt or return a safe fallback.</li>
 *   <li><b>SANITIZE</b>: Sensitive data (such as PII) was detected and redacted into safe tokens before downstream processing.</li>
 *   <li><b>WARN</b>: Non-fatal observation (such as an unverified metric or suspicious pattern) that permits execution but logs an alert.</li>
 * </ul>
 */
public record GuardrailResult(
        String guardrailName,
        Status status,
        String reason,
        Optional<String> sanitizedValue,
        Map<String, Object> metadata
) {
    public enum Status {
        ALLOW,
        BLOCK,
        SANITIZE,
        WARN
    }

    public GuardrailResult {
        Objects.requireNonNull(guardrailName, "guardrailName must not be null");
        Objects.requireNonNull(status, "status must not be null");
        reason = reason == null ? "" : reason;
        sanitizedValue = sanitizedValue == null ? Optional.empty() : sanitizedValue;
        metadata = metadata == null ? Collections.emptyMap() : Collections.unmodifiableMap(metadata);
    }

    public boolean isAllowed() {
        return status == Status.ALLOW || status == Status.SANITIZE || status == Status.WARN;
    }

    public boolean isBlocked() {
        return status == Status.BLOCK;
    }

    public boolean isSanitized() {
        return status == Status.SANITIZE;
    }

    public boolean isWarn() {
        return status == Status.WARN;
    }

    public static GuardrailResult allow(String guardrailName) {
        return new GuardrailResult(guardrailName, Status.ALLOW, "Passed validation", Optional.empty(), Map.of());
    }

    public static GuardrailResult allow(String guardrailName, String reason) {
        return new GuardrailResult(guardrailName, Status.ALLOW, reason, Optional.empty(), Map.of());
    }

    public static GuardrailResult allow(String guardrailName, String reason, Map<String, Object> metadata) {
        return new GuardrailResult(guardrailName, Status.ALLOW, reason, Optional.empty(), metadata);
    }

    public static GuardrailResult block(String guardrailName, String reason) {
        return new GuardrailResult(guardrailName, Status.BLOCK, reason, Optional.empty(), Map.of());
    }

    public static GuardrailResult block(String guardrailName, String reason, Map<String, Object> metadata) {
        return new GuardrailResult(guardrailName, Status.BLOCK, reason, Optional.empty(), metadata);
    }

    public static GuardrailResult sanitize(String guardrailName, String sanitizedValue, String reason) {
        return new GuardrailResult(guardrailName, Status.SANITIZE, reason, Optional.of(sanitizedValue), Map.of());
    }

    public static GuardrailResult sanitize(String guardrailName, String sanitizedValue, String reason, Map<String, Object> metadata) {
        return new GuardrailResult(guardrailName, Status.SANITIZE, reason, Optional.of(sanitizedValue), metadata);
    }

    public static GuardrailResult warn(String guardrailName, String reason) {
        return new GuardrailResult(guardrailName, Status.WARN, reason, Optional.empty(), Map.of());
    }
}
