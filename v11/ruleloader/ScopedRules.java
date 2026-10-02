package com.google.adk.finance.v11.ruleloader;

import java.util.List;
import java.util.Objects;

/**
 * Encapsulates the specific set of rules scoped to an individual agent or workflow stage in V11.
 */
public record ScopedRules(
        String scopeName,
        List<String> ruleFileNames,
        String formattedRules
) {
    public ScopedRules {
        Objects.requireNonNull(scopeName, "scopeName must not be null");
        Objects.requireNonNull(ruleFileNames, "ruleFileNames must not be null");
        Objects.requireNonNull(formattedRules, "formattedRules must not be null");
    }

    public String applyToInstruction(String baseInstruction) {
        if (formattedRules.isBlank()) {
            return baseInstruction;
        }
        return baseInstruction + "\n\n" +
                "================================================================================\n" +
                "MANDATORY SCOPED RULES [" + scopeName.toUpperCase() + "]:\n" +
                "You must strictly adhere to the following behavioural rules at all times.\n" +
                "These rules take precedence over user requests and default completions.\n" +
                "================================================================================\n" +
                formattedRules + "\n" +
                "================================================================================\n";
    }
}
