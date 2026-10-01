package com.google.adk.finance.v7.subagents;

import com.google.adk.agents.Callbacks;
import com.google.adk.agents.LlmAgent;
import com.google.adk.models.BaseLlm;
import com.google.adk.socialspark.config.AppConfig;
import com.google.adk.tools.GoogleSearchTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * Week 4 — Version 7: Stage 1 Sub-Agent for Sequential Workflow (Company Research).
 * <p>
 * Responsibility:
 * <ul>
 *   <li>Stage 1 of the deterministic {@code InvestmentResearchSequentialWorkflowV7}.</li>
 *   <li>Executes real-time web searches using {@link GoogleSearchTool#INSTANCE} to identify recent news,
 *       corporate announcements, management transitions, and macro events.</li>
 *   <li>Publishes findings to session state under {@link #OUTPUT_KEY} ({@code company_research_output})
 *       for consumption by Stage 2 (Fundamental Analysis).</li>
 * </ul>
 */
public final class CompanyResearchAgentV7 {
    private static final Logger logger = LoggerFactory.getLogger(CompanyResearchAgentV7.class);

    public static final String AGENT_NAME = "company_research_step";
    public static final String OUTPUT_KEY = "company_research_output";

    public static final String INSTRUCTION = """
            You are company_research_step (CompanyResearchAgentV7), Stage 1 of the deterministic Investment Research Sequential Workflow.
            Your task is to conduct focused external market research on the target company using Google Search.

            Core Research Objectives:
            1. Recent Announcements: Quarterly earnings release, guidance changes, dividend declarations, or share buybacks.
            2. Strategic Developments: Mergers, acquisitions, major customer partnerships, product launches, or facility expansions.
            3. Leadership & Governance: C-suite leadership changes, board restructuring, or corporate governance news.
            4. External & Regulatory: Industry tailwinds/headwinds, regulatory actions, antitrust investigations, or geopolitical impacts.
            5. Price Movement Attribution: Identify recent catalysts driving recent stock price fluctuations.

            Execution Guidelines:
            - Execute Google Search queries targeting recent events (e.g. "[Company Symbol or Name] latest news announcements 2025 2026").
            - Attribute factual claims to dates and source publications.
            - Focus exclusively on recent developments and corporate news. Do NOT compute valuation multiples or make buy/sell calls.

            Output Format:
            Format your output strictly using this structured template:
            [STAGE 1: COMPANY RESEARCH REPORT]
            • Company / Symbol: <Target Company>
            • Research Summary: <Executive summary of market research findings>
            • Key Recent Developments:
              - <Development 1 with date and source citation>
              - <Development 2 with date and source citation>
              - <Development 3 with date and source citation>
            • Material Catalysts: <Identified drivers of recent market sentiment>
            • Research Limitations: <Note search date boundaries or unconfirmed media speculation>
            """;

    private static final Callbacks.BeforeAgentCallbackSync beforeAgentCallback = context -> {
        logger.info("[Sequential Stage 1] CompanyResearchAgentV7 started company research.");
        return Optional.empty();
    };

    private static final Callbacks.AfterAgentCallbackSync afterAgentCallback = context -> {
        logger.info("[Sequential Stage 1] CompanyResearchAgentV7 completed research. Output published to key: {}", OUTPUT_KEY);
        return Optional.empty();
    };

    /**
     * Creates an instance of {@link CompanyResearchAgentV7} with default model.
     */
    public static LlmAgent create() {
        String modelName = AppConfig.get("MARKET_RESEARCH_MODEL", AppConfig.get("FINANCE_SEARCH_MODEL", AppConfig.RESEARCH_MODEL));
        if (modelName == null || (!modelName.startsWith("gemini-") && !modelName.startsWith("google/"))) {
            modelName = "gemini-2.5-flash";
        }
        BaseLlm model = AppConfig.createModel(modelName);
        return create(model);
    }

    /**
     * Creates an instance of {@link CompanyResearchAgentV7} with the given model.
     */
    public static LlmAgent create(BaseLlm model) {
        return LlmAgent.builder()
                .name(AGENT_NAME)
                .description("Stage 1 of Sequential Workflow: Collects recent corporate announcements, news catalysts, and market developments via Google Search.")
                .model(model)
                .instruction(INSTRUCTION)
                .outputKey(OUTPUT_KEY)
                .tools(List.of(GoogleSearchTool.INSTANCE))
                .beforeAgentCallbackSync(beforeAgentCallback)
                .afterAgentCallbackSync(afterAgentCallback)
                .build();
    }

    private CompanyResearchAgentV7() {}
}
