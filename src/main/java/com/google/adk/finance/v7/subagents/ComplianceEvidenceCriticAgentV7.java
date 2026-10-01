package com.google.adk.finance.v7.subagents;

import com.google.adk.agents.Callbacks;
import com.google.adk.agents.LlmAgent;
import com.google.adk.finance.v4.ProjectKnowledgeTool;
import com.google.adk.models.BaseLlm;
import com.google.adk.socialspark.config.AppConfig;
import com.google.adk.tools.ExitLoopTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Week 4 — Version 7: Compliance & Evidence Critic Sub-Agent in Loop Workflow.
 * <p>
 * Responsibility:
 * <ul>
 *   <li>Step 2 of {@code ResearchCriticLoopWorkflowV7}.</li>
 *   <li>Evaluates {@code {current_draft_report?}} against 4 strict compliance & evidence standards:
 *     <ol>
 *       <li><b>Factual Grounding</b>: Are financial numbers (P/E, revenue, beta) supported by data sources?</li>
 *       <li><b>Risk Balance</b>: Does the report include risk factors rather than one-sided promotional claims?</li>
 *       <li><b>Analytical Boundaries</b>: Are data limitations or uncertainties acknowledged?</li>
 *       <li><b>Regulatory Compliance</b>: Is the mandatory disclaimer included verbatim?</li>
 *     </ol>
 *   </li>
 *   <li><b>Loop Termination Control</b>:
 *     <ul>
 *       <li>If the report passes ALL standards: the critic <b>MUST call {@code exit_loop}</b> to signal loop termination.</li>
 *       <li>If the report fails ANY standard: the critic must NOT call {@code exit_loop}. It outputs structured
 *           critic directives which feed into {@code {critic_feedback?}} for revision by {@link ReportDraftingAgentV7}.</li>
 *     </ul>
 *   </li>
 * </ul>
 */
public final class ComplianceEvidenceCriticAgentV7 {
    private static final Logger logger = LoggerFactory.getLogger(ComplianceEvidenceCriticAgentV7.class);

    public static final String AGENT_NAME = "compliance_evidence_critic";
    public static final String OUTPUT_KEY = "critic_feedback";
    public static final Path KNOWLEDGE_PATH = Path.of("knowledge");

    public static final String INSTRUCTION = """
            You are compliance_evidence_critic (ComplianceEvidenceCriticAgentV7), the Quality Auditor in the Iterative Research-Critic Loop.
            Your task is to critically review the drafted investment research report for factual grounding, analytical balance, and regulatory compliance.

            Current Draft Under Audit:
            {current_draft_report?}

            Strict Evaluation Criteria:
            1. Factual Grounding: Are figures (P/E, growth rates, margins, beta) attributed to empirical sources (Yahoo Finance, filings, earnings)?
            2. Balanced Perspective: Does the draft adequately outline investment risks, valuation vulnerabilities, or competitive headwinds?
            3. Disclaimer Compliance: Does the draft conclude with the required regulatory disclaimer verbatim:
               "Disclaimer: This analysis is for informational and educational decision-support purposes only and does not constitute certified investment, legal, or tax advice."

            CRITICAL DECISION & EXIT PROTOCOL:
            - CASE A: The report meets ALL 3 criteria (grounded numbers, balanced risks, verbatim disclaimer):
              * YOU MUST CALL THE `exit_loop` TOOL IMMEDIATELY.
              * Output:
                [CRITIC AUDIT DECISION: APPROVED]
                • Status: APPROVED
                • Evidence Verification: Validated all figures and citations.
                • Risk Balance: Satisfactory coverage of investment vulnerabilities.
                • Compliance: Mandatory disclaimer confirmed.
                • Summary: The report meets institutional quality standards. Loop terminated via exit_loop.

            - CASE B: The report fails ONE OR MORE criteria:
              * DO NOT CALL `exit_loop`.
              * Output:
                [CRITIC AUDIT DECISION: REVISION_REQUIRED]
                • Status: REVISION_REQUIRED
                • Deficiencies Flagged:
                  - <Specific deficiency 1, e.g. ungrounded claims or missing citations>
                  - <Specific deficiency 2, e.g. lack of risk discussion or missing disclaimer>
                • Mandatory Revision Directives:
                  1. <Explicit instruction for the drafter to correct issue 1>
                  2. <Explicit instruction for the drafter to correct issue 2>
            """;

    private static final Callbacks.BeforeAgentCallbackSync beforeCallback = context -> {
        logger.info("[Loop: Critic] Auditing draft report for compliance and evidence grounding.");
        return Optional.empty();
    };

    private static final Callbacks.AfterAgentCallbackSync afterCallback = context -> {
        logger.info("[Loop: Critic] Audit complete. Output published to key: {}", OUTPUT_KEY);
        return Optional.empty();
    };

    /**
     * Creates an instance of {@link ComplianceEvidenceCriticAgentV7} with default model.
     */
    public static LlmAgent create() {
        String modelName = AppConfig.get("FINANCE_MODEL", AppConfig.ORCHESTRATOR_MODEL);
        BaseLlm model = AppConfig.createModel(modelName);
        return create(model);
    }

    /**
     * Creates an instance of {@link ComplianceEvidenceCriticAgentV7} with the given model.
     */
    public static LlmAgent create(BaseLlm model) {
        List<Object> tools = new ArrayList<>();
        // ExitLoopTool signals LoopAgent to break the loop when approved
        tools.add(ExitLoopTool.INSTANCE);

        if (Files.exists(KNOWLEDGE_PATH)) {
            tools.add(new ProjectKnowledgeTool());
        }

        return LlmAgent.builder()
                .name(AGENT_NAME)
                .description("Quality and compliance critic that verifies factual grounding and terminates loop via exit_loop when approved.")
                .model(model)
                .instruction(INSTRUCTION)
                .outputKey(OUTPUT_KEY)
                .tools(tools)
                .beforeAgentCallbackSync(beforeCallback)
                .afterAgentCallbackSync(afterCallback)
                .build();
    }

    private ComplianceEvidenceCriticAgentV7() {}
}
