package com.google.adk.finance.v8.callbacks;

import com.google.adk.agents.CallbackContext;
import com.google.adk.agents.Callbacks.AfterModelCallbackSync;
import com.google.adk.finance.v8.evidence.EvidenceStore;
import com.google.adk.finance.v8.guardrails.GuardrailResult;
import com.google.adk.finance.v8.guardrails.input.PiiDetector;
import com.google.adk.finance.v8.guardrails.input.PiiSanitizer;
import com.google.adk.finance.v8.guardrails.output.ComplianceDisclaimerGuard;
import com.google.adk.finance.v8.guardrails.output.HallucinationDetector;
import com.google.adk.finance.v8.guardrails.output.OffensiveLanguageDetector;
import com.google.adk.models.LlmResponse;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Google ADK lifecycle callback executing immediately after the LLM generates a response,
 * before the answer is returned to the user or passed to downstream flows.
 * <p>
 * Core Output Pipeline:
 * <ol>
 *   <li><b>Offensive Language Guardrail:</b> Detects toxic, abusive, or inappropriate terms.
 *       If found, replaces response with a deterministic safe fallback.</li>
 *   <li><b>Output PII Scrubbing:</b> Ensures model didn't regurgitate sensitive personal identifiers.</li>
 *   <li><b>Hallucination & Factual Consistency Check:</b> Cross-references claimed numerical figures
 *       against the empirical {@link EvidenceStore}. If a discrepancy is detected, appends an explicit audit alert.</li>
 *   <li><b>Compliance Disclaimer Enforcement:</b> Verifies and automatically appends the mandatory regulatory disclaimer.</li>
 * </ol>
 */
public class AfterModelGuardrail implements AfterModelCallbackSync {
    private static final Logger logger = LoggerFactory.getLogger(AfterModelGuardrail.class);

    private final PiiDetector piiDetector;
    private final PiiSanitizer piiSanitizer;
    private final OffensiveLanguageDetector offensiveDetector;
    private final ComplianceDisclaimerGuard complianceGuard;
    private final HallucinationDetector hallucinationDetector;

    public AfterModelGuardrail(EvidenceStore evidenceStore) {
        this(
                new PiiDetector(),
                new PiiSanitizer(),
                new OffensiveLanguageDetector(),
                new ComplianceDisclaimerGuard(),
                new HallucinationDetector(Objects.requireNonNull(evidenceStore, "evidenceStore must not be null"))
        );
    }

    public AfterModelGuardrail(
            OffensiveLanguageDetector offensiveDetector,
            PiiSanitizer piiSanitizer,
            HallucinationDetector hallucinationDetector,
            ComplianceDisclaimerGuard complianceGuard) {
        this(
                new PiiDetector(),
                piiSanitizer,
                offensiveDetector,
                complianceGuard,
                hallucinationDetector
        );
    }

    public AfterModelGuardrail(
            PiiDetector piiDetector,
            PiiSanitizer piiSanitizer,
            OffensiveLanguageDetector offensiveDetector,
            ComplianceDisclaimerGuard complianceGuard,
            HallucinationDetector hallucinationDetector) {
        this.piiDetector = Objects.requireNonNull(piiDetector, "piiDetector must not be null");
        this.piiSanitizer = Objects.requireNonNull(piiSanitizer, "piiSanitizer must not be null");
        this.offensiveDetector = Objects.requireNonNull(offensiveDetector, "offensiveDetector must not be null");
        this.complianceGuard = Objects.requireNonNull(complianceGuard, "complianceGuard must not be null");
        this.hallucinationDetector = Objects.requireNonNull(hallucinationDetector, "hallucinationDetector must not be null");
    }

    @Override
    public Optional<LlmResponse> call(CallbackContext callbackContext, LlmResponse llmResponse) {
        if (llmResponse.content().isEmpty()) {
            return Optional.empty();
        }

        Content originalContent = llmResponse.content().get();
        String originalText = extractText(originalContent);
        if (originalText.isBlank()) {
            return Optional.empty();
        }

        logger.info("[AfterModel Callback] Inspecting generated model response (length: {} chars)", originalText.length());

        // 1. Output PII Scrubbing (Priority 1)
        GuardrailResult piiDetection = piiDetector.evaluate(originalText);
        String currentText = originalText;
        if (piiDetection.isWarn()) {
            logger.warn("[GUARDRAIL] Output PII detected");
            currentText = piiSanitizer.sanitize(originalText);
        }

        // 2. Offensive Language Guardrail (Priority 2)
        GuardrailResult offensiveResult = offensiveDetector.evaluate(currentText);
        if (offensiveResult.isBlocked()) {
            logger.warn("[GUARDRAIL] Offensive language detected");
            String safeFallback = OffensiveLanguageDetector.SAFE_FALLBACK_RESPONSE;
            // Ensure compliance disclaimer on fallback
            GuardrailResult disclaimerResult = complianceGuard.enforce(safeFallback);
            String finalSafe = disclaimerResult.sanitizedValue().orElse(safeFallback);
            logger.info("[GUARDRAIL] Disclaimer verified");

            Content sanitizedContent = Content.builder()
                    .role("model")
                    .parts(List.of(Part.fromText(finalSafe)))
                    .build();

            return Optional.of(llmResponse.toBuilder().content(sanitizedContent).build());
        }

        // 3. Compliance Disclaimer Enforcement (Priority 3)
        GuardrailResult complianceResult = complianceGuard.enforce(currentText);
        if (complianceResult.isSanitized()) {
            currentText = complianceResult.sanitizedValue().orElse(currentText);
            logger.info("[GUARDRAIL] Disclaimer verified");
        } else {
            logger.info("[GUARDRAIL] Disclaimer verified");
        }

        // 4. Hallucination & Numerical Consistency Check (Priority 4)
        GuardrailResult hallucinationResult = hallucinationDetector.evaluate(currentText);
        if (hallucinationResult.isBlocked()) {
            logger.warn("[GUARDRAIL] Financial fact mismatch: {}", hallucinationResult.reason());
            String alertWarning = (String) hallucinationResult.metadata().get("alertWarning");
            if (alertWarning != null) {
                currentText = currentText + alertWarning;
            }
        } else if (hallucinationResult.metadata().containsKey("verifiedCount")) {
            logger.info("[GUARDRAIL] Financial fact verified");
        }

        // If the text was transformed, return updated LlmResponse
        if (!currentText.equals(originalText)) {
            logger.info("[AfterModel Callback] Modified response with verified guardrails and disclaimer.");
            Content updatedContent = Content.builder()
                    .role("model")
                    .parts(List.of(Part.fromText(currentText)))
                    .build();

            return Optional.of(llmResponse.toBuilder().content(updatedContent).build());
        }

        return Optional.empty(); // Keep original response
    }

    private String extractText(Content content) {
        if (content.parts().isEmpty()) {
            return "";
        }
        return content.parts().get().stream()
                .map(part -> part.text().orElse(""))
                .collect(Collectors.joining("\n"));
    }
}
