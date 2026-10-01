package com.google.adk.finance.v8.guardrails.output;

import com.google.adk.finance.v8.guardrails.GuardrailResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Institutional regulatory compliance guardrail.
 * <p>
 * Enforces the invariant that every financial decision-support output returned to a client
 * MUST carry an explicit regulatory disclaimer distinguishing research analysis from certified
 * investment advice. If the LLM omitted the required disclaimer, this guardrail automatically appends it.
 */
public class ComplianceDisclaimerGuard {
    private static final Logger logger = LoggerFactory.getLogger(ComplianceDisclaimerGuard.class);

    public static final String MANDATORY_DISCLAIMER =
            "\n\n---\n**Regulatory Disclaimer**: This analysis and research synthesis is prepared strictly for educational " +
            "and informational decision-support purposes only and does not constitute certified financial, investment, legal, " +
            "or tax advice. Past performance is no guarantee of future returns. Consult a licensed SEBI/SEC financial advisor " +
            "prior to executing any transactions.";

    private static final Pattern DISCLAIMER_KEYWORDS_PATTERN = Pattern.compile(
            "(?i)(?:regulatory\\s+disclaimer|not\\s+financial\\s+advice|educational\\s+(?:and\\s+informational\\s+)?purposes\\s+only|consult\\s+a\\s+(?:licensed|certified)?\\s*financial\\s+advisor)"
    );

    public ComplianceDisclaimerGuard() {}

    /**
     * Checks if the output contains the necessary regulatory disclaimers.
     *
     * @param text output text
     * @return true if a compliant disclaimer is already present
     */
    public boolean hasDisclaimer(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        return DISCLAIMER_KEYWORDS_PATTERN.matcher(text).find();
    }

    /**
     * Enforces the regulatory disclaimer by appending it if missing.
     *
     * @param text the raw candidate model response
     * @return {@link GuardrailResult} containing the verified or augmented response text
     */
    public GuardrailResult enforce(String text) {
        if (text == null || text.isBlank()) {
            return GuardrailResult.sanitize(
                    "ComplianceDisclaimerGuard",
                    MANDATORY_DISCLAIMER.trim(),
                    "Injected disclaimer into empty response."
            );
        }

        if (hasDisclaimer(text)) {
            return GuardrailResult.allow(
                    "ComplianceDisclaimerGuard",
                    "Compliant regulatory disclaimer detected in output."
            );
        }

        logger.info("[Compliance Guardrail] Appending missing regulatory disclaimer to model output.");
        String updated = text.trim() + MANDATORY_DISCLAIMER;
        return GuardrailResult.sanitize(
                "ComplianceDisclaimerGuard",
                updated,
                "Mandatory regulatory disclaimer was absent and has been appended.",
                Map.of("disclaimerLength", MANDATORY_DISCLAIMER.length())
        );
    }
}
