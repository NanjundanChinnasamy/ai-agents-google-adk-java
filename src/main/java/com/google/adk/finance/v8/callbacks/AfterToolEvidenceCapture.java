package com.google.adk.finance.v8.callbacks;

import com.google.adk.agents.Callbacks.AfterToolCallbackSync;
import com.google.adk.agents.InvocationContext;
import com.google.adk.finance.v8.evidence.EvidenceStore;
import com.google.adk.tools.BaseTool;
import com.google.adk.tools.ToolContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Google ADK lifecycle callback executing immediately after a tool finishes running.
 * <p>
 * Captures empirical tool outputs (such as market quotes, price points, and financial metrics)
 * and records them into the {@link EvidenceStore}. These verified figures form the baseline
 * for factual consistency audits performed in the {@link AfterModelGuardrail}.
 */
public class AfterToolEvidenceCapture implements AfterToolCallbackSync {
    private static final Logger logger = LoggerFactory.getLogger(AfterToolEvidenceCapture.class);

    private final EvidenceStore evidenceStore;

    public AfterToolEvidenceCapture(EvidenceStore evidenceStore) {
        this.evidenceStore = Objects.requireNonNull(evidenceStore, "evidenceStore must not be null");
    }

    @Override
    public Optional<Map<String, Object>> call(
            InvocationContext invocationContext,
            BaseTool baseTool,
            Map<String, Object> input,
            ToolContext toolContext,
            Object response) {

        String toolName = baseTool.name();
        logger.info("[AfterTool Callback] Tool '{}' completed. Capturing evidence.", toolName);

        // Capture evidence from tool execution
        evidenceStore.capture(toolName, input, response);

        // Return empty to keep original tool response unmodified
        return Optional.empty();
    }
}
