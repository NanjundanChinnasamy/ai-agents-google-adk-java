package com.google.adk.finance.v8.callbacks;

import com.google.adk.agents.CallbackContext;
import com.google.adk.agents.Callbacks.BeforeAgentCallbackSync;
import com.google.adk.finance.v8.guardrails.GuardrailResult;
import com.google.adk.finance.v8.guardrails.input.PiiDetector;
import com.google.adk.finance.v8.guardrails.input.PiiSanitizer;
import com.google.adk.finance.v8.guardrails.input.PromptInjectionDetector;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Google ADK lifecycle callback executing deterministic input guardrails before agent execution.
 * <p>
 * Core Interceptions:
 * <ol>
 *   <li><b>Prompt Injection Interception:</b> Scans user input for instruction overrides, secret extraction probes,
 *       or guardrail bypass commands. If detected, terminates the agent invocation immediately with a user-safe alert.</li>
 *   <li><b>PII Detection & Sanitization:</b> Identifies sensitive personal and banking identifiers (bank accounts, card numbers,
 *       emails, phone numbers, tax IDs) and redacts them into safe privacy tokens stored into session state.</li>
 * </ol>
 */
public class BeforeAgentGuardrail implements BeforeAgentCallbackSync {
    private static final Logger logger = LoggerFactory.getLogger(BeforeAgentGuardrail.class);

    private final PromptInjectionDetector injectionDetector;
    private final PiiSanitizer piiSanitizer;

    public BeforeAgentGuardrail() {
        this(new PromptInjectionDetector(), new PiiSanitizer());
    }

    public BeforeAgentGuardrail(PromptInjectionDetector injectionDetector, PiiSanitizer piiSanitizer) {
        this.injectionDetector = injectionDetector;
        this.piiSanitizer = piiSanitizer;
    }

    @Override
    public Optional<Content> call(CallbackContext callbackContext) {
        Optional<Content> userContentOpt = callbackContext.userContent();
        if (userContentOpt.isEmpty()) {
            return Optional.empty();
        }

        String rawInput = extractText(userContentOpt.get());
        if (rawInput.isBlank()) {
            return Optional.empty();
        }

        logger.info("[BeforeAgent Callback] Intercepted user input (length: {} chars)", rawInput.length());

        // 1. Evaluate Input PII Guardrail & Sanitize (Priority 1)
        GuardrailResult piiResult = piiSanitizer.evaluateAndSanitize(rawInput);
        String inputToProcess = rawInput;
        if (piiResult.isSanitized()) {
            logger.info("[GUARDRAIL] PII detected");
            inputToProcess = piiResult.sanitizedValue().orElse(rawInput);
            logger.info("[GUARDRAIL] Input sanitized");

            // Store sanitized value and metadata in session state
            callbackContext.state().put("user_input_original_has_pii", true);
            callbackContext.state().put("user_input_sanitized", inputToProcess);
            callbackContext.state().put("user_input_pii_redaction_count", piiResult.metadata().get("redactedCount"));
        } else {
            callbackContext.state().put("user_input_original_has_pii", false);
            callbackContext.state().put("user_input_sanitized", rawInput);
        }

        // 2. Evaluate Prompt Injection Guardrail (Priority 2)
        // Check both raw and sanitized inputs to prevent bypass attempts
        GuardrailResult injectionResult = injectionDetector.evaluate(rawInput);
        if (!injectionResult.isBlocked() && !inputToProcess.equals(rawInput)) {
            injectionResult = injectionDetector.evaluate(inputToProcess);
        }

        if (injectionResult.isBlocked()) {
            logger.warn("[GUARDRAIL] Prompt injection detected");
            logger.warn("[GUARDRAIL] Request blocked: {}", injectionResult.reason());

            // Terminate invocation in ADK so no model or tools run
            callbackContext.invocationContext().setEndInvocation(true);

            String blockExplanation = "Security Policy Notice: Your request was blocked because a potential instruction override " +
                    "or system prompt extraction pattern was detected. Finance Advisor operates strictly under verified security rules.";

            Content blockedResponse = Content.builder()
                    .role("model")
                    .parts(Part.fromText(blockExplanation))
                    .build();

            return Optional.of(blockedResponse);
        }

        // Proceed with normal agent execution
        return Optional.empty();
    }

    private String extractText(Content content) {
        if (content.parts().isEmpty()) {
            return "";
        }
        return content.parts().get().stream()
                .map(part -> part.text().orElse(""))
                .collect(Collectors.joining(" "));
    }
}
