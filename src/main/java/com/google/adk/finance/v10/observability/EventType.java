package com.google.adk.finance.v10.observability;

/**
 * Standardized lifecycle event types for Finance Advisor Version 10 observability.
 * <p>
 * Categorizes all interactions across agent lifecycle, model execution, tool operations,
 * safety guardrails, evaluations, and telemetry persistence.
 */
public enum EventType {
    // Agent Lifecycle Events
    AGENT_STARTED("AGENT", "Agent invocation initiated by user request"),
    AGENT_COMPLETED("AGENT", "Agent invocation successfully completed"),
    AGENT_FAILED("AGENT", "Agent invocation terminated due to unhandled error"),

    // Model Lifecycle Events
    MODEL_CALL_STARTED("MODEL", "Outbound model generation request initiated"),
    MODEL_CALL_COMPLETED("MODEL", "Model generation response received"),
    MODEL_CALL_FAILED("MODEL", "Model generation failed or timed out"),

    // Tool Lifecycle Events
    TOOL_CALL_STARTED("TOOL", "Tool execution started"),
    TOOL_CALL_COMPLETED("TOOL", "Tool execution finished successfully"),
    TOOL_CALL_FAILED("TOOL", "Tool execution failed or threw an exception"),

    // Guardrail Lifecycle Events (V8-style)
    PII_DETECTED("GUARDRAIL", "Sensitive personal or financial identifier detected"),
    PII_SANITIZED("GUARDRAIL", "Sensitive identifier replaced with privacy token"),
    PROMPT_INJECTION_DETECTED("GUARDRAIL", "Instruction override or probe attempt blocked"),
    TOOL_OPERATION_BLOCKED("GUARDRAIL", "Unauthorized tool action (trade/transfer) blocked"),
    INVALID_TICKER("GUARDRAIL", "Malformed or unsafe ticker symbol blocked"),
    OFFENSIVE_LANGUAGE_DETECTED("GUARDRAIL", "Inappropriate language replaced with safe fallback"),
    OUTPUT_BLOCKED("GUARDRAIL", "Model output blocked due to safety policy violation"),
    DISCLAIMER_ENFORCED("GUARDRAIL", "Mandatory institutional disclaimer appended/verified"),

    // Evaluation Lifecycle Events (V9-style)
    EVALUATION_STARTED("EVALUATION", "Evaluation suite or criterion run started"),
    EVALUATION_COMPLETED("EVALUATION", "Evaluation criterion completed with PASS/FAIL/WARN"),
    EVALUATION_FAILED("EVALUATION", "Evaluation execution failed with an unexpected error"),

    // Persistence Lifecycle Events
    PERSISTENCE_STARTED("PERSISTENCE", "Telemetry record persistence initiated"),
    PERSISTENCE_COMPLETED("PERSISTENCE", "Telemetry record successfully saved to repository"),
    PERSISTENCE_FAILED("PERSISTENCE", "Telemetry persistence failed (non-blocking for agent)"),

    // Generic System Error
    ERROR("SYSTEM", "Generic system or infrastructure error encountered");

    private final String defaultComponent;
    private final String description;

    EventType(String defaultComponent, String description) {
        this.defaultComponent = defaultComponent;
        this.description = description;
    }

    public String defaultComponent() {
        return defaultComponent;
    }

    public String description() {
        return description;
    }

    public boolean isAgentEvent() {
        return this == AGENT_STARTED || this == AGENT_COMPLETED || this == AGENT_FAILED;
    }

    public boolean isModelEvent() {
        return this == MODEL_CALL_STARTED || this == MODEL_CALL_COMPLETED || this == MODEL_CALL_FAILED;
    }

    public boolean isToolEvent() {
        return this == TOOL_CALL_STARTED || this == TOOL_CALL_COMPLETED || this == TOOL_CALL_FAILED;
    }

    public boolean isGuardrailEvent() {
        return this == PII_DETECTED || this == PII_SANITIZED || this == PROMPT_INJECTION_DETECTED
                || this == TOOL_OPERATION_BLOCKED || this == INVALID_TICKER || this == OFFENSIVE_LANGUAGE_DETECTED
                || this == OUTPUT_BLOCKED || this == DISCLAIMER_ENFORCED;
    }

    public boolean isEvaluationEvent() {
        return this == EVALUATION_STARTED || this == EVALUATION_COMPLETED || this == EVALUATION_FAILED;
    }

    public boolean isPersistenceEvent() {
        return this == PERSISTENCE_STARTED || this == PERSISTENCE_COMPLETED || this == PERSISTENCE_FAILED;
    }

    public boolean isFailure() {
        return this == AGENT_FAILED || this == MODEL_CALL_FAILED || this == TOOL_CALL_FAILED
                || this == EVALUATION_FAILED || this == PERSISTENCE_FAILED || this == ERROR;
    }
}
