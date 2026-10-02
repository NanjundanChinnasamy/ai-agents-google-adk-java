package com.google.adk.finance.v11.hooks;

/**
 * Defines the operational execution policy of a V11 lifecycle hook.
 */
public enum HookPolicy {
    /**
     * If validation fails, intercept execution immediately, short-circuit downstream calls,
     * and return a deterministic error payload to the agent or caller.
     */
    BLOCKING,

    /**
     * If an error, warning, or observability failure occurs, log the problem to telemetry/audit
     * and permit normal agent execution to proceed without disruption.
     */
    NON_BLOCKING
}
