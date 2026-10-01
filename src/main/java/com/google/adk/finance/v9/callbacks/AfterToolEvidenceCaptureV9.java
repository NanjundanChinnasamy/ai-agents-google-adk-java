package com.google.adk.finance.v9.callbacks;

import com.google.adk.agents.Callbacks.AfterToolCallbackSync;
import com.google.adk.agents.InvocationContext;
import com.google.adk.finance.v9.evaluation.EvidenceStoreV9;
import com.google.adk.tools.BaseTool;
import com.google.adk.tools.ToolContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Google ADK lifecycle callback executing immediately after a tool finishes running in Version 9.
 * Captures empirical tool outputs into {@link EvidenceStoreV9}.
 */
public class AfterToolEvidenceCaptureV9 implements AfterToolCallbackSync {
    private static final Logger logger = LoggerFactory.getLogger(AfterToolEvidenceCaptureV9.class);

    private final EvidenceStoreV9 evidenceStore;
    private final com.google.adk.finance.v8.evidence.EvidenceStore v8StoreBridge;

    public AfterToolEvidenceCaptureV9(EvidenceStoreV9 evidenceStore) {
        this(evidenceStore, null);
    }

    public AfterToolEvidenceCaptureV9(EvidenceStoreV9 evidenceStore, com.google.adk.finance.v8.evidence.EvidenceStore v8StoreBridge) {
        this.evidenceStore = Objects.requireNonNull(evidenceStore, "evidenceStore must not be null");
        this.v8StoreBridge = v8StoreBridge;
    }

    @Override
    public Optional<Map<String, Object>> call(
            InvocationContext invocationContext,
            BaseTool baseTool,
            Map<String, Object> input,
            ToolContext toolContext,
            Object response) {

        String toolName = baseTool != null ? baseTool.name() : "unknown_tool";
        logger.info("[AfterTool Callback V9] Tool '{}' completed. Capturing evidence.", toolName);

        evidenceStore.capture(toolName, input, response);
        if (v8StoreBridge != null) {
            v8StoreBridge.capture(toolName, input, response);
        }
        return Optional.empty();
    }
}
