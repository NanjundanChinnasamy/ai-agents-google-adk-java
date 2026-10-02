package com.google.adk.finance.v10.observability.events;

import com.google.adk.finance.v10.observability.EventType;
import com.google.adk.finance.v10.observability.ExecutionEvent;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Event helper for safety guardrail interventions.
 * <p>
 * Security Invariant: Strictly records the policy rule and action taken (e.g. piiType="BANK_ACCOUNT", action="SANITIZED"),
 * and NEVER stores the underlying raw sensitive value or malicious exploit payload.
 */
public final class GuardrailEvent {

    public static ExecutionEvent piiDetected(
            String executionId,
            String agentName,
            String piiType,
            String actionTaken) {

        Objects.requireNonNull(piiType, "piiType must not be null");
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("guardrail", "PiiDetector");
        meta.put("piiType", piiType);
        meta.put("action", actionTaken != null ? actionTaken : "DETECTED");

        return ExecutionEvent.builder(executionId, EventType.PII_DETECTED)
                .agentName(agentName)
                .component("GUARDRAIL")
                .status("TRIGGERED")
                .safeSummary(String.format("PII detected (type: %s, action: %s)", piiType, actionTaken))
                .metadata(meta)
                .build();
    }

    public static ExecutionEvent piiSanitized(
            String executionId,
            String agentName,
            int redactionCount) {

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("guardrail", "PiiSanitizer");
        meta.put("redactionCount", redactionCount);
        meta.put("action", "SANITIZED");

        return ExecutionEvent.builder(executionId, EventType.PII_SANITIZED)
                .agentName(agentName)
                .component("GUARDRAIL")
                .status("SUCCESS")
                .safeSummary(String.format("Sanitized %d sensitive identifier(s) with privacy tokens", redactionCount))
                .metadata(meta)
                .build();
    }

    public static ExecutionEvent promptInjectionDetected(
            String executionId,
            String agentName,
            String probeCategory,
            double riskScore) {

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("guardrail", "PromptInjectionDetector");
        meta.put("probeCategory", probeCategory != null ? probeCategory : "SUSPICIOUS_OVERRIDE");
        meta.put("riskScore", riskScore);
        meta.put("action", "BLOCKED");

        return ExecutionEvent.builder(executionId, EventType.PROMPT_INJECTION_DETECTED)
                .agentName(agentName)
                .component("GUARDRAIL")
                .status("BLOCKED")
                .safeSummary(String.format("Prompt injection attempt intercepted [risk=%.2f, type=%s]",
                        riskScore, probeCategory))
                .metadata(meta)
                .build();
    }

    public static ExecutionEvent toolOperationBlocked(
            String executionId,
            String agentName,
            String toolName,
            String reason) {

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("guardrail", "ToolOperationGuard");
        meta.put("toolName", toolName != null ? toolName : "unknown");
        meta.put("reason", reason);
        meta.put("action", "BLOCKED");

        return ExecutionEvent.builder(executionId, EventType.TOOL_OPERATION_BLOCKED)
                .agentName(agentName)
                .component("GUARDRAIL")
                .status("BLOCKED")
                .safeSummary(String.format("Blocked unauthorized tool operation '%s': %s", toolName, reason))
                .metadata(meta)
                .build();
    }

    public static ExecutionEvent invalidTicker(
            String executionId,
            String agentName,
            String reason) {

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("guardrail", "TickerValidator");
        meta.put("reason", reason);
        meta.put("action", "BLOCKED");

        return ExecutionEvent.builder(executionId, EventType.INVALID_TICKER)
                .agentName(agentName)
                .component("GUARDRAIL")
                .status("BLOCKED")
                .safeSummary(String.format("Invalid/unsafe ticker symbol rejected: %s", reason))
                .metadata(meta)
                .build();
    }

    public static ExecutionEvent offensiveLanguageDetected(
            String executionId,
            String agentName,
            String category) {

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("guardrail", "OffensiveLanguageDetector");
        meta.put("category", category != null ? category : "TOXICITY");
        meta.put("action", "REPLACED_WITH_SAFE_FALLBACK");

        return ExecutionEvent.builder(executionId, EventType.OFFENSIVE_LANGUAGE_DETECTED)
                .agentName(agentName)
                .component("GUARDRAIL")
                .status("BLOCKED")
                .safeSummary("Offensive language detected and replaced with institutional fallback")
                .metadata(meta)
                .build();
    }

    public static ExecutionEvent disclaimerEnforced(
            String executionId,
            String agentName,
            boolean wasAlreadyPresent) {

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("guardrail", "ComplianceDisclaimerGuard");
        meta.put("alreadyPresent", wasAlreadyPresent);
        meta.put("action", wasAlreadyPresent ? "VERIFIED" : "APPENDED");

        return ExecutionEvent.builder(executionId, EventType.DISCLAIMER_ENFORCED)
                .agentName(agentName)
                .component("GUARDRAIL")
                .status("SUCCESS")
                .safeSummary(wasAlreadyPresent
                        ? "Institutional compliance disclaimer verified"
                        : "Mandatory regulatory compliance disclaimer appended")
                .metadata(meta)
                .build();
    }
}
