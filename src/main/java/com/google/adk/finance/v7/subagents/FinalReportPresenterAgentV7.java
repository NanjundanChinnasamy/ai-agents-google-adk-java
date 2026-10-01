package com.google.adk.finance.v7.subagents;

import com.google.adk.agents.Callbacks;
import com.google.adk.agents.LlmAgent;
import com.google.adk.models.BaseLlm;
import com.google.adk.socialspark.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * Week 4 — Version 7: Final Presenter Sub-Agent in Loop Workflow.
 * <p>
 * Responsibility:
 * <ul>
 *   <li>Runs as the post-loop sibling in {@code SequentialAgent(LoopAgent, FinalReportPresenterAgentV7)}.</li>
 *   <li>Formats the finalized, approved investment report alongside the Critic's compliance audit summary.</li>
 *   <li>Publishes final output under {@link #OUTPUT_KEY} ({@code final_verified_report}).</li>
 * </ul>
 */
public final class FinalReportPresenterAgentV7 {
    private static final Logger logger = LoggerFactory.getLogger(FinalReportPresenterAgentV7.class);

    public static final String AGENT_NAME = "final_report_presenter";
    public static final String OUTPUT_KEY = "final_verified_report";

    public static final String INSTRUCTION = """
            You are final_report_presenter (FinalReportPresenterAgentV7), the final presentation step following the Iterative Research-Critic Loop.
            Your task is to present the approved investment research report alongside its official verification audit log.

            Final Verified Draft from Loop:
            {current_draft_report?}

            Final Critic Audit Log:
            {critic_feedback?}

            Presentation Guidelines:
            1. Display the verified investment report clearly.
            2. Append the official "Compliance & Verification Audit Stamp" showing that the report passed iterative critic inspection.
            3. Ensure the mandatory regulatory disclaimer concludes the document verbatim.
            """;

    private static final Callbacks.BeforeAgentCallbackSync beforeCallback = context -> {
        logger.info("[Loop: Presenter] Presenting finalized and audited investment report.");
        return Optional.empty();
    };

    private static final Callbacks.AfterAgentCallbackSync afterCallback = context -> {
        logger.info("[Loop: Presenter] Final report presented. Output published to key: {}", OUTPUT_KEY);
        return Optional.empty();
    };

    /**
     * Creates an instance of {@link FinalReportPresenterAgentV7} with default model.
     */
    public static LlmAgent create() {
        String modelName = AppConfig.get("FINANCE_MODEL", AppConfig.ORCHESTRATOR_MODEL);
        BaseLlm model = AppConfig.createModel(modelName);
        return create(model);
    }

    /**
     * Creates an instance of {@link FinalReportPresenterAgentV7} with the given model.
     */
    public static LlmAgent create(BaseLlm model) {
        return LlmAgent.builder()
                .name(AGENT_NAME)
                .description("Presents the final approved investment research report following successful loop critique.")
                .model(model)
                .instruction(INSTRUCTION)
                .outputKey(OUTPUT_KEY)
                .tools(List.of())
                .beforeAgentCallbackSync(beforeCallback)
                .afterAgentCallbackSync(afterCallback)
                .build();
    }

    private FinalReportPresenterAgentV7() {}
}
