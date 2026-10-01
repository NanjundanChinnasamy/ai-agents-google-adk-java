package com.google.adk.finance.v7.subagents;

import com.google.adk.agents.Callbacks;
import com.google.adk.agents.LlmAgent;
import com.google.adk.finance.v4.ProjectKnowledgeTool;
import com.google.adk.models.BaseLlm;
import com.google.adk.skills.LocalSkillSource;
import com.google.adk.socialspark.config.AppConfig;
import com.google.adk.tools.mcp.McpToolset;
import com.google.adk.tools.skills.SkillToolset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Week 4 — Version 7: Stage 4 Sub-Agent for Sequential Workflow (Valuation Analysis).
 * <p>
 * Responsibility:
 * <ul>
 *   <li>Stage 4 of {@code InvestmentResearchSequentialWorkflowV7}.</li>
 *   <li>Ingests upstream context via {@code {fundamental_analysis_output?}} and {@code {risk_analysis_output?}}.</li>
 *   <li>Evaluates valuation multiples (P/E, forward P/E, PEG, P/B, EV/EBITDA) in the context of growth rates and risk profile.</li>
 *   <li><b>Strict Invariant</b>: Never invent or hallucinate missing financial figures; explicitly declare if a multiple is unavailable.</li>
 *   <li>Publishes findings under {@link #OUTPUT_KEY} ({@code valuation_analysis_output})
 *       for consumption by Stage 5 (Synthesis).</li>
 * </ul>
 */
public final class ValuationAnalysisAgentV7 {
    private static final Logger logger = LoggerFactory.getLogger(ValuationAnalysisAgentV7.class);

    public static final String AGENT_NAME = "valuation_analysis_step";
    public static final String OUTPUT_KEY = "valuation_analysis_output";
    public static final Path FINANCE_SKILLS_PATH = Path.of("skills", "finance");
    public static final Path KNOWLEDGE_PATH = Path.of("knowledge");

    public static final String INSTRUCTION = """
            You are valuation_analysis_step (ValuationAnalysisAgentV7), Stage 4 of the deterministic Investment Research Sequential Workflow.
            Your task is to analyze valuation multiples and pricing considerations based on verified data.

            Context from Upstream Stage 2 (Fundamental Analysis):
            {fundamental_analysis_output?}

            Context from Upstream Stage 3 (Risk Analysis):
            {risk_analysis_output?}

            Core Valuation Assessment Principles:
            1. Multiples Framework:
               - Trailing P/E and Forward P/E (Price-to-Earnings).
               - PEG Ratio (Price/Earnings to Growth): Evaluates whether a high P/E is justified by high earnings growth.
               - Price-to-Book (P/B) and Enterprise Value to EBITDA (EV/EBITDA).
            2. Contextual Evaluation:
               - A high P/E does NOT automatically imply overvaluation; interpret multiples in light of return on capital (ROE/ROCE) and secular growth catalysts.
               - A low P/E can be a "value trap" if debt is excessive or growth is deteriorating (check Stage 3 risk output).
            3. Strict Anti-Hallucination Directive:
               - NEVER fabricate, invent, or estimate missing financial figures.
               - If a multiple is not returned by Yahoo Finance or reported fundamentals, state "Data Not Available" explicitly.

            Tool Usage Directives:
            - Call `get_stock_info` to retrieve exact current valuation metrics (PE, Forward PE, PEG, Price/Book, EV/EBITDA).
            - Use `read_project_knowledge` for valuation methodology (topic='valuation-principles').

            Output Format:
            Format your output strictly using this structured template:
            [STAGE 4: VALUATION ANALYSIS REPORT]
            • Company / Symbol: <Target Company>
            • Valuation Multiples:
              - Trailing P/E: <Value or Not Available>
              - Forward P/E: <Value or Not Available>
              - PEG Ratio: <Value or Not Available>
              - Price / Book (P/B): <Value or Not Available>
              - EV / EBITDA: <Value or Not Available>
            • Contextual Valuation Thesis:
              - Growth Justification: <Does growth justify current multiples?>
              - Risk Discount: <Does the risk profile from Stage 3 warrant a lower valuation multiple?>
            • Valuation Summary: <Fairly Valued / Premium / Discounted relative to historical fundamentals>
            • Data Availability & Limitations: <Explicitly note any unavailable valuation metrics>
            """;

    private static final Callbacks.BeforeAgentCallbackSync beforeAgentCallback = context -> {
        logger.info("[Sequential Stage 4] ValuationAnalysisAgentV7 started valuation analysis with Stages 2 & 3 context.");
        return Optional.empty();
    };

    private static final Callbacks.AfterAgentCallbackSync afterAgentCallback = context -> {
        logger.info("[Sequential Stage 4] ValuationAnalysisAgentV7 completed valuation analysis. Output published to key: {}", OUTPUT_KEY);
        return Optional.empty();
    };

    /**
     * Creates an instance of {@link ValuationAnalysisAgentV7} with default model and MCP toolset.
     */
    public static LlmAgent create(McpToolset mcpToolset) {
        String modelName = AppConfig.get("FINANCE_MODEL", AppConfig.ORCHESTRATOR_MODEL);
        BaseLlm model = AppConfig.createModel(modelName);
        return create(model, mcpToolset);
    }

    /**
     * Creates an instance of {@link ValuationAnalysisAgentV7} with the given model and MCP toolset.
     */
    public static LlmAgent create(BaseLlm model, McpToolset mcpToolset) {
        List<Object> tools = new ArrayList<>();

        if (mcpToolset != null) {
            tools.add(mcpToolset);
        }

        if (Files.exists(FINANCE_SKILLS_PATH)) {
            try {
                SkillToolset skillToolset = new SkillToolset(new LocalSkillSource(FINANCE_SKILLS_PATH));
                tools.add(skillToolset);
            } catch (Exception e) {
                logger.warn("Could not load skills for ValuationAnalysisAgentV7: {}", e.getMessage());
            }
        }

        if (Files.exists(KNOWLEDGE_PATH)) {
            tools.add(new ProjectKnowledgeTool());
        }

        return LlmAgent.builder()
                .name(AGENT_NAME)
                .description("Stage 4 of Sequential Workflow: Evaluates valuation multiples (P/E, PEG, EV/EBITDA) grounded in verified financial figures.")
                .model(model)
                .instruction(INSTRUCTION)
                .outputKey(OUTPUT_KEY)
                .tools(tools)
                .beforeAgentCallbackSync(beforeAgentCallback)
                .afterAgentCallbackSync(afterAgentCallback)
                .build();
    }

    private ValuationAnalysisAgentV7() {}
}
