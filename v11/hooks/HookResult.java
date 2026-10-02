package com.google.adk.finance.v11.hooks;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable outcome record produced whenever a V11 lifecycle hook executes.
 */
public record HookResult(
        String hookName,
        HookPolicy policy,
        boolean success,
        boolean blocked,
        String message,
        Map<String, Object> metadata,
        long executionDurationMs,
        Instant timestamp
) {
    public HookResult {
        Objects.requireNonNull(hookName, "hookName must not be null");
        Objects.requireNonNull(policy, "policy must not be null");
        Objects.requireNonNull(message, "message must not be null");
        metadata = metadata != null ? Collections.unmodifiableMap(metadata) : Collections.emptyMap();
        timestamp = timestamp != null ? timestamp : Instant.now();
    }

    public static HookResult success(String hookName, HookPolicy policy, String message, Map<String, Object> metadata, long durationMs) {
        return new HookResult(hookName, policy, true, false, message, metadata, durationMs, Instant.now());
    }

    public static HookResult blocked(String hookName, String reason, Map<String, Object> metadata, long durationMs) {
        return new HookResult(hookName, HookPolicy.BLOCKING, false, true, reason, metadata, durationMs, Instant.now());
    }

    public static HookResult warning(String hookName, String message, Map<String, Object> metadata, long durationMs) {
        return new HookResult(hookName, HookPolicy.NON_BLOCKING, true, false, message, metadata, durationMs, Instant.now());
    }

    public static HookResult failure(String hookName, HookPolicy policy, String error, Map<String, Object> metadata, long durationMs) {
        return new HookResult(hookName, policy, false, policy == HookPolicy.BLOCKING, error, metadata, durationMs, Instant.now());
    }
}
