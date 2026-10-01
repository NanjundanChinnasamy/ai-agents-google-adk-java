package com.google.adk.finance.v8.callbacks;

import com.google.adk.agents.CallbackContext;
import com.google.adk.agents.Callbacks.BeforeModelCallbackSync;
import com.google.adk.finance.v8.guardrails.input.PiiSanitizer;
import com.google.adk.models.LlmRequest;
import com.google.adk.models.LlmResponse;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Google ADK lifecycle callback executing immediately before LLM generation.
 * <p>
 * Ensures that payloads transmitted over the wire to the external LLM provider
 * are strictly sanitized of any residual raw PII (such as bank accounts, credit cards, or tax IDs),
 * enforcing defense-in-depth across the model boundary.
 */
public class BeforeModelGuardrail implements BeforeModelCallbackSync {
    private static final Logger logger = LoggerFactory.getLogger(BeforeModelGuardrail.class);

    private final PiiSanitizer piiSanitizer;

    public BeforeModelGuardrail() {
        this(new PiiSanitizer());
    }

    public BeforeModelGuardrail(PiiSanitizer piiSanitizer) {
        this.piiSanitizer = piiSanitizer;
    }

    @Override
    public Optional<LlmResponse> call(CallbackContext callbackContext, LlmRequest.Builder llmRequestBuilder) {
        List<Content> contents;
        try {
            LlmRequest request = llmRequestBuilder.build();
            contents = request.contents();
        } catch (Exception e) {
            logger.debug("[BeforeModel Callback] Could not build LlmRequest for inspection: {}", e.getMessage());
            return Optional.empty();
        }

        if (contents == null || contents.isEmpty()) {
            return Optional.empty();
        }

        boolean modified = false;
        List<Content> sanitizedContents = new ArrayList<>();

        for (Content content : contents) {
            if (content.parts().isEmpty()) {
                sanitizedContents.add(content);
                continue;
            }

            List<Part> newParts = new ArrayList<>();
            for (Part part : content.parts().get()) {
                if (part.text().isPresent()) {
                    String text = part.text().get();
                    String sanitized = piiSanitizer.sanitize(text);
                    if (!sanitized.equals(text)) {
                        modified = true;
                        newParts.add(Part.fromText(sanitized));
                    } else {
                        newParts.add(part);
                    }
                } else {
                    newParts.add(part);
                }
            }

            if (modified) {
                sanitizedContents.add(content.toBuilder().parts(newParts).build());
            } else {
                sanitizedContents.add(content);
            }
        }

        if (modified) {
            logger.info("[GUARDRAIL] PII detected in model payload");
            logger.info("[GUARDRAIL] Input sanitized before wire transmission");
            llmRequestBuilder.contents(sanitizedContents);
        }

        return Optional.empty(); // Continue with model generation
    }
}
